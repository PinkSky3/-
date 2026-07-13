param(
    [Parameter(Mandatory = $true)]
    [string]$Version,

    [switch]$ConfirmedByUser
)

$ErrorActionPreference = 'Stop'

if (-not $ConfirmedByUser) {
    throw 'Explicit user confirmation is required before publishing releases/latest'
}
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    throw "Version must use stable semantic format, received '$Version'"
}

$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
Set-Location -LiteralPath $repositoryRoot
if ((git branch --show-current) -ne 'main') {
    throw 'Confirmed publication must run from the main branch'
}
if (-not [string]::IsNullOrWhiteSpace((git status --porcelain))) {
    throw 'Confirmed publication requires a clean working tree'
}
& git fetch origin main
if ($LASTEXITCODE -ne 0 -or (git rev-parse HEAD) -ne (git rev-parse origin/main)) {
    throw 'Local main must match origin/main before confirmed publication'
}

$tag = "v$Version"
$release = & gh release view $tag --json isDraft,isPrerelease,tagName,url | ConvertFrom-Json
if (-not $release.isDraft -or $release.isPrerelease) {
    throw "$tag must be a stable draft before publication"
}

& gh release edit $tag --draft=false --prerelease=false --latest
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to publish confirmed release'
}

$latest = & gh api 'repos/PinkSky3/DailyHot-Android/releases/latest' | ConvertFrom-Json
if ($latest.tag_name -ne $tag -or $latest.draft -or $latest.prerelease) {
    throw "releases/latest does not point to $tag"
}

$expectedAssetName = "JuHeZhiXun_ver$Version.apk"
$expectedAssetLabel = "聚合智讯_ver$Version.apk"
$asset = $latest.assets | Where-Object {
    $_.name -eq $expectedAssetName -and $_.label -eq $expectedAssetLabel
} | Select-Object -First 1
if ($null -eq $asset) {
    throw "Formal release is missing the confirmed APK $expectedAssetName"
}

$manifest = [ordered]@{
    channel = 'stable'
    version = $Version
    title = if ([string]::IsNullOrWhiteSpace($latest.name)) { "聚合智讯 $Version" } else { $latest.name }
    notes = if ($null -eq $latest.body) { '' } else { $latest.body.Trim() }
    downloadUrl = $asset.browser_download_url
    releasePageUrl = $latest.html_url
}
$manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath 'update.json' -Encoding UTF8

& git add -- 'update.json'
& git diff --cached --quiet -- 'update.json'
if ($LASTEXITCODE -ne 0) {
    & git commit -m "Publish update manifest for $Version"
    if ($LASTEXITCODE -ne 0) { throw 'Unable to commit confirmed update manifest' }
    & git push origin main
    if ($LASTEXITCODE -ne 0) { throw 'Unable to push confirmed update manifest' }
}

try {
    Invoke-WebRequest -Uri 'https://purge.jsdelivr.net/gh/PinkSky3/DailyHot-Android@main/update.json' -UseBasicParsing | Out-Null
} catch {
    Write-Warning 'jsDelivr cache purge failed; the stable manifest will refresh automatically'
}

Write-Host "Published confirmed formal release $($latest.html_url)"
