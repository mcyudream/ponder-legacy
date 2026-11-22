# Ponder Legacy 发布状态

## 正式发行候选：1.8.0

- 新增经典 / 现代主题及 JSON、ZenScript、资源包控制，支持新建、继承和局部覆盖；现代主题按官方高版本源码校准。
- 修复捕获 / 导入的火花在时间轴 0 刻提前显示；结构实体随所属选区显示、隐藏和移动，回拖与重播恢复初始状态。
- 361 项单元测试、正式 Cleanroom 0.4.0 / Java 25 的 1316 项运行检查通过，见 [1.8.0 验收](docs/verification/release-1.8.0-cleanroom-0.4.0/README.md) 和 [升级说明](docs/releases/1.8.0.md)。
- 生产 SHA-256：`8e1b765416a93df0b0513fb45a5fc941eceb7523c3d0f787208dd31f4a9299c7`。
- 运行验收完成；待主分支 CI 与实测包逐字节核对后创建正式标签。发行哈希门槛保持启用。

## 本地开发候选：1.8.0-alpha.2

修复结构内火花在时间轴 0 刻提前显示：捕获/导入的实体跟随所属选区的显示、隐藏和变换，倒拖与重播恢复初始隐藏状态；显式创建的实体保持原有时间语义。保留 alpha.1 的主题系统。

361 项单元测试、正式 Cleanroom 0.4.0 的 1315 项运行检查通过；上一测试包在相同的编辑器 0 刻用例中复现失败，见 [实体时间轴验收](docs/verification/structure-entity-timeline-1.8.0-alpha.2-cleanroom-0.4.0/README.md)。
本轮仅生成本地测试包，未创建发行标签或 GitHub Release。

## 本地开发候选：1.8.0-alpha.1

新增经典 / 现代主题，以及 JSON、ZenScript 和资源包主题接口。现代主题按高版本源码校准控件、标题、旁白、进度条与背景；支持小窗口和关闭后恢复 HUD。

356 项单元测试、正式 Cleanroom 0.4.0 的 800 项运行检查通过，见 [开发包验收](docs/verification/ui-themes-1.8.0-alpha.1-cleanroom-0.4.0/README.md)。
本轮仅生成本地测试包，未创建发行标签或 GitHub Release；下述 1.7.3 发行记录保留。

## 发行版本：1.7.3

