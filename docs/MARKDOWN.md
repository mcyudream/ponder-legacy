# Markdown 教程：一份内容，两种展示

从 Ponder Legacy 1.5.0 起，Markdown 可以直接编排动画，同时提供静态图文页面。Minecraft 1.12.2 / Cleanroom 0.4.0 正式版 / Java 25；无需 CraftTweaker 或现代 GuideME。

## 玩家入口

只注册 **W**。悬停物品并长按 W，按玩家的默认展示设置打开教程。动画左上角可切到图文，图文左上角可切到对应动画；星号按钮将当前展示设为默认并保存到客户端配置。仅有一种展示的教程自动使用可用形式。

静态页左侧可搜索正文和标题、选择教程和目录。正文与左栏分别滚动；正文支持滚轮、Page Up/Down、Home/End 和拖动滚动条。拖动 3D 场景旋转，Shift+滚轮缩放。物品可显示原生提示，支持的物品链接可进入相关教程。图文页按 Tab 可切至对应动画。

### 索引的模组名称

从 1.6.2 起，思索索引的“模组”分类使用当前语言的名称。简体／繁体中文已包含 Minecraft、AE2S / Supergiant、应用气动、MMCE、FTB Quests 等名称；英文界面保留模组原名。分类标题、悬浮说明、分类排序和搜索使用同一套名称，原英文名与注册命名空间仍可用于搜索。切换语言后刷新标签，保留当前分类、搜索词及 Mod ID。

整合包可在资源包的 `assets/<任意命名空间>/lang/zh_cn.lang` 覆盖或新增：

资源包的 `pack.mcmeta` 使用 Minecraft 1.12.2 的 `pack_format: 3`，以加载上述小写语言文件名。

```properties
ponder.mod_name.ae2=应用能源 2 超巨星
ponder.mod_name.ae2s-appliedpneumatics=应用气动
ponder.mod_name.bmaddon-legacy=血魔法 AE2S 附属
ponder.mod_name.ae2s-probability-pattern-legacy=AE2S 概率样板
ponder.mod_name.your_mod_id=你的模组中文名
```

查找顺序为 `ponder.mod_name.<物品注册命名空间>` → 模组提供的 `mod.<命名空间>.name` → 所属 Mod ID 的同类翻译 → Forge 元数据名称 → 命名空间。已适配 `bmaddon` → `bmaddon-legacy`、`statpatterns` → `ae2s-probability-pattern-legacy`、旧 `appliedpneumatics` → `ae2s-appliedpneumatics` 和 `ponder` → `ponder-legacy`。直接注册在命名空间下的模组元数据优先于别名元数据。中文名、英文原名、资源命名空间与所属 Mod ID 均可搜索。翻译只影响显示与搜索，不修改场景 ID 或物品注册名。资源包重载后重新打开索引即可应用同语言下的新覆盖。

## 最小共享教程

放到 `scripts/ponder/guides/mypack/first_machine.md`。结构 `mypack:machine` 可用现有 F8 框选工具捕获，保存在 `scripts/ponder/structures/mypack/machine.nbt`。

````markdown
---
id: mypack:first_machine
title: 第一台机器
item_ids: [minecraft:furnace]
structure: mypack:machine
presentation: both
step_ticks: 160
---
# 第一台机器

## 放置机器

放置熔炉，并为输入、燃料和成品留出不同路线。

```ponder
[
  {
    "op": "world.show_section",
    "selection": {"type": "position", "pos": [1, 1, 1]},
    "direction": "down"
  }
]
```

## 移动示意

将这一组方块向右平移一格。这里是演示动画。

```ponder
[
  {
    "op": "world.show_independent_immediate",
    "handle": "machine",
    "selection": {"type": "position", "pos": [1, 1, 1]}
  },
  {"op": "world.move_section", "handle": "machine", "x": 1, "y": 0, "z": 0, "duration": 30},
  {"op": "scene.idle", "ticks": 35}
]
```
````

H2 标题划分步骤。每步的正文成为动画讲解，`ponder` 围栏中的 JSON 数组进入现有指令校验器和执行器；作者可使用位移、旋转、方块、NBT、物品移动、提示、等待等现有指令。编译器添加步骤关键帧、讲解停留和完成指令，静态页在移动完成后的步骤时刻显示同一场景的快照。

动作操作和必填字段见 [指令表](../schemas/ponder-operations-v1.json)。`scene.idle` 会推进动作编排时间；单个动作的 `duration` 表示持续时间，不会自动顺序等待。不要自行添加 `scene.finish`；Markdown 编译器会结束教程。开始时结构整体可见，需要逐步搭建时可先隐藏需要演示的区域。

## 元数据

