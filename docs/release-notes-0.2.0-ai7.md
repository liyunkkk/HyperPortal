# HyperPortal v0.2.0-ai7

- 跨 ClassLoader 实例驱逐机制：`PortalSystemUiModule.initOverlay` 启动时向 `com.android.systemui` 发送带 `INSTANCE_ID` 的 `ACTION_TEARDOWN_OVERLAY` 驱逐广播，自动清理旧版本或热重载残留的 `SystemUiOverlay` 悬浮窗与接收器
- 补全 `TransferDockController.teardown()` 生命周期清理，确保中转站胶囊、侧边浮标、抽屉面板及跨窗口模糊监听器在模块热重载或实例切换时彻底移除，根除雙层叠加重影
- 保持 BigBang 液态玻璃真背景模糊（`BackgroundBlurDrawable`）、现代矢量图标与交互状态机，以及 `HyperPortalEntry.onPackageLoaded` 首包/进程过滤与 `onHotReloading` 自动回收
