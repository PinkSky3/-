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
$appDisplayName = "$([char]0x805A)$([char]0x5408)$([char]0x667A)$([char]0x8BAF)"
$expectedAssetLabel = "${appDisplayName}_ver$Version.apk"
$asset = $latest.assets | Where-Object {
    $_.name -eq $expectedAssetName -and $_.label -eq $expectedAssetLabel
} | Select-Object -First 1
if ($null -eq $asset) {
    throw "Formal release is missing the confirmed APK $expectedAssetName"
}

$channelRoot = Join-Path $env:TEMP ('dailyhot-release-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $channelRoot | Out-Null
try {
    $sourceApk = Join-Path $channelRoot $expectedAssetName
    Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $sourceApk -UseBasicParsing

    $displayApkPath = Join-Path $channelRoot $expectedAssetLabel
    Copy-Item -LiteralPath $sourceApk -Destination $displayApkPath
    Remove-Item -LiteralPath $sourceApk -Force

    $encodedDisplayName = [uri]::EscapeDataString($expectedAssetLabel)
    $manifest = [ordered]@{
        channel = 'stable'
        version = $Version
        title = if ([string]::IsNullOrWhiteSpace($latest.name)) { "$appDisplayName $Version" } else { $latest.name }
        notes = if ($null -eq $latest.body) { '' } else { $latest.body.Trim() }
        downloadUrl = "https://cdn.jsdelivr.net/gh/PinkSky3/DailyHot-Android@release-channel/$encodedDisplayName"
        releasePageUrl = $latest.html_url
    }
    $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $channelRoot 'update.json') -Encoding UTF8

    Set-Location -LiteralPath $channelRoot
    & git init
    & git config user.name 'github-actions[bot]'
    & git config user.email '41898282+github-actions[bot]@users.noreply.github.com'
    & git add -- 'update.json' $expectedAssetLabel
    & git commit -m "Publish stable update channel $Version"
    if ($LASTEXITCODE -ne 0) { throw 'Unable to commit stable update channel' }
    & git remote add origin 'https://github.com/PinkSky3/DailyHot-Android.git'
    & git push --force origin 'HEAD:refs/heads/release-channel'
    if ($LASTEXITCODE -ne 0) { throw 'Unable to publish stable update channel' }

    foreach ($url in @(
        'https://purge.jsdelivr.net/gh/PinkSky3/DailyHot-Android@release-channel/update.json',
        "https://purge.jsdelivr.net/gh/PinkSky3/DailyHot-Android@release-channel/$encodedDisplayName"
    )) {
        try {
            Invoke-WebRequest -Uri $url -UseBasicParsing | Out-Null
        } catch {
            Write-Warning "jsDelivr cache purge failed for $url"
        }
    }
} finally {
    Set-Location -LiteralPath $repositoryRoot
    $resolvedTemp = (Resolve-Path -LiteralPath $env:TEMP).Path
    $resolvedChannel = (Resolve-Path -LiteralPath $channelRoot -ErrorAction SilentlyContinue).Path
    if ($resolvedChannel -and $resolvedChannel.StartsWith($resolvedTemp, [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolvedChannel -Recurse -Force
    }
}

Write-Host "Published confirmed formal release $($latest.html_url)"
