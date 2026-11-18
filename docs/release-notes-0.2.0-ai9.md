# HyperPortal v0.2.0-ai9

- **固定签名身份**：仓库里没有配置 `RELEASE_KEYSTORE_*` 这几个 secret，之前每次云端构建都会
  用 `keytool -genkeypair` 现场生成一把新钥匙，于是每个版本的证书都不同、覆盖安装必然
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，而卸载重装又会清掉 LSPosed 的模块作用域。现在改为
  从仓库内 `signing/key.pem` + `signing/cert.pem` 派生出 `.sign/my-release-key.jks`，
  所有产物共用同一张证书；配置了 secret 时仍优先用 secret。
- **首页能看见「AES 补丁包未安装」**：`com.miui.contentcatcher` 被 HyperOS 系统更新连签名一起
  换掉后，会在下一次包扫描时被直接删除（`System package signature mismatch` 紧跟
  `no longer exists`），此后长按任何内容都不再取词，而两个 LSPosed 钩子仍显示已注入，
  首页只会停在「等待 AES 握手」。现在用 PackageManager 直接查这个包，缺失时状态卡与
  「AES 握手」一行都明确写出根因和修复动作。
