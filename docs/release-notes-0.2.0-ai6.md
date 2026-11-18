# HyperPortal v0.2.0-ai6

- 1:1 复刻液态玻璃 v2 预览设计：BigBang 分词主面板、悬浮胶囊操作条与 AI 翻译弹窗接入 `ViewRootImpl.createBackgroundBlurDrawable()` 局部圆角真磨砂背景模糊，彻底消除底层页面锐利文本透出造成的视觉重影
- 全面升级现代液态玻璃矢量图标（顶栏编辑/全选/分享/更多、底栏 OCR/关闭/语言、操作条搜索/AI 翻译/拆字/分享/复制）与浅/深色渐变选中态质感，严格保留原版面板尺寸、进出场动画、默认不选词、滑动连选与选词后动态弹出逻辑
- 修复 SystemUI 侧 `重影` 根因：`HyperPortalEntry.onPackageLoaded` 限定首包与 `com.android.systemui` 主进程，`onHotReloading` 自动清理旧 `SystemUiOverlay` 实例与广播接收器；`PortalSystemUiModule` 切换状态时先取消残留动画并重置位移/透明度，为隐藏回调增加状态保护
