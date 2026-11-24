# HyperPortal v0.2.0-ai15

## 合并上游 0.1.6 / 0.1.7

本次把 `Leaf-lsgtky/HyperPortal` 的 6 个上游提交合入本 fork（共同祖先 `e7ac8b1`），
在保留全部本地改造的前提下吸收上游的拖拽健壮性修复。

### 上游带来的修复

- **忽略非传送门拖拽**（`94cc338`）：`Bridge` / `PortalSystemUiModule` / `PortalSystemModule`
  统一用 `application/x-dragshare` 做 MIME 门控，普通系统内拖拽不再被误当成传送门拖拽；
  `isOurDrag()` 不再依赖 `sDragActive` 兜底，`onDragClosed()` 改为非本模块拖拽直接放行。
- **中转站拖拽保留原生行为**（`f2beb87`）：新增 `TransferDockContract.DRAG_MIME`
  （`application/x-hyperportal-transferdock`），文本卡片拖出也带自定义 MIME，
  由 `isShareFallbackDrag()` 判定后走原生分享链路。
- **拖回右侧不再收起**（`723ca76`）：面板让位判断从「窗口内坐标」改为「手指到屏幕边缘的绝对距离」，
  并加了 56dp / 130dp 的滞回区间与 260ms 起手保护，解决滑出后又立刻收回的问题。
- **不再重启货架收起动画**（`0a0776f`）：`handleDragStateChanged` 用 `previousState` 判断，
  state 1 只启动一次退出动画、state 2 先 `cancel()` 再复用，`ColorOSDockBar` 同时补齐
  `mMagnificationReset` 与 animator 竞态处理，消除连续位置回调导致的动画抖动。

### 本 fork 的适配

- `TransferDockPanel` 保留本地整套液态玻璃外观（`LiquidGlassRimDrawable`、`GlassDividerDrawable`、
  44dp 模糊、动态面板尺寸/圆角/左右锚点），只替换拖拽判定逻辑。
- 上游的滞回判定原本只按「距右边缘」计算；本 fork 支持从左侧 Dock 打开，
  因此把度量抽成 `distanceFromDockEdge()`，`mAlignRight` 为真取右边缘距离、为假取左边缘距离。
- `PortalSystemUiModule` 保留本地 `OPEN_TRANSFER_DOCK` 的 `dock_left/dock_top/dock_width/
  dock_height/dock_corner_radius` 锚点参数与 `ACTION_TEARDOWN_OVERLAY` 热重载清理。
- `tools/build.py` 采用上游 AES versionCode `1000000051`；`bridge` 版本号保持本 fork 线
  （`versionCode 26` / `0.2.0-ai15`）。