- 结构框选捕获非玩家实体，保留植魔普通／升级火花和多媒体火花的类型与 NBT；新增 JSON / ZenScript 通用注册实体创建。
- 验证框选捕获、独立原版 NBT 导入、Java、JSON 和实际 ZenScript 五条路径，以及首次播放、重播和进度回拖。
- 正式 Cleanroom 0.4.0 / Java 25 的验收与产物指纹见 [1.7.3 验收](docs/verification/release-1.7.3-cleanroom-0.4.0/summary.json)，[升级说明](docs/releases/1.7.3.md)。
- 346 项单元测试、四组正式 Cleanroom 0.4.0 客户端／专服验收共 287 项检查通过，其中包含 70 次逐实体 framebuffer 比较。
- 生产 SHA-256：`9cef926fd3f34593900e766033c1e9cbb75b6b74835c8970e2880487c88a8273`。
- [GitHub 正式发行](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.7.3) 已设为 Latest；[主分支 CI](https://github.com/mcyudream/ponder-legacy/actions/runs/45035733016) 和 [标签 CI](https://github.com/mcyudream/ponder-legacy/actions/runs/45035971360) 均通过，发行哈希门槛保持启用。
- 4 个发布资产全部下载并校验，生产 JAR 与运行验收包逐字节一致，见 [published.json](docs/verification/release-1.7.3-cleanroom-0.4.0/published.json)。
- 旧结构若未捕获火花，需要重新框选保存，选区须包含火花所在的空气层。

## 历史发行版本：1.7.2

修复 `/pondergui`、`demo` 和 `edit` 界面被聊天框提交后的关闭操作覆盖的问题。

- 341 项单元测试、正式 Cleanroom 0.4.0 独立 GUI 专项 72 项检查、Ponder／FTB Quests 联合客户端 27 项检查和独立／联合专服检查通过。
- 生产 SHA-256：`6c2c6ddc9b267750dc498852885f7dabae2e20bfb4f4d5c3ffa2661734f082c1`。
- [正式发行](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.7.2) 已发布为非预览 Latest；主分支与标签流水线全部通过，4 个发布资产全部下载并校验，生产 JAR 与游戏验收包逐字节一致。
- 运行报告和截图见 [本版验收](docs/verification/release-1.7.2-cleanroom-0.4.0/summary.json)，说明见 [1.7.2](docs/releases/1.7.2.md)，下载校验见 [published.json](docs/verification/release-1.7.2-cleanroom-0.4.0/published.json)。

## 历史发行版本：1.7.1

- 提供完整 GUI 的分组布局、绘制层级、裁切、动态可见性与共享高亮坐标；修正分数缩放、退出世界重载与底部播放控件留白。
- 修正应用气动、血魔法 AE2S 附属和 AE2S 概率样板的简繁中文分类；兼容资源命名空间与 Mod ID 不一致的情况，支持中文名、英文名、命名空间与 Mod ID 搜索。
- 341 项单元测试；正式 Cleanroom 0.4.0 核心客户端 249 项、GUI 专项 62 项、实际三附属目录专项 157 项及两种专服各 11 项检查通过。独立 API jar 编译检查通过。
- 生产 SHA-256：`923cb6653eee53c96e4fbd9ac871c897cb151b91b3e0759c40ed4687611c6dd4`；[验收记录](docs/verification/development-1.7.1-cleanroom-0.4.0/summary.json)。
- [GitHub Release](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.7.1)；标签流水线强制核对已验收产物哈希。最低 Cleanroom 0.4.0 正式版 / Java 25。

## 历史开发版：1.7.0

- 新增时间轴 GUI 动画、W 实机引导、独立演示与 GUI 采集编辑，详见 [使用说明](docs/GUI-TUTORIALS.md)。
- 331 项单元测试、314 项正式 Cleanroom 0.4.0 运行检查通过；[验收证据](LEGACY-VERIFICATION.md)。
- 本次推送 main，触发 CI 构建；正式发行仍以 GitHub Release 页面为准。
- 最低 Cleanroom 0.4.0 正式版 / Java 25，外置兼容保持可选。

## 已发行：1.6.2

- 作者与维护者 **yudream**，原作者和来源见 [README](README.md) / [NOTICE](NOTICE.md)。
- 当前版本 **1.6.2**，标签 `v1.6.2`：索引模组分类支持简体／繁体中文、资源包自定义名称，以及中文名、英文原名和 Mod ID 搜索；切换语言保留分类与搜索词。
- [正式发行入口](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.6.2) · [CI](https://github.com/mcyudream/ponder-legacy/actions/workflows/ci.yml)。标签 CI 通过哈希门槛后创建正式版。
- Minecraft 1.12.2 / Java 25 / 最低 **Cleanroom 0.4.0 正式版（无 -alpha）**。
- 替换旧 Ponder jar；AE2S Ponder Legacy 0.2.0 可继续使用。应用气动 `air_network` 重复 ID 须另更新应用气动本体至 `1.0.9-cleanroom.2`；Ponder 保留真实重复注册诊断。
- 保留 Markdown 媒体面板、世界内投影与可选 Revo UI 适配；CraftTweaker、MMCE、FTB Quests、Media API 等外置联动保持可选。
- 314 项单元测试、341 项本版运行检查：核心完整流程、联合安装时的分类本地化／检索专项，以及两种专服组合；[验收记录](LEGACY-VERIFICATION.md)。应用气动修复包另通过有／无 Ponder 的客户端和专服四组合。
- 生产 SHA-256：`dddf7f51b50dcd4fa7ebb60e61ab0ddb196bcbff173c429e8a3aaf340c00494e`；CI 强制匹配已验收产物，不发布测试 harness。
- CurseForge / Modrinth 尚未上传。