机器界面也可以作为动画片段，随同一时间轴展开、高亮讲解与收起，并在图文页使用同一份定义。格式、编辑入口与 W 实机引导见 [GUI 教程](GUI-TUTORIALS.md)。

| 字段 | 用途 |
| --- | --- |
| `id` | 稳定的教程 ID，`命名空间:路径`；游戏内编辑时保持不变 |
| `title` | 页面和动画标题，缺省取第一个标题 |
| `item_ids` | 入口物品列表，可用缩进的 `-` 列表或方括号列表 |
| `structure` | 用原生 NBT 结构生成动画，与 `scene` 二选一 |
| `scene` | 引用现有 Java／JSON／ZenScript 动画 ID，不替换该动画 |
| `presentation` | `both`、`guide` 或 `animation`；默认为 `both` |
| `step_ticks` | 每步自动停留时间，20–1200，默认 160 tick |
| `tags` | 已注册的 Ponder 主题 ID 列表 |
| `categories` | 静态导航分类列表，使用第一个分类 |
| `navigation` | 支持缩进的 `title`、`icon`、`parent`、`position` 元数据；未指定 categories 时用 parent 作分组 |

前置区实现的是明确的 YAML 子集：标量、字符串列表、navigation 映射。不支持任意 YAML 对象、锚点或脚本。资源 ID 需要完整命名空间，不接受 `..`。文件最大 512 Ki 字符，本地目录总大小最大 16 MiB。

## 为两种展示定制内容

````markdown
<GuideOnly>

## 参数与故障排查

| 症状 | 检查 |
| --- | --- |
| 停止输入 | 目标已满，或过滤不匹配 |

</GuideOnly>

<AnimationOnly>

这一段只作为动画讲解。

```ponder
[{"op":"scene.rotate_camera","degrees":45}]
```

</AnimationOnly>
````

`presentation: guide` 可以完全没有动画；`presentation: animation` 不生成静态页。已有 Java／JSON 教程未写 Markdown 时，会从原生讲解和时间轴自动生成图文回退页。

## 原生静态组件

借助 GuideME 的 Markdown/MDX 解析器，支持标题、段落、粗体／斜体、列表、代码、链接、图片和 GFM 表格，以及这些原生组件：

````markdown
<ItemLink id="minecraft:iron_ingot" />
<ItemImage id="minecraft:furnace" />

<RecipeFor item="minecraft:furnace" fallbackText="当前整合包没有这项配方。" />
<Recipe id="minecraft:furnace" />

<GameScene scene="mypack:first_machine" tick={80} interactive={true} />

<GameScene yaw={-35} pitch={25} zoom={1} interactive={true}>
  <ImportStructure src="mypack:machine" />
  <Block id="minecraft:chest" x={2} y={1} z={1} p:facing="north" />
  <BlockAnnotation x={2} y={1} z={1} color="#00ff00">产物容器</BlockAnnotation>
  <BoxAnnotation min="1 1 1" max="3 2 2" color="green" />
  <LineAnnotation from="1.5 1.5 1.5" to="2.5 1.5 1.5" color="white" />
</GameScene>
````

- 每个独立 GameScene 可以导入一份原生结构，使用其已保存坐标；也可直接使用 Block 组成场景。链接现有场景时不混入结构修改。
- 摄像机缩放为原生渲染倍率（大于 0，不超过 8）。不等同于现代 GuideME 的像素缩放参数。
- 注解接受 Ponder 调色板名或 `#RRGGBB`；十六进制色映射到最接近的原生调色板色。
- `ItemIcon`、`ItemGrid`、`RecipesFor`、`Row`、`Column`、`Color`、`OptionalSection` 也可解析。当前 Row/Column 使用顺序排版，OptionalSection 不求值条件，Color 使用正文样式。
- 配方来自当前游戏实际的合成／冶炼注册表；多配方循环显示。现代 GuideME 专有标签、实体场景、任意 JavaScript及任意 JEI 分类配方没有直接移植。未知组件会报告错误。
- 图片请用完整资源 ID，例如 `mypack:textures/guide/diagram.png`。链接支持同命名空间页面、标题锚点和 `item:minecraft:furnace`。一个文本块的链接区域跟随其第一个链接；索引页建议每行放一个链接。

## 图片、视频和 FTB Quests 任务

### 游戏内插入面板（1.6.1 起）

在图文教程中按 **F7** 打开 Markdown 编辑器，顶部选择 **插入图片** 或 **插入视频**：

