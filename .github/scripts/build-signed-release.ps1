$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
Set-Location -LiteralPath $repositoryRoot

if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
    throw 'ANDROID_HOME is not configured'
}

Set-Content -LiteralPath 'local.properties' -Value "sdk.dir=$env:ANDROID_HOME" -Encoding UTF8

$encodedKeystore = (Get-Content -LiteralPath 'debug.keystore.base64' -Raw -Encoding UTF8).Trim()
$keystorePath = Join-Path $repositoryRoot 'debug.keystore'
[IO.File]::WriteAllBytes($keystorePath, [Convert]::FromBase64String($encodedKeystore))

$env:KEYSTORE_PATH = $keystorePath
$env:STORE_PASSWORD = 'android'
$env:KEY_ALIAS = 'androiddebugkey'
$env:KEY_PASSWORD = 'android'

& ./gradlew testDebugUnitTest assembleRelease --stacktrace --no-daemon
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE"
}

$sourceApk = Resolve-Path -LiteralPath 'app/build/outputs/apk/release/app-release.apk'
$buildScript = Get-Content -LiteralPath 'app/build.gradle.kts' -Raw -Encoding UTF8
$versionMatch = [regex]::Match($buildScript, 'versionName\s*=\s*"([^"]+)"')
if (-not $versionMatch.Success) {
    throw 'Unable to read versionName from app/build.gradle.kts'
}

$version = $versionMatch.Groups[1].Value
$fileName = "聚合智讯_ver$version.apk"
$targetApk = Join-Path (Split-Path -Parent $sourceApk.Path) $fileName
$apksigner = Get-ChildItem -LiteralPath "$env:ANDROID_HOME/build-tools" -Recurse -File -Filter 'apksigner' |
    Sort-Object FullName -Descending |
    Select-Object -First 1
if ($null -eq $apksigner) {
    throw 'Android apksigner was not found'
}

& $apksigner.FullName verify --verbose --print-certs $sourceApk.Path
if ($LASTEXITCODE -ne 0) {
    throw 'Release APK signature verification failed'
}

Move-Item -LiteralPath $sourceApk.Path -Destination $targetApk -Force

if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_OUTPUT)) {
    "apk_name=$fileName" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Encoding UTF8 -Append
    "apk_path=$targetApk" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Encoding UTF8 -Append
    "version=$version" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Encoding UTF8 -Append
}

Write-Host "Prepared signed APK $targetApk"
