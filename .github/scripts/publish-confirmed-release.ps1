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

Write-Host "Published confirmed formal release $($latest.html_url)"
