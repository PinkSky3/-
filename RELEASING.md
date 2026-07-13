# 正式发布流程

普通提交只运行 Android CI，生成签名候选 APK。Actions 产物不会进入 GitHub Releases，应用也不会把它识别成更新。

准备发布时，先确认 `app/build.gradle.kts` 的 `versionName`、`versionCode` 和 `CHANGELOG.md` 已同步，再手动运行 `Prepare Draft Release`。该流程会重新测试、构建、验签，然后创建或更新草稿 Release。

草稿完成后，把版本号、更新日志、构建结果和草稿地址交给用户检查。没有收到用户明确的“确认发布”，不得执行正式发布命令。

收到确认后运行：

```powershell
$ErrorActionPreference = 'Stop'
& '.github/scripts/publish-confirmed-release.ps1' -Version '1.3.1' -ConfirmedByUser
```

脚本会把草稿转成稳定正式版，并验证 GitHub 的 `releases/latest` 已指向该版本。应用只读取这个接口，同时再次拒绝草稿版、预览版、非法版本号和名称不匹配的 APK。