- 点击 **选择本地文件**，在游戏内选择图片／视频；可进入目录，也可粘贴文件或目录的完整路径后点 **打开路径**。外部文件自动复制到 `scripts/ponder/media/`，生成 `local:` 引用；同名内容复用，内容不同时另存名称，不覆盖已有文件。中文文件名会转换为可引用的文件名。
- 也可直接填写 `local:`、完整资源 ID、HTTP(S) 链接或 `media:` 任务媒体来源；视频可填写 Media API 支持的 Bilibili BV / av 号。
- 填写标题／图片说明、可选宽高；视频还可设置封面，封面同样可以选择本地图片。宽高留空或 `0` 时自动调整；按页面宽度约束显示，图片／视频高度上限 300 个界面像素，视频保留至少 70 × 40 的区域及独立播放控制条。
- **插入到文档** 或 **Ctrl+Enter** 确认；在光标处插入完整媒体块，**Ctrl+Z** 一次撤销，**F5** 预览，**Ctrl+S** 保存。光标仍在 YAML 元数据中时插入到元数据之后，保留教程标识；取消不会修改文档。
- 小窗口的工具栏自动换行，长表单可用滚轮或 Tab 滚动。导入图片上限 8 MiB、视频上限 256 MiB，原文件保留。分享教程时需一并分发 Markdown 及引用的媒体文件。

媒体插入和普通图片不需要外置 Mod。视频解码仍由可选的 **Media API 1.0.2** 提供；未安装时仍能插入和保存视频块，预览显示封面及缺失说明。

### 手写语法

```markdown
<Image src="mypack:textures/guide/wiring.png" height={100} />
<Image src="local:wiring.png" />
<Image src="https://example.org/wiring.png" />
<Video src="local:demo.mp4" poster="mypack:textures/guide/poster.png" title="搭建过程" />
<QuestLink id="1234abcd" title="打开对应任务" />
[查看任务](quest:1234abcd)
```

- 本地媒体放在 `scripts/ponder/media/`；资源使用完整资源 ID。路径禁止越界和符号链接。
- 普通图片由本模组加载；HTTP 图片异步读取并限制文件大小、像素数和缓存。外部网页链接使用 Minecraft 的原生确认界面。
- **Media API 1.0.2 为可选依赖**：安装后支持视频播放／暂停、重播、进度跳转，以及其支持的本地、资源、HTTP、Bilibili 来源。视频解码依赖 Media API 的平台运行环境；未安装时显示说明和封面。离开页面释放播放器，滚出视口暂停。
- **FTB Quests 为可选依赖**：安装后使用其原生任务链接处理器跳到指定任务，并可通过 `media:` 引用任务媒体资源、复用其 GIF/APNG 图片后端。引用任务 ID 需要与当前任务文件一致；缺失时显示说明。
- 这些适配不打包外部 Mod，不要求安装 FTB Quests、Media API 或现代 GuideME。

## 结构投影与材料目标

动画和静态页面右下角有 **投影** 与 **材料** 图标。1.6.0 起，点击投影会直接回到世界：预览跟随准星，能继续走动和转动视角，无需先填写坐标。静态页面优先使用当前可见的 3D 场景。

| 世界内操作 | 效果 |
| --- | --- |
| 移动鼠标／玩家 | 未固定的预览跟随准星，模板底面中心对准目标表面 |
| 左键 | 固定投影并恢复普通建造操作 |
| 右键 | 旋转 90° |
| Shift + 右键 | 镜像 |
| 滚轮 | 每格调高／调低一层 |
| Shift + 滚轮 | 沿玩家朝向前后挪动一格，支持命中地面时微调 |
| Esc | 取消本次定位；移动已有投影时恢复原位置、旋转及镜像 |
| 固定后蹲下 + 中键点击投影 | 重新抓取，在世界里移动 |

屏幕底部显示当前模式和操作提示；固定后点击恢复普通挖掘、使用物品和建造。`/ponderprojection` 或 `/ponderprojection move` 也可重新定位。

投影使用连续透明模型、亮色边框和黄色原点，被墙或地形遮挡时仍可看见轮廓。实际不存在的底板不会删除第一层机器；非空模板也不会因关闭底板变成空投影。准星可命中 16 格内的地面／方块，无命中时在前方 6 格预览，初始位置保持可见。

Shift 点击教程的投影图标，或 `/ponderprojection settings`，打开透明的工具面板，世界保持可见。镜像、底板、材料钉住直接可选，「更多」内可选显示层、隐藏已完成方块、隐藏投影和 HUD 位置。无需额外安装投影类 Mod，也不新增全局快捷键；教程仍只用 W。

投影不会放置方块、发送建造请求或消耗材料。退出服务器或切换维度后清除。

材料目标默认检查主背包、快捷栏和副手，每 10 tick 刷新。投影建好的部分从剩余需求中扣除。支持原生物品变种、AE2S 线缆和面板部件；容器内物品与机器 NBT 不计为建造材料。不认识的方块会列出未识别项，不能当作材料齐全。一个结构上限为 8192 格。

