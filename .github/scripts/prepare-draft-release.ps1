param(
    [Parameter(Mandatory = $true)]
    [string]$Version,

    [Parameter(Mandatory = $true)]
    [string]$ApkPath,

    [Parameter(Mandatory = $true)]
    [string]$TargetCommit
)

$ErrorActionPreference = 'Stop'

if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    throw "Version must use stable semantic format, received '$Version'"
}

$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
Set-Location -LiteralPath $repositoryRoot
$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$expectedName = "聚合智讯_ver$Version.apk"
if ([IO.Path]::GetFileName($resolvedApk) -cne $expectedName) {
    throw "APK must be named '$expectedName'"
}

$buildScript = Get-Content -LiteralPath 'app/build.gradle.kts' -Raw -Encoding UTF8
$versionMatch = [regex]::Match($buildScript, 'versionName\s*=\s*"([^"]+)"')
if (-not $versionMatch.Success -or $versionMatch.Groups[1].Value -ne $Version) {
    throw "Requested version $Version does not match app version"
}

$changelog = Get-Content -LiteralPath 'CHANGELOG.md' -Encoding UTF8
$escapedVersion = [regex]::Escape($Version)
$sectionStart = -1
for ($index = 0; $index -lt $changelog.Count; $index++) {
    if ($changelog[$index] -match "^##\s+$escapedVersion(?:\s|（|\()") {
        $sectionStart = $index
        break
    }
}
if ($sectionStart -lt 0) {
    throw "CHANGELOG.md has no section for $Version"
}

$sectionEnd = $changelog.Count
for ($index = $sectionStart + 1; $index -lt $changelog.Count; $index++) {
    if ($changelog[$index] -match '^##\s+') {
        $sectionEnd = $index
        break
    }
}
$releaseNotes = $changelog[($sectionStart + 1)..($sectionEnd - 1)]
if ([string]::IsNullOrWhiteSpace(($releaseNotes -join "`n"))) {
    throw "Release notes for $Version are empty"
}

$notesPath = Join-Path $env:RUNNER_TEMP "release-notes-$Version.md"
Set-Content -LiteralPath $notesPath -Value $releaseNotes -Encoding UTF8
$tag = "v$Version"
$title = "聚合智讯 $Version"

$existingJson = & gh release view $tag --json isDraft,tagName 2>$null
if ($LASTEXITCODE -eq 0) {
    $existing = $existingJson | ConvertFrom-Json
    if (-not $existing.isDraft) {
        throw "$tag is already published; refusing to overwrite a formal release"
    }
    & gh release upload $tag $resolvedApk --clobber
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to replace draft release asset'
    }
    & gh release edit $tag --draft --prerelease=false --title $title --notes-file $notesPath --target $TargetCommit
} else {
    & gh release create $tag $resolvedApk --draft --title $title --notes-file $notesPath --target $TargetCommit
}
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to prepare draft release'
}

$draft = & gh release view $tag --json isDraft,isPrerelease,tagName,url | ConvertFrom-Json
if (-not $draft.isDraft -or $draft.isPrerelease) {
    throw 'Release preparation escaped the draft-only gate'
}

Write-Host "Draft release ready for user confirmation: $($draft.url)"
