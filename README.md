# Ponder Legacy


**1.7.1 GUI 布局修复**：复杂 GUI 演示可使用统一的 `GuiDemoLayout` 分组、图层与裁切。
绘制元素和高亮框读取同一坐标树，弹窗、侧栏和隐藏控件按当前状态处理，分数缩放按边缘取整。
[完整界面 API](docs/GUI-TUTORIALS.md#完整界面布局-api171)。

[![构建与发行](https://github.com/mcyudream/ponder-legacy/actions/workflows/ci.yml/badge.svg)](https://github.com/mcyudream/ponder-legacy/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/mcyudream/ponder-legacy?include_prereleases)](https://github.com/mcyudream/ponder-legacy/releases)

![Ponder Legacy](branding/cover.png)

<!-- legacy-overview:start -->
为 Minecraft 1.12.2 整合包和 Mod 附属提供分步骤、可回放的三维玩法演示。玩家可以从物品进入教程；作者可以在游戏内捕获结构、编辑时间轴，再通过 Java、ZenScript 或 JSON 分发场景。

## 当前状态

当前开发版本为 **`1.7.1`**，新增 **时间轴 GUI 动画片段**：在机器演示的指定环节，于侧边空白区展示 GUI，支持控件高亮、演示物品、文字讲解、截取帧与定格，并跟随场景暂停、跳转及重播。保留机器界面长按 **W** 的实机引导、只读操作检测、独立演示与游戏内 GUI 采集编辑器。动画编辑器、Markdown、JSON 与 Java API 共用定义，见 [GUI 教程手册](docs/GUI-TUTORIALS.md)。现有双展示、媒体插入、世界投影和可选 Revo UI 适配继续保留。CraftTweaker、MMCE、FTB Quests、Media API 等外置联动均为可选。最低运行基线为 **Cleanroom 0.4.0 正式版（无 `-alpha`）**；已发布版本与开发版的验收状态分别记录在 [发布状态](PUBLICATION.md) 与 [验收记录](LEGACY-VERIFICATION.md)。

安装包从 [GitHub Releases](https://github.com/mcyudream/ponder-legacy/releases) 获取，选择无分类后缀的 `ponder-legacy-*.jar`。从 `1.4.1` 起，仅使用 ZenScript 时需要独立安装 CraftTweaker；`1.4.0` 仍要求安装。发行状态见 [PUBLICATION.md](PUBLICATION.md)。

<!-- legacy-authorship:start -->
## 作者与来源 / Authors and upstream

- 此 Legacy 版本作者与维护者 / Legacy author and maintainer：**yudream**。
- 原作者 / Original author(s)：**The Create Team (original Ponder), MCMGGF (1.12.2 base)**。
- 原仓库 / Original repository：[MCMGGF/ponder-for-1.12.2](https://github.com/MCMGGF/ponder-for-1.12.2)。
- 固定上游提交：[`c494bf7692c7`](https://github.com/MCMGGF/ponder-for-1.12.2/commit/c494bf7692c7b7363ef86ed82954874be6e49074)。
- Ponder 原始项目：[Creators-of-Create/Ponder](https://github.com/Creators-of-Create/Ponder)。
- 本版本独立维护；上游版权、许可证及资源来源声明保留于 LICENSE / NOTICE 文件中。
<!-- legacy-authorship:end -->

## 运行环境与身份

| 项目 | 要求 / 标识 |
| --- | --- |
| Minecraft | 1.12.2 |
| 运行基线 | **Cleanroom 0.4.0 正式版（无 `-alpha`）** |
| Java | 25 |
| 加载器 Mod ID | `ponder-legacy` |
| 资源与原注册命名空间 | `ponder`（保留） |

开发 SDK 可以较新，兼容结论来自对应版本的实际运行验证。历史 Mod 的 alpha 版本号与加载器版本无关。
加载器身份改名后，安装时应替换本项目旧 jar；原资源命名空间的保留不构成跨实现的存档转换承诺。

## 功能与迁移范围

- 物品教程入口、主题与模组分类索引，同一物品可关联多个场景。
- 游戏内结构框选、NBT 捕获、局部 section 动画、时间轴拖动、撤销/重做和保存重载。
- 保留 Java 插件、Forge IMC / ServiceLoader、ZenScript 与 JSON 场景入口。
- Markdown 一份教程编排两种展示，支持按展示特化；静态页含搜索目录、独立滚动、原生交互 3D 快照、物品、配方和图片。
- 场景一键进入世界内预览：准星定位，左键固定、右键旋转、滚轮调高低；透明模型与轮廓可见。背包材料实时检查；材料目标可钉住 HUD／背包、拖拽并保存位置。
- 图片、视频和 FTB Quests 任务链接；Media API、FTB Quests 均为可选依赖。
- Markdown 源码编辑提供多行选区、撤销重做、校验保存与预览；[完整格式与 API](docs/MARKDOWN.md)。

## 与上游的具体差异

| 项目 | 上游基线 | 本 Legacy 版本 |
| --- | --- | --- |
| 运行环境 | 既有 1.12.2 / Forge 回迁实现 | 迁至 Java 25 / Cleanroom；以 0.4.0 正式版为运行基线，开发 userdev 版本单独记录。历史包的 0.4.0-alpha 验收单独保留。 |
| 编辑工具 | 原场景与脚本 API | 新增 F7 编辑器、F8 框选、结构保存、局部位移/旋转编排及 JSON pack 编辑。 |
| 教程导航 | 原有教程入口 | 增加模组/主题筛选、搜索、滚动位置保留及多场景下拉切换。 |
| 默认内容 | 上游附带演示脚本 | 默认不向整合包注入示例；仅移走内容未改动的旧默认示例，保留用户改过的文件。 |
| 可选模块 | 上游含 MMCE 可选模块 | 接入可选 MMCE 结构 Provider、蓝图 Resolver、ZenScript / Java API；MMCE 独立安装，具体组合见 [MMCE 接入手册](docs/MMCE.md)。 |
| 提示框 | 使用原版 Forge 提示框坐标 | Revo UI 安装后按实际行布局定位进度条，兼顾标题换行、模型预览、行距与字号；Revo UI 保持可选。 |
| 服务端 | 0.4.0 / 0.4.0-alpha 模组发现会触发 Kirino 客户端入口 | 加入只对这两个精确版本物理专服生效的单处发现调用修复，客户端和其他版本不启用。 |

## 尚未实现与兼容边界

- 结构框选捕获方块、状态和方块实体 NBT，不自动捕获实体或掉落物。
- 远程结构捕获需要服务器安装相同版本并授予相应权限；编辑器文件保存在客户端。
- 不承诺与现代官方 Ponder 的全部二进制 API 互换。

## 依赖

- 必需：Cleanroom 0.4.0 正式版（无 `-alpha`）与 Java 25；Cleanroom 提供 MixinBooter 兼容接口。
- 可选：CraftTweaker 4.1.20+（验证版本 4.1.20.698），用于加载 ZenScript 场景；未安装时 JSON、Java API、结构捕获和编辑器仍可使用。
- 可选：MMCE 2.3.2，安装后启用机器结构与蓝图入口；无需额外安装 Ponder-MMCE 附属包。MMCE 2.3.2 自身要求 CraftTweaker，安装它时仍须满足该依赖。
- 可选：Revo UI 0.8.7 / NFR 0.8.0 / ModularUI 3.2.0-nfr.2；按其官方要求独立安装，见 [适配说明](docs/REVO-UI.md)。
- 可选：Media API 1.0.2，提供视频解码；FTB Quests，提供任务跳转、共享媒体与动态图片，见 [Markdown 手册](docs/MARKDOWN.md)。
- 作为其他 Mod 的教程运行库时独立安装；附属应以可选兼容方式接入。

## 使用方式

1. 悬停已注册教程的物品并按住 W；`/ponder index` 打开目录。
2. 界面左上角切换动画／图文，星号设置长按 W 的默认展示；图文页 F7 编辑 Markdown。
3. 创造模式按 F8 框选，再按 F7 保存结构和编辑场景。
4. 分发 `scripts/ponder/guides/` 的 Markdown、`scripts/ponder/packs/` 的 JSON 与 `scripts/ponder/structures/` 的 NBT；场景按作者自己的命名空间组织。

## 构建与 CI

在完整源码项目目录使用 Java 25 执行：

```bash
python3 tools/bootstrap_legacy.py
bash gradlew --no-daemon :build :reobfJar --console=plain
python3 tools/verify_legacy_package.py
```

CI 会准备固定版本的源码依赖，构建完整主源码与测试，并检查发行 jar 的身份、作者、图标和可选适配边界。
推送 `main` 或提交 PR 时执行构建；推送与包版本对应的 `v*` 标签时，还会核对已验收包 SHA-256，成功后自动创建 GitHub Release，上传安装包、API、源码与 `SHA256SUMS`。Alpha 标签发布为预发行版。CI 构建不能替代客户端玩法和专服验收。

## 发布平台

GitHub 发行流程、CurseForge / Modrinth 项目资料和平台凭据配置见 [发布说明](docs/PUBLISHING.md)。平台状态见 [PUBLICATION.md](PUBLICATION.md)。

## 授权与图像

沿用上游授权；原始 Ponder、1.12.2 回迁与本版本的来源分别记录在 LICENSE / NOTICE.md。

此版本作者为 **yudream**，上游作者与版权保留如上。`branding/icon.png` 和 `branding/cover.png` 为本项目生成的展示图，
以代表物品或概念表现项目；不作为游戏实机截图，也不改变上游游戏资源的授权。生成记录见 `branding/provenance.json`。

<!-- legacy-overview:end -->

## 详细使用与开发说明

面向 **Cleanroom / Java 25** 的思索开发分支，版本 `1.6.2`，最低加载器声明为 **0.4.0 正式版**。
编译与编辑器开发工具链使用 `0.7.14-alpha` userdev；当前安装包与历史快照见 [验收记录](LEGACY-VERIFICATION.md)，Revo UI 组合见 [适配说明](docs/REVO-UI.md)，历史 MMCE 证据见 [运行验收](docs/CLEANROOM-VERIFICATION.md)。
基于 [MCMGGF 的 1.12.2 实现](https://github.com/MCMGGF/ponder-for-1.12.2)，保留原有场景、Catnip、ZenScript、JSON 与插件 API；新增游戏内场景和动画编辑器。[来源说明](NOTICE.md)。

## 构建与本地开发

```bash
# 本机已经准备了 Java 25；其他机器可以使用自己的 Java 25
export JAVA_HOME=/home/yudream/code/mods/.dev/jdk-25.0.5+8
./gradlew build
./gradlew runClient
# 自动建立测试世界、捕获结构、编辑动画、保存重载并截图后退出
./gradlew runClient -PponderSmoke
# 本机快捷入口（自动选择已准备的 Java 25）
./tools/dev.sh runClient
```

首次构建需要联网获取 Cleanroom、CraftTweaker、Minecraft 和原版资源。
本地开发环境使用 MCP 命名，资源与世界保存在工程 `run/client/` 目录。
默认 Gradle/JDK 配置、运行说明和 API 示例见 [Cleanroom 开发手册](docs/CLEANROOM.md)。

安装时使用 `build/libs/ponder-legacy-1.6.2.jar`；仅使用 `.zs` 场景时需要安装 **CraftTweaker 4.1.20.698**。
Cleanroom 已提供 MixinBooter 兼容接口。`-mcp`、`-api`、`-sources` 文件用于开发，不要作为重复模组安装。
Mod ID 保持 `ponder-legacy`；资源空间为 `ponder`。客户端与安装了 Ponder 的服务器需要使用相同版本。

## 编辑器操作

1. 进入创造模式世界，按 **F8** 开启框选工具：左键角 A，右键角 B。画面会显示区域边框。
2. 按 **F7** 打开编辑器，点击「保存结构」，填写结构 ID（如 `mypack:machine`）。选区方块、状态和方块实体 NBT 会保存到本地。
3. 在预览中开启「选择区域」，左、右键选取场景局部。点击「快捷动画」，填写位移、旋转和持续 tick。
4. 拖动时间轴预览；步骤可以拖动排序、修改参数、复制、删除。`Ctrl+Z/Y` 撤销/重做，`Ctrl+S` 保存并重载。
5. 点击「打开」可编辑已有的 JSON pack。按住预览右键或中键旋转，滚轮缩放。

快捷动画把局部转为独立 section，可同时位移和旋转，并自动插入等待。
复用已有 section 的句柄，可以继续编排下一段动画。单独动作的 `duration` 不推进故事板游标，`scene.idle` 才推进时间。

| 数据 | 位置（相对于游戏目录） |
| --- | --- |
| JSON 场景包 | `scripts/ponder/packs/**/*.ponder.json` |
| 捕获的结构 | `scripts/ponder/structures/<namespace>/<path>.nbt` |
| 旧默认示例备份 | `config/ponder/removed-examples/` |
| 自动客户端报告和截图 | `ponder-smoke/report.json`、`ponder-smoke/screenshots/` |

普通思索入口保留：悬停物品按 **W**，或 `/ponder <物品 ID>`；`/ponder index` 打开索引，`/ponder reload` 重载。
物品提示框中的按住进度使用铺满文字区宽度的横条，自动适应长物品名称和换行。

## 指引目录与分类

默认不安装八个演示脚本和编辑器示例；目录只收录整合包和其他 Mod 注册的内容。
升级时，旧版本生成且内容完全未改动的示例会移至 `config/ponder/removed-examples/`，不再加载；修改过的示例和自定义文件会保留。
示例源仍在源码资源与 `examples/` 中，方便手动参考，不会自动注册到游戏。

`/ponder index` 左侧可切换「模组」或「主题」，点击分类筛选，数字显示符合搜索条件的物品数。
主题直接沿用 Java / ZenScript / JSON 的 Ponder 标签；同一物品会合并所有场景的主题，未分配可见主题的物品显示在「未分类」。
右上搜索支持物品名称、模组名和注册 ID，Ctrl+F 聚焦搜索；Enter / Esc 结束输入。
物品、主题、主题内容及左侧分类均有独立滚动区域，支持滚轮与拖动滚动条；物品网格支持 PgUp / PgDn、Home / End。
底部工具栏保持固定，缩放窗口及返回上一页时保留并校正浏览位置。多场景物品只显示一次，悬停可查看场景数量。

## 同一物品的多个情景

多个场景使用同一个 `component`，各自保留不同的 `id`；Java / ZenScript 的多个 storyboard 关联同一物品也使用相同入口。
播放时悬停右上角的场景计数会展开菜单，点击计数可固定展开；点击列表中的标题可直接切换。
菜单支持滚轮、方向键、Home / End、Page Up / Down 和回车；`Tab` 开关，`Esc` 或点击外部关闭。
单场景不显示菜单入口。导航按钮在首尾置灰，播放、重放、识别和关闭均有独立图标及悬停说明。
可直接复制的双场景 JSON 见 [multi-scene.ponder.json](examples/json/multi-scene.ponder.json)。

## JSON 与扩展 API

- JSON 场景格式与原有 1.12.2 实现兼容：[格式说明](docs/JSON-PACKS.md)、[完整操作表](schemas/ponder-operations-v1.json)、[JSON Schema](schemas/ponder-pack-v1.schema.json)。
- Java 插件：`PonderPlugin`、`SceneBuilder`、`PonderIndex.addPlugin`；支持 ServiceLoader / Forge IMC。
- 自定义指令：`ScriptInstructionCodec` / `ScriptInstructionCodecs`，在 JSON 使用 `scene.custom`。
- 编辑器 API：`SceneDocument`、`ScenePackIO`、`CuboidSelection`、`EditorAnimations`、`EditorActions.register`。

## 范围

这是可本地维护的 Alpha 分支。框选目前捕获方块和方块实体，不自动捕获生物或掉落物；实体可以通过动作指令创建。
远程服务器的选区捕获需要服务器安装相同版本的 Ponder，并具有创造或 OP 权限；编辑器保存到客户端本地，整合包发布时需分发 JSON 与 NBT。

MMCE 软适配的使用方式、实测版本和限制见 [MMCE 接入手册](docs/MMCE.md)。本分支不声明兼容旧 Forge/Java 8 或现代官方 Ponder 的全部二进制 API。
旧 Forge 文档及验证工具作为参考保留在 `docs/`、`tools/`、`verification/`，当前入口以本 README 和 Cleanroom 手册为准。
