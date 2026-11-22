# Cleanroom 开发与编辑器

## 开发基线

- Minecraft 1.12.2，运行基线及最低加载器声明为 **Cleanroom `0.4.0` 正式版（无 `-alpha`）**。
- 编译与编辑器开发环境使用 Cleanroom `0.7.14-alpha` userdev；该工具链版本不代表最低运行要求。
- Java 25（当前工作区提供 Temurin `25.0.5+8`）。
- CleanroomGradle `0.22.0`，Gradle Wrapper `9.8.1`。
- CraftTweaker 为可选外部依赖，仅 ZenScript 前端需要；验证版本为 `4.1.20.698`。默认开发客户端不安装，可用 `-PwithCraftTweaker` 启用。
- Cleanroom 内置 MixinBooter 兼容 API，当前开发运行显示版本 `12.1`。
- 来源、基础提交及 MIT 许可证见 [`../NOTICE.md`](../NOTICE.md)。

```bash
export JAVA_HOME=/home/yudream/code/mods/.dev/jdk-25.0.5+8
./gradlew build                 # 单元测试、MCP 开发 jar、SRG 安装 jar、API 和源码 jar
./gradlew runClient             # MCP 开发客户端，无需 CraftTweaker
./gradlew runClient -PwithCraftTweaker # 同时验证可选 ZenScript 前端
./gradlew runServer             # 本地独立服务器；首次运行需自行接受 EULA
./gradlew runClient -PponderSmoke  # 自动客户端验证，完成后退出
./gradlew runClient -PponderSmoke -PpackagedSmoke # 用安装 jar 替换源码输出进行验证
```

Windows 使用 `gradlew.bat`，并把 `JAVA_HOME` 指向 Java 25。
本工作区也可直接运行 `./tools/dev.sh runClient` 或 `./tools/dev.sh build`。
该脚本优先使用 `PONDER_JAVA_HOME`，否则使用已准备的本地 Java 25，并将简单的 HTTP(S) 代理环境变量转为 Java 参数。
`runClient` 的游戏目录是 `run/client/`，`runServer` 使用 `run/server/`。
开发客户端通过 `fml.coreMods.load` 加载 Ponder 的 mixin 引导；发布 jar 通过清单声明加载。
发布构建通过 Mixin annotation processor 生成 MCP → SRG refmap，并将其包含在安装 jar 中。
使用多个物品关联同一场景时，可共享同一个 storyboard 实例和结构；不同物品的这些关联不会被诊断为重复场景。
当前实际安装包、客户端及专服结果见[安装包验收](../LEGACY-VERIFICATION.md)。
历史 MMCE 产物在 CR 0.4.0-alpha 与 0.4.0 上分别验收，见[历史验证记录](CLEANROOM-VERIFICATION.md)；旧结果按原包哈希保留。
首次创建客户端配置时关闭 Forge 异步启动画面，以避免此 Cleanroom 版本在当前环境出现的并发类加载异常。
若已有 `config/splash.properties`，工程不覆盖它；遇到相同异常时可以手动设置 `enabled=false`。

如网络必须使用 HTTP 代理，Gradle 的 Java 网络连接需要独立配置。例如本机使用：

```bash
./gradlew runClient -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 \
  -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897
```

不要将本机代理配置提交到工程。Java 25、原版游戏、资源下载缓存均位于工程外，未打入发布 jar。

## 安装文件

`build/libs/` 中：

| 文件后缀 | 用途 |
| --- | --- |
| `.jar`（无分类器） | SRG 命名的安装文件，放入 Cleanroom 的 `mods/` |
| `-mcp.jar` | MCP 命名的完整开发依赖，供 Cleanroom addon 编译 |
| `-api.jar` | API 包的分类器；其类型仍可能引用完整开发 jar 中的实现 |
| `-sources.jar` | 源码 |

Java addon 推荐编译依赖完整 `-mcp.jar`，运行时使用安装 jar。
发布文件没有嵌入 Cleanroom、CraftTweaker 或开发测试模组。
Mod ID 为 `ponder-legacy`，内容 namespace 为 `ponder`。

## 世界选区到场景

