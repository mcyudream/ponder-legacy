# Ponder Legacy 1.8.0 安装包验收

2026-11-22。Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25.0.5。
生产 JAR：`ponder-legacy-1.8.0.jar`。SHA-256：`8e1b765416a93df0b0513fb45a5fc941eceb7523c3d0f787208dd31f4a9299c7`。

361 项单元测试通过；使用同一生产 JAR 在正式安装运行环境通过 1316 项检查。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| 植魔 / Baubles / CraftTweaker：五条实体路径和时间轴 | PASS | 383 |
| 植魔 / Baubles，无 CraftTweaker | PASS | 333 |
| 无可选 Mod：原版实体、编辑器时间轴 | PASS | 23 |
| 实际 ZS 主题、JSON、资源包及现代布局 | PASS | 232 |
| 播放器、编辑器、指南、媒体和投影回归 | PASS | 252 |
| 原生 GUI 引导、演示与采集编辑 | PASS | 72 |
| 无可选 Mod 专服 | PASS | 15 |
| 植魔 / Baubles / CraftTweaker 专服及 ZS 主题声明 | PASS | 6 |

主题覆盖内置经典/现代、JSON 新建/继承/覆盖、实际 ZS 启动声明、资源包重载、无效定义回退、玩家选择、聊天命令、暂停和场景保持。实机验证 1280×720、854×480 和 640×480（320×240 逻辑界面）、主控件位置与外框点击、背景模糊与无 framebuffer 回退、HUD/手臂恢复、原生机器 GUI 与侧栏留白。

实体覆盖七种真实植魔火花的框选捕获、独立原版 NBT 导入、Java、JSON 和实际 ZS 五条路径；默认三步编辑器场景、播放器 0 刻/延迟显示/隐藏/移动/重显/关键帧恢复/回拖/重播均验收。98 次可见实体 framebuffer 比较通过，最少 43 个变化像素；42 次隐藏状态比较全部为 0 个变化像素，7 次像素位置比较确认实体跟随独立选区移动。无可选 Mod 时另用原版捕获实体验证同一时间轴语义。

alpha.1 旧包在相同 0 刻用例复现预期失败，见 `negative-alpha1/`；不计入本版通过数。指南投影测试继续使用正常/临时失效箱子 framebuffer 差分，避免旧棕色阈值受天空混合影响，生产投影代码未因此修改。

所有截图来自实际 framebuffer，绘制检查无 OpenGL 错误。运行 classpath 使用正式安装 JAR，未加入 userdev 或开发版 Minecraft。每组报告和运行指纹在 `cases/`；复现 NBT、默认三步 JSON、真实 ZS 在 `fixtures/`，截图在 `screenshots/`。植魔 r1.10-364.4、Baubles 1.12-1.5.2、CraftTweaker 4.1.20.698 按组合安装，均保持可选。

Linux x86_64 / llvmpipe；未扩大到其他平台/GPU 或完整整合包。发布与下载校验状态见 [summary.json](summary.json)。

主分支 CI `45050032836` 和标签 CI `45050248234` 均通过；[1.8.0 正式版](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.8.0) 已设为 Latest。
三个 JAR 与本地及两次 CI 产物一致，生产 JAR 与上述八组实测包逐字节一致。
主题示例 ZIP 由发行提交 `b728e70322ee546c50fb9084578ae5ff2324f3d0` 中的 Git 文件生成，ZIP 条目时间戳和权限固定。
当前 GitHub OAuth 登录不具备修改工作流的权限，因此沿用既有发行流水线，单独上传同一提交生成的主题示例和覆盖四个文件的最终 SHA256SUMS。
5 个发布资产已全部重新下载，大小、SHA256SUMS 和 GitHub 资产摘要均一致，见 [published.json](published.json)、[主分支 CI](ci-main.json)、[标签 CI](ci-tag.json) 和 [主题示例来源](theme-examples-origin.json)。
