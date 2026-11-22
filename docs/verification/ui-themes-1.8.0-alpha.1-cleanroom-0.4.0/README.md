# 1.8.0-alpha.1 主题开发包验收

2026-11-22。Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 `-alpha`）** / Java 25.0.5。
本目录对应本地开发候选包，未创建发行标签或 GitHub Release。

安装包：`ponder-legacy-1.8.0-alpha.1.jar`。SHA-256：`9084ebe0f4dc1f7b76662ef1c7eb5346860988cbb91cc185e9ef514a4fb8bbec`。
356 项发布目录单元测试及保留现有可选集成的工作区 357 项单元测试通过。

| 场景 | 结果 | 运行检查项 |
| --- | --- | --- |
| 现代主题 / CraftTweaker / 中文 | PASS | 232 |
| 现代主题 / 无 CraftTweaker / 英文 | PASS | 229 |
| 完整经典播放器、指南、编辑器、投影回归 | PASS | 252 |
| 原生 GUI 引导、演示与采集编辑回归 | PASS | 72 |
| 专服 / 无 CraftTweaker | PASS | 12 |
| 专服 / 实际主题 ZenScript | PASS | 3 |

共 800 项运行检查。所有组合运行正式 SRG 安装包，`runtime.json` 记录安装器、生产 JAR、harness 和全部库的指纹；classpath 中没有 userdev 或开发版 Minecraft。

主题专项覆盖 JSON 新建、继承、内置覆盖、非法定义回退、资源包实际重载、ZenScript 实际启动声明、玩家优先级、聊天命令、同一场景暂停与时间保持。现代布局检查原版按钮位置、20×20 主体及外扩边框、细进度条、边框点击、侧栏点击不旋转场景；实机检查 1280×720、854×480 和 640×480（320×240 逻辑界面）。验证背景模糊初始化、禁用 framebuffer 后的纯色回退、关闭页面后恢复 HUD 和手臂，以及原生熔炉 GUI 贴图与侧栏无交叠。截图来自实际游戏 framebuffer，绘制检查均无 OpenGL 错误。

外观参照固定为官方 Ponder `mc1.21.1/dev` 的 `89819291b726708d119b118dea01667eae0b64c9`。校准标题图标、横向渐隐条、旁白细边框与水平引线、主控件顺序、快捷键和进度条。扩展功能入口继续保留；场景相机采用本实现的 1.12 渲染方式。

## 投影测试方法校正

旧指南测试按半透明原生箱子的棕色阈值判断是否绘制；天空背景混合使该阈值在已发布的 1.7.3 上同样失败。`harness-baseline-1.7.3/` 保留该对照，不计入本版通过数。新的测试逐帧比较正常箱子与临时失效箱子的 framebuffer 像素差，恢复箱子后继续原场景。仅修改验收 harness，未修改生产投影代码。

## 范围

Linux x86_64 / llvmpipe (LLVM 21.1.8)。尚未扩展到其他 GPU、Windows/macOS 或完整用户整合包。CraftTweaker 可选；专服验证主题数据与实际 ZenScript 声明安全，客户端主题不会强制同步给玩家。

完整数据见 [summary.json](summary.json)，使用方法见 [主题手册](../../UI-THEMES.md)。