1. 在创造模式搭建结构，按 F8 开启工具。
2. 左键方块设角 A，右键方块设角 B，角点包含在选区内。工具开启时相应点击被拦截。
3. 按 F7，点击「保存结构」，输入 `mypack:machines/example`。
4. 客户端向服务端请求结构，收到后写入 `scripts/ponder/structures/mypack/machines/example.nbt`，并关联发起请求的场景。
5. 选区最小角成为场景 `(0,0,0)`；预览中的坐标都是归一化后的结构坐标。

方块 ID、所有状态属性和 TileEntity NBT 被保留，包括箱子内容。
自 1.7.3 起，同时捕获选区内可序列化的非玩家实体，包括植魔的普通火花、升级火花、多媒体火花及掉落物。
实体位置转换为场景局部坐标，原实体不受影响；框选上角须包含火花所在的空间层。
旧版捕获的 NBT 若不含实体数据，须重新捕获。实体也可通过 Java `createEntity`、JSON `world.create_entity`
或 ZenScript `scene.world.createEntity` 创建，见 [示例](JSON-PACKS.md#entities)。
最大 32768 方块、每轴 256 格、压缩后 900 KB；选区必须已加载且在世界高度内。
服务端校验创造或 OP 权限，玩家须距两个角点均不超过 128 格，请求间隔至少 1 秒。
单机使用内置服务器走相同协议。远程服务器须安装本模组；保存结果位于客户端。

## 指引目录

默认不生成八个 ZenScript 演示或编辑器动画示例。升级时，在 CraftTweaker 扫描前逐文件比对随包原文，
完全未修改的旧示例移动到 `config/ponder/removed-examples/`，原路径不再参与加载。
修改过的示例、自定义文件和符号链接保持原样，已有备份不覆盖；删除旧生成标记也不会重新安装示例。
示例源继续保存在仓库和 jar 的资源中，供作者按需参考。

`/ponder index` 支持按物品所属模组或已注册的 Ponder 主题筛选，并搜索物品名称、模组名称和注册 ID。
目录合并组件注册标签与各场景的标签；`highlightAllTags()` 的内部标记不作为主题。
只有 `addToIndex()` / JSON `indexed: true` 的主题出现在分类栏和主题目录。
未关联可见主题的物品归入「未分类」，被 index exclusion 排除或没有可显示物品图标的组件不进入目录。
多个场景共用的物品仍只有一个入口，点击后可在播放器选择具体情景。

滚轮只滚动指针所在的列表，滚动条可拖动；网格支持上下方向键、Home / End、PgUp / PgDn。
Ctrl+F 聚焦搜索，Enter / Esc 结束输入，搜索中退格不会返回上一页面。
窗口改变和返回目录时保留查询、分类与有效的滚动位置。主题目录与主题详情也支持滚动，底部导航保持固定。

## 场景与动画编辑

### 同一物品的多个播放场景

JSON 中为多个场景设置相同的 `component` 和不同的场景 `id` 即可；示例见
[`../examples/json/multi-scene.ponder.json`](../examples/json/multi-scene.ponder.json)。
Java 可对同一 component 多次调用 `addStoryBoard`，每个 storyboard 用 `identifiedBy` 指定不同场景 ID。
场景标题及顺序沿用注册与排序结果，支持混合 Java、ZenScript 和 JSON 来源。

- 悬停右上角的叠页图标 / 场景计数打开悬浮列表，点击计数可固定展开。
- 列表显示序号、标题和 ID，当前场景有颜色标记。点击某一行，从头播放该场景。
- 滚轮浏览长列表；方向键、Home / End、Page Up / Down 选择，回车打开。
- Tab 开关菜单，Esc 优先关闭菜单，点击外部关闭；菜单操作不旋转或缩放底层预览。
- 单场景隐藏菜单。上一个 / 下一个按钮在首尾禁用；快捷键仍使用 Left / Right。
- 物品提示框的按住进度横条按实际提示框文字区宽度绘制，并适应自动换行。

### 编辑入口

编辑器主工具栏保留打开、保存、撤销、世界框选、保存结构、快捷动画。
「更多」中可新建、选择 pack 内的其他场景、编辑场景属性、重做和关闭。
在原 Ponder 播放界面按 F7 可以打开当前场景的本地 JSON 源；没有本地 JSON 的 Java / ZenScript 场景会提示修改其源文件。
快捷键可在 Minecraft 控制设置中修改。

### 预览和动作

- 「选择区域」：左键角 A、右键角 B，按原始结构坐标保存局部区域。
- 右键或中键拖动旋转视角，滚轮缩放。
- 「快捷动画」：输入 section 句柄、XYZ 位移（格）、XYZ 旋转（度）、时长（tick）和是否等待。
- 新句柄会创建独立 section，旋转中心设在局部区域中心。
- 复用已在前序步骤中定义的 section 句柄，可添加下一段动画；原始选区仅在创建时使用。
- 位移和旋转同时开始。「等待动画结束」插入 `scene.idle`。
- 全部操作在一次撤销中恢复；也可分别编辑生成的每个动作。

左侧列表显示动作开始的 tick，支持拖动排序和参数编辑。
底部时间轴支持点击、拖动回放位置，选中动作显示持续区间。

| 键 | 操作 |
| --- | --- |
| Ctrl+S | 校验、保存、重载 JSON 与结构缓存 |
| Ctrl+Z / Ctrl+Y | 撤销 / 重做 |
| Ctrl+D | 复制动作 |
| Space | 播放 / 暂停 |
| Enter | 编辑选中动作 |
| Insert / Delete | 添加 / 删除动作 |
| Left / Right | 逐 tick 查看 |
| Shift+Up / Down | 移动动作顺序 |
| 表单中 Tab | 切换输入框 |
| 表单中 Ctrl+Enter | 应用参数 |

`scene.idle` 推进故事板游标，其他动作的持续时间可以相互重叠。
创建一个时长 40 tick 的移动后，通常还需要 `scene.idle:40` 才能在下一段故事前等待动画完成。
自定义 codec 可以在 Java 的 `program` 中调用 `idle`；通用编辑器无法静态推导这种自定义动作的内部时长。

当前可以编辑任意已有 pack 内的场景；新建操作创建一个新的单场景 pack。
删除或复制有句柄依赖的步骤后可能暂时产生无效草稿；修正句柄或撤销后即可继续保存。

### 保存行为

JSON 保存在 `scripts/ponder/packs/<namespace>/<pack path>.ponder.json`。
每次保存先通过原 JSON / 指令验证器，再原子替换文件，并保留上一版 `.bak`。
外部文件在编辑期间有修改时，保存会提示冲突；重新打开磁盘版本后再编辑。
保存后的场景进入本机 Ponder 注册表；在远程服务器环境需要将 pack 和结构文件交给服务器或整合包维护者。

编辑器动画示例保留在 `assets/ponder/editor/examples/animation.ponder.json`，默认不复制到游戏目录。
完整的格式、选择器、NBT 和重载规则见 [`JSON-PACKS.md`](JSON-PACKS.md)。

## Java 扩展

### 注册普通场景

```java
public final class MyScenes implements PonderPlugin {
    @Override public String getModId() { return "myaddon"; }
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> scenes) {
        scenes.addStoryBoard(new ResourceLocation("minecraft:book"),
            new ResourceLocation("myaddon:demo"), (scene, util) -> {
                scene.title("demo", "My animation");
                scene.world().showSection(util.select().everywhere(), EnumFacing.DOWN);
                scene.idle(20);
                ElementLink<WorldSectionElement> part = scene.world()
                    .makeSectionIndependent(util.select().fromTo(1, 1, 1, 2, 2, 2));
                scene.world().moveSection(part, new Vec3d(0, 1, 0), 20);
                scene.idle(20);
                scene.markAsFinished();
            }).identifiedBy(new ResourceLocation("myaddon:demo"));
    }
}
```

在 addon 的初始化阶段调用 `PonderIndex.addPlugin(new MyScenes())`，或者提供
`META-INF/services/net.createmod.ponder.api.registration.PonderPlugin`。
也可使用 Forge IMC 消息 `register_ponder_plugin`，值为具有公共无参构造器的插件全限定类名。
结构可放在 `assets/myaddon/ponder/demo.nbt`；也支持外部结构和 `PonderStructureProvider`。
现成完整例子位于 [`../examples/addon/`](../examples/addon/)。

### JSON 自定义动作与编辑器模板

在 addon preInit 注册 `ScriptInstructionCodec`：

```java
ScriptInstructionCodecs.register(new MyInstructionCodec());
JsonObject template = new JsonObject();
template.addProperty("op", "scene.custom");
template.addProperty("codec", "myaddon:pulse");
template.addProperty("payload", "{duration:20,x:1,y:1,z:1,color:\"blue\"}");
EditorActions.register("myaddon:pulse", "Pulse", template);
```

codec 实现 `getId()`、`validate(NBTTagCompound)` 和
`program(NBTTagCompound, SceneBuilder, SceneBuildingUtil)`。
其 payload 使用 SNBT 字符串；完整例子见 `ExamplePulseCodec.java`。
服务器同步自定义指令需要客户端安装对应 codec，并匹配协议版本与能力。

### 直接编辑 JSON 文档

```java
SceneDocument doc = ScenePackIO.read(path);
doc.editScene(scene -> scene.addProperty("title", "Updated title"));
EditorAnimations.insert(doc, 2, "moving_part",
    new CuboidSelection(new BlockPos(1, 1, 1), new BlockPos(2, 2, 2)),
    new Vec3d(0, 1, 0), new Vec3d(0, 90, 0), 20, true);
ScenePackIO.save(path, doc);
PonderJsonLoader.reload();
```

`ScenePackIO.parse/validate` 只解析和检查，不替换 live 场景。
`SceneDocument.snapshot` 返回深拷贝，`edit`、`insertStep`、`replaceStep`、`moveStep`、`undo/redo` 提供有历史的编辑。
`EditorAnimations.insert` 会先验证完整候选文档再应用。
这些 API 是此分支的扩展，不承诺与现代官方 Ponder 二进制兼容。

## 验证记录

当前包的单元测试和客户端验证结果见 [安装包验收](../LEGACY-VERIFICATION.md)，历史记录见 [`CLEANROOM-VERIFICATION.md`](CLEANROOM-VERIFICATION.md)。

自动客户端会创建或复用专用世界 `ponder-editor-smoke`，放置方块和箱子，
请求服务端捕获选区，执行动画表单、预览、撤销重做、保存重载、语言切换和截图。
报告为 `run/client/ponder-smoke/report.json`，截图在同目录 `screenshots/`。
开发 harness 独立编译到 `build/dev/`，只有带 `-PponderSmoke` 时安装，普通 `runClient` 会移除它。

验证真实安装环境（Linux x64，使用 **`0.4.0` 正式版**）：

```bash
./tools/dev.sh build reobfDevHarnessJar reobfServerHarnessJar
python3 tools/verify-cleanroom.py client
python3 tools/verify-cleanroom.py server
python3 tools/verify-cleanroom.py server --empty  # 对照：只装加载器
```

脚本使用官方安装器中的精确依赖与校验值，以及重混淆后的安装 jar 和独立测试 jar，
没有 MCP / userdev 重映射层。编辑器报告与截图位于 `run/cleanroom-<版本>-legacy/client/ponder-smoke/`，
MMCE fixture 位于 `run/cleanroom-<版本>-mmce/<模式>/<端>/`。
专用服务器结果和加载器自身启动问题需按验证记录判读，不能只以进程退出码零判定成功。

此编辑器测试没有覆盖大规模整合包、多名远程玩家、全部第三方 TileEntity 或人工输入的所有交互。
MMCE 软适配由单独的生产包 fixture 验证，见 [MMCE 接入手册](MMCE.md)。
Revo UI 的实际物品提示框与像素检查见 [Revo UI 可选适配](REVO-UI.md)。

## 共享 Markdown 教程

1.5.0 新增静态图文展示与 Markdown 动画编排。使用 `PonderPlugin.registerTutorials(TutorialRegistrationHelper)` 注册；格式、原生组件、资源本地化和编辑器操作见 [Markdown 手册](MARKDOWN.md)。玩家入口只注册 W。
