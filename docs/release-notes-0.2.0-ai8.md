# HyperPortal v0.2.0-ai8

- 修复重启后无法触发与 SystemUI 钩子未注入问题：移除 `HyperPortalEntry.onPackageLoaded` 中会误拦冷启动主包注入的 `isFirstPackage()` 早期过滤，恢复由 `PortalSystemUiModule.initSystemUi` 内部 `isSecondarySystemUiProcess()` 与 `sHookedAppOnCreateClasses` 去重保护
- 将 `ACTION_TEARDOWN_OVERLAY` 跨 ClassLoader 驱逐广播移至 `sBgExecutor` 后台线程执行，避免在 `SystemUIApplication.onCreate` 主线程启动窗口期发起同步 IPC 阻塞输入分发
- 保留 BigBang 液态玻璃真背景模糊（`BackgroundBlurDrawable`）、现代矢量图标与 `TransferDockController.teardown()` 全窗口回收
