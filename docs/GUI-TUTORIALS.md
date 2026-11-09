# 机器 GUI 教程与动画片段

GUI 可以作为思索时间轴中的一种动画：先展示机器，播放到指定时刻时，在场景旁边展开机器界面，按步骤高亮槽位、按钮或区域，显示演示物品和操作说明；片段结束时收起界面，继续原来的 3D 演示。

默认在右侧预留空白区域，并把 3D 场景安排到余下区域。支持左侧、指定位置和多个并行片段。它们使用原思索页面的播放、暂停、重播与时间条；跳转到片段中间会还原该帧的 GUI 状态。演示不会打开实际机器容器或向服务器发送机器操作。

## 在动画编辑器中插入

1. 在思索动画里打开现有动画编辑器。
2. 点击“添加”，搜索并选择“GUI 动画片段”。
3. 从 GUI 教程库选择界面，再设置持续时间、`side`（`left` 或 `right`）、缩放和起止帧。
4. 在原时间轴预览、拖动进度检查，保存场景包。

编辑器会自动在片段后插入对应的等待，并放在场景完成指令之前，方便后续动画顺序播放；整段插入可以一次撤销。需要与 3D 动作同时播放时，可以调整或移走这条等待。

片段的 `duration` 按 Minecraft tick 计时，20 tick 为一秒。`from` 和 `to` 是 GUI 教程本身的起止 tick，含两端；相同数值表示固定一帧供讲解。结束帧大于教程长度时自动取最后一帧，因此编辑器默认的 `720000` 表示播放到末尾。片段本身和其他非阻塞动画一样不会移动编排游标，后面用 `scene.idle` 安排持续时间。

GUI 教程库命令为 `/pondergui`，支持搜索、预览和 Shift+点击编辑。熔炉示例需要手动打开或保存，默认不注册示例思索场景。

## JSON 场景包

```json
[
  {"op":"scene.idle","ticks":40},
  {
    "op":"overlay.show_gui",
    "tutorial":"mypack:machine_gui",
    "duration":200,
    "side":"right",
    "from":0,
    "to":200,
    "pointX":2.5,"pointY":1.5,"pointZ":2.5
  },
  {"op":"scene.idle","ticks":200}
]
```

`tutorial` 和 `duration` 必填，其他字段可省略。缺省播放整套 GUI 教程，并将其时长适配到片段长度。

| 可选字段 | 含义 |
| --- | --- |
| `side` | `right`（默认）、`left` 或 `auto`（当前使用右侧） |
| `scale` | 最大显示缩放，默认 1；小窗口自动缩小以适应区域 |
| `from`、`to` | 同时指定；GUI 教程起止帧，可设成相同数值 |
| `x`、`y` | 同时指定；场景区域内的相对位置，0–1，覆盖侧边自动布局；作者负责预留空白 |
| `pointX`、`pointY`、`pointZ` | 同时指定；从机器的 3D 位置连线到 GUI 片段 |
| `definition` | 内嵌的完整 GUI JSON 字符串，ID 必须与 `tutorial` 一致；便于场景随服务器数据一起传递 |

只显示一帧也可以：`duration: 1`。如需把某帧停留五秒，使用 `duration: 100` 并将 `from`、`to` 设为同一帧。缺失 GUI 教程时会在片段位置显示错误说明，时间轴继续运行；安装资源后重新加载即可恢复。

完整示例见 [furnace.ponder.json](../examples/gui/furnace.ponder.json)。示例不会自动安装。

## Markdown：一份教程的两种展示

在带有 `structure`、`item_ids` 且 `presentation` 为 `both` 或 `animation` 的教程中使用：

```md
## 认识机器面板

上方放入原料，下方放入燃料，右侧取出产物。

<GuiTutorial id="mypack:machine_gui" duration={200} side="right" />

## 继续连接管道

将输入和输出接到不同路线。
```

动画在这一环节展开 GUI，静态图文页显示同一 GUI 教程的演示卡片。`GuideOnly` 和 `AnimationOnly` 可以让它只在某一种展示中出现。MDX 支持与 JSON 片段一致的布局、缩放、起止帧和机器连线属性。

也可以使用 `ponder-gui` 围栏，写入完整的 GUI JSON。Markdown 编辑器的“插入 GUI 教程”会从教程库选择并写入这一格式；动画直接携带这份定义，静态页使用同一份定义。围栏会播放全部 GUI 步骤。需要截取或固定帧时，使用上述引用组件或 `ponder` 指令中的 `overlay.show_gui`。

共享教程示例见 [furnace-animation.md](../examples/gui/furnace-animation.md)。

## GUI 定义与采集

本地定义保存在 `scripts/ponder/gui/<namespace>/<path>.gui.json`。可以在真实机器页按 Ctrl+F7 打开采集编辑器：左键选槽位或原生按钮，右键拖出矩形区域，再设置步骤标题、说明、完成条件和演示物品。标题栏支持拖动，采集期间的点击不会操作真实机器。

完整字段见 [熔炉 GUI 定义](../examples/gui/furnace.gui.json)。主要结构为：