- 点击材料按钮可单独钉住清单，HUD 和背包均可查看，不必开启投影。
- **背包中拖动标题栏**移动面板；Shift 点击管理，滚轮或底部分页切换清单。
- 在管理页选择「调整 HUD 位置」，或输入 `/ponderprojection layout`，拖动游戏画面中的面板；Esc 结束。
- HUD、背包位置分别保存到客户端配置。调整窗口和 GUI 缩放后自动约束到屏幕内。
- 自动避开容器槽位、原版配方书、可见按钮、原版通知和常见 HUD 区域；物品提示框显示在面板上方。窄窗口折叠为摘要，悬停查看材料、点击打开完整清单。其他 Mod 自定义覆盖层可通过拖拽手动避开。
- 「重置面板位置」或 `/ponderprojection reset` 恢复自动布局。另有 `settings`、`move`、`clear`、`unpin`、`here`、`rotate`、`layer all|Y`、`offset X Y Z`。

附属可通过 `PonderMaterialResolvers.register(ResourceLocation, Resolver)` 扩展材料解析。Resolver 返回 `null` 交给后续解析器，空列表表示该格不需要独立物品，返回的物品数量计入需求。使用 `ProjectionPlan.capture(scene)` 获取独立结构数据；世界渲染与 HUD 管理 API 位于客户端包，应按侧隔离。

## 游戏内 Markdown 编辑

在图文页点击「Markdown 编辑」或按 F7。具备多行输入、鼠标选区、复制粘贴、撤销重做、步骤与动作插入、保存校验及预览。宽窗口显示原生场景和时间轴预览；窄窗口使用完整源码区域，按预览查看图文结果。

- Ctrl+S：校验、原子保存到本地 guides 目录并重新加载。
- Ctrl+Z / Ctrl+Y：撤销／重做；F5：预览当前草稿。
- 预览区点击播放／暂停，点击进度条定位。
- 返回时在本次游戏进程中保留草稿；未保存内容不跨游戏重启。
- 错误文件不会覆盖有效运行内容，重新加载保留上次有效版本。删除本地覆盖文件后重新加载可恢复资源包原文。

在动画页按 F7：Markdown 教程进入源码编辑器，原生 JSON 场景继续使用现有可视化动画编辑器。F8 世界框选、NBT 捕获和原有时间轴编辑功能可继续配合使用。

## Java API 与资源分发

```java
@Override
public void registerTutorials(TutorialRegistrationHelper helper) {
    helper.addMarkdown(new ResourceLocation("myaddon", "machine"));
    // 或注册清单：assets/myaddon/ponder/guides/index.json
    helper.addMarkdownIndex(new ResourceLocation("myaddon", "index"));
}
```

清单格式为 `{"pages":["machine","automation/line"]}`。页面路径为：

- `assets/myaddon/ponder/guides/zh_cn/machine.md`
- `assets/myaddon/ponder/guides/en_us/machine.md`
- 或无语言目录的 `assets/myaddon/ponder/guides/machine.md`

按当前游戏语言、英文、无语言目录回退。资源包可覆盖页面；本地 `scripts/ponder/guides/` 覆盖同 ID 页面。`/ponder reload` 或资源重新加载后更新。资源包新增页面应加入已注册的 index 清单。

API 模型为 `Tutorial`，注册快照见 `PonderIndex.getTutorials()`。编译器 `MarkdownTutorialCompiler.compile(...)` 返回同一份静态文档和已经过指令校验的动画定义。注册和编译保持服务端安全，渲染和编辑在 client 包中隔离。

Markdown 页面按客户端资源加载；当前服务端脚本同步协议仍传输现有 JSON／ZenScript 场景，不下发 Markdown 原文。多人整合包应将 Markdown 和结构一起分发到客户端。

## 世界内定位交互参考

参考 [Litematica 官方基本操作](https://github.com/maruohon/litematica/wiki/Basic-Operations) 和 [FAQ](https://github.com/maruohon/litematica/wiki/Frequently-Asked-Questions) 中的准星定位、世界内选择及滚轮微调思路。实现使用本项目的 1.12.2 原生虚拟世界与输入事件，不嵌入 Litematica 或要求其作为依赖。

## 来源

解析器取自 [AppliedEnergistics/GuideME](https://github.com/AppliedEnergistics/GuideME) 的独立 MIT Markdown 模块，固定于 `cb12bc543ff051e1dbc3828bfeed521a5e3cdd59`。版权、许可证与变更记录见 [third_party/guideme-markdown](../third_party/guideme-markdown)。1.12.2 渲染器、双视图编译和编辑器由本项目维护。
