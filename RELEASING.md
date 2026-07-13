# 正式发布流程

普通提交只运行 Android CI，生成签名候选 APK。Actions 产物不会进入 GitHub Releases，应用也不会把它识别成更新。

准备发布时，先确认 `app/build.gradle.kts` 的 `versionName`、`versionCode` 和 `CHANGELOG.md` 已同步，再手动运行 `Prepare Draft Release`。该流程会重新测试、构建、验签，然后创建或更新草稿 Release。

草稿完成后，把版本号、更新日志、构建结果和草稿地址交给用户检查。没有收到用户明确的“确认发布”，不得执行正式发布命令。

收到确认后运行：

```powershell
$ErrorActionPreference = 'Stop'
& '.github/scripts/publish-confirmed-release.ps1' -Version '1.3.1' -ConfirmedByUser
```

脚本会把草稿转成稳定正式版，验证 GitHub 的 `releases/latest` 已指向该版本，再更新 `update.json`。应用优先通过 jsDelivr 读取这份稳定清单，Raw GitHub 只作备用；普通 Actions 构建和未确认草稿不会改动清单。GitHub 会清洗中文资源文件名，因此 Release 内部使用 `JuHeZhiXun_ver版本号.apk`，界面标签仍显示 `聚合智讯_ver版本号.apk`。