- `format: 1`、`id`、`title`、`components`（教程对应的物品 ID）。
- `screen`：真实 GUI 的类名字符串，`adapter` 默认 `generic`。
- `width`、`height`、`texture`：演示界面的尺寸与纹理；纹理按 1.12 原生 256×256 纹理坐标绘制。省略纹理时使用通用面板。
- `targets`：`slot`、`button` 或 `region`，记录原生槽位／按钮 ID 和相对界面的矩形。
- `steps`：标题、说明、高亮目标、持续时间和 `demo` 动作。
- `demo` 支持 `set_slot`、`set_value`、`click`，用 `at` 指定步骤内发生时刻。步骤之间保留前面动作产生的状态。

标题、说明和目标标签以 `@语言键` 开头时使用当前游戏语言。演示只绘制声明的数据与纹理；第三方机器的自定义控件、动态进度和专用渲染需要作者提供对应的演示数据或适配器，不会直接执行机器原生 GUI。

## W 实机引导

安装了与当前机器匹配的 GUI 定义后，在实际机器界面按住现有思索键（默认 W），会在同一个机器页显示操作指引。普通操作仍由原机器处理；指引只观察槽位、同步值和实际点击。输入文字时不会触发 W 引导。

完成条件包括手动下一步、点击目标、槽位包含物品、槽位为空、同步值相等或达到阈值。原生熔炉同步值可以用 `inventory_field.0`（剩余燃烧时间）和 `inventory_field.2`（冶炼进度）。指引面板可拖动；Esc 先退出指引并保持机器页面打开。面板中的独立演示和采集也保留原容器，退出后继续实际操作。

命令：`/pondergui reload`、`/pondergui demo <id>`、`/pondergui edit <id>`。独立演示可以暂停、重播、切步和拖动进度。

## Java / 可选接入

通过插件的 `registerGuiTutorials(Consumer<GuiTutorial>)` 注册纯数据定义，或在资源包中列出：

```text
assets/<namespace>/ponder/gui/index.json
assets/<namespace>/ponder/gui/<lang>/<path>.json
```

索引格式为 `{"tutorials":["namespace:path"]}`；语言查找顺序为当前语言 → `en_us` → 无语言目录。也可仅打包无索引的定义，由场景按 ID 引用。

Java 场景 API：

```java
scene.overlay().showGui(200, new ResourceLocation("mypack:machine_gui"))
    .onRight().frames(0, 200).pointAt(new Vec3d(2.5, 1.5, 2.5));
scene.idle(200);
scene.overlay().showGui(80, new ResourceLocation("mypack:machine_gui"))
    .onLeft().holdFrame(100);
scene.idle(80);
```

`showGui` 也接受完整 `GuiTutorial` 对象。GUI 片段数据和场景 API 可用于专服；客户端渲染器及 `GuiTutorialAdapter` 应隔离在客户端兼容包。自定义实机适配器通过 `GuiTutorials.registerAdapter` 注册，负责提供目标位置、只读槽位／同步值及文本框焦点。AE2S 等自有控件不在原生按钮列表时，可用矩形区域或专用适配器。

可选 CraftTweaker 安装后还可使用 `scene.overlay().showGui(200, "mypack:machine_gui")`，或带 `side`、`from`、`to` 的重载。其他 Mod 均保持可选；此功能无需 CraftTweaker。


## 完整界面布局 API（1.7.1）

复杂界面应通过客户端适配器的 `demoLayout(session)` 提供 `GuiDemoLayout`。
Ponder 负责分组坐标、稳定图层顺序、裁切、缩放和高亮；布局节点同时声明绘制区域和目标标识，
不再把“教程提到的目标”当作“整个界面”。标题、工具栏、升级区、列表以及未被讲解的槽位也应建成可见节点。

```java
@Override
public GuiDemoLayout demoLayout(GuiDemoState session) {
    var builder = GuiDemoLayout.builder(240, 200);
    var machine = builder.root().group(24, 8);
    machine.node("", new Rect(0, 0, 176, 166), GuiDemoLayout.BACKGROUND, this::drawBackground);
    var input = new Rect(8, 18, 16, 16);
    machine.node("input", input, GuiDemoLayout.SLOT, () -> drawSampleSlot(input, session));
    var list = machine.group(8, 40).clip(new Rect(0, 0, 160, 64));
    list.node("list.row.0", new Rect(0, 0, 160, 18), GuiDemoLayout.CONTENT, this::drawSampleRow);
    return builder.build();
}
```

绘制回调的坐标属于所在分组；Ponder 统一应用分组原点。同一节点的 `bounds` 决定指引框，
嵌套裁切也会裁切高亮。`BACKGROUND`、`PANEL`、`SLOT`、`CONTENT`、`CONTROL`、`TEXT`、`MODAL`
按层级绘制，同一层保持声明顺序。弹窗必须放在 `MODAL` 层，避免底层标题压在弹窗上。

每一帧只加入当前可见节点。暂时隐藏的控件使用 `hideTarget(key)` 清除预先声明的备用区域，
防止引导框浮在关闭的弹窗或未选择的页面上。文本节点的矩形应使用实际文本宽度、对齐、换行和缩放计算。

回调可以使用不绑定容器的原生按钮、文本框或数值控件，保留原版皮肤与复合控件行为；
不能创建真实机器 GUI、发送操作数据包，或写入真实背包、机器、配置文件。
此 API 同时用于独立演示、嵌入实机页的演示和场景时间轴中的 GUI 动画片段。
返回 `null` 的适配器继续使用原有 JSON 目标与纹理绘制。
