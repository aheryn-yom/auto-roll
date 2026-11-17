# AutoRoll MVP 实施计划

## 目标

交付无 Root 的 Android 自动滑屏 MVP：悬浮点控制器、间歇滑屏、近似连续微滑动、三档速度；适配 Android 8 至 Android 17，并针对 Xiaomi 17 Pro Max 的高分辨率/高密度屏幕按运行时尺寸计算手势位置。

## 决策

- Kotlin + 原生 Android View；单个 `AccessibilityService` 管理 overlay 和手势，避免服务间状态同步。
- 使用 `TYPE_ACCESSIBILITY_OVERLAY`，不申请 `SYSTEM_ALERT_WINDOW`。
- `minSdk 26`、`compileSdk/targetSdk 37`；Android 17 设备行为以真机测试最终确认。
- 手势结束回调后才调度下一次；停止通过运行代次使旧回调失效。
- SMOOTH 定义为短距离高频微滑动近似连续，明确不承诺跨阅读器恒定像素速度。

## 进度

- [x] 建立 Android Gradle 工程与 API 37 配置
- [x] 配置和状态模型、速度预设
- [x] 手势执行器与循环控制器
- [x] 无障碍服务、可拖动悬浮控制器
- [x] 权限引导、使用说明和验收文档
- [x] Gradle 工程任务与依赖解析、Manifest/XML、权限边界和空白检查
- [x] API 37 编译与单元测试
- [ ] Xiaomi 17 Pro Max 真机验收（当前未连接 Android 设备）

## 交互优化

- [x] 移除悬浮点白底和应用图标白色内圆；悬浮点半透明，菜单保持不透明
- [x] 将悬浮标记缩小一半并改为半透明深灰色
- [x] 用三档速度滑动条替换速度按钮，将模式按钮移至滑动条下方并点击即启动
- [x] 重跑构建、单元测试和 Lint

## 精细速率调节

- [x] 设置菜单改为不透明面板
- [x] 速度条支持 0.1% 连续调节并显示当前数值；慢/中/快作为插值参考点
- [x] 重跑构建、单元测试和 Lint
