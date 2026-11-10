# Ponder Legacy — 安装包验收

## 1.7.1 发行验收（完整 GUI 布局与附属名称本地化）

日期：2026-11-10。Minecraft 1.12.2 / Cleanroom **0.4.0 正式版（无 -alpha）** / Java 25。生产包 `ponder-legacy-1.7.1.jar`，SHA-256：`923cb6653eee53c96e4fbd9ac871c897cb151b91b3e0759c40ed4687611c6dd4`。

341 项单元测试通过，覆盖嵌套布局、裁切、弹窗层级、隐藏目标、别名所属模组解析、翻译覆盖顺序和缺省回退。独立 API jar 的 `GuiDemoState` 与布局树编译检查通过。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| client | PASS | 249 |
| gui-client | PASS | 62 |
| server | PASS | 11 |
| catalog-addons | PASS | 157 |
| addons-server | PASS | 11 |

`GuiDemoLayout` 统一处理完整界面节点、分组偏移、图层、裁切与高亮。核心回归覆盖编辑器、Markdown、目录和投影；GUI 专项验证实机熔炉引导、独立演示与采集编辑器；断线回归验证附属重载在客户端主线程执行。AE2S 的逐页面外观及真实控件几何另由 AE2S Ponder 验收。

联合安装实际应用气动 `1.0.9-cleanroom.4`、血魔法附属 `1.0.4-cleanroom.1`、概率样板 `1.0.2` 及其前置，验证简体／繁体／英文名称、中文名／英文原名／命名空间／所属 Mod ID 搜索、分类切换、应用气动重复注册诊断和联合专服启动。该目录组合使用 AE2S `1.1.0+a47dfe029`；AE2S Ponder 最终版本的正式 `1.1.0` 四组合见其独立记录。CraftTweaker 未安装，类加载审计通过。所有通过报告的生产 Ponder jar 哈希均与本次验收一致。

[报告、运行指纹与截图](docs/verification/development-1.7.1-cleanroom-0.4.0/summary.json)。Linux x64 / llvmpipe；未扩大声明到任意整合包或其他 GPU。[发行入口](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.7.1)。

## 历史开发构建：1.7.0（时间轴 GUI 动画与 W 实机引导）

- 日期：2026-11-08；Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25.0.5。
- 生产安装包：`ponder-legacy-1.7.0.jar`；SHA-256：`86e072acb375b28dd68051c87ae2c752a6e9df81794e8da701fb641943f35041`。
- 331 项单元测试通过；本次仅推送 main CI，正式发行状态见 PUBLICATION.md。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| gui-client | PASS | 60 |
| core-client | PASS | 243 |
| core-server | PASS | 11 |

GUI 片段在同一 3D 思索页面的侧边留白显示，与机器动画共用时钟、暂停、跳转与重播。实机引导在真实熔炉页验证 W 长按、原生点击与槽位同步、燃烧／输出等待、独立演示不改变背包和服务器容器；采集编辑器支持框选区域、保存与重载。小窗口和缺失可选教程回退均通过实际 framebuffer 检查。外置 Mod 保持可选，专服不加载 GUI 客户端类。

[报告、运行指纹与实机截图](docs/verification/development-1.7.0-cleanroom-0.4.0/summary.json)。Linux llvmpipe；历史外置组合仅对应各自验收包。本次全新隔离实例通过；一个复用测试存档时熔炉物品移动未完成的超时记录保留在本地运行目录，不作为通过证据。

## 历史：1.6.2（索引模组名称本地化）

- 日期：2026-11-06；Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25.0.5。
- 生产安装包：`ponder-legacy-1.6.2.jar`；SHA-256：`dddf7f51b50dcd4fa7ebb60e61ab0ddb196bcbff173c429e8a3aaf340c00494e`。
- 314 项单元测试通过；独立发布目录清理后完整重建，生产安装包与本地验收包逐字节一致。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| 无外部 Mod 客户端，完整编辑器／教程／投影回归 | PASS | 240 |
| 无外部 Mod 专服 | PASS | 10 |
| AE2S／应用气动／FTBQ／Media／Revo 联合安装，分类本地化专项 | PASS | 81 |
| 上述可选组合专服 | PASS | 10 |

实机核对中文／英文分类、资源包 `ponder.mod_name.<命名空间>` 与模组 `mod.<命名空间>.name` 翻译、缺省元数据／未知命名空间回退、中文名／英文原名／Mod ID 搜索；语言切换保持搜索词与所选分类。回归长目录滚动、类别与物品独立视口、小窗口、点击场景和返回索引。联合组合使用实际 AE2S 与应用气动教程，检查翻译与应用气动无重复 ID 诊断。

联合组合使用 AE2S / Supergiant `1.1.0+a47dfe029`、AE2S Ponder Legacy 0.2.0、应用气动 `1.0.9-cleanroom.2`、PNC `1.12.2-0.11.15-398`、FTB Quests `1202.10.0-modern.10`、FTB Library `5.5.4-modern.1`、ItemFilters `1.0.4.2`、Media API `1.0.2`，以及 Revo UI `0.8.7` / NFR `0.8.0` / ModularUI `3.2.0-nfr.2`。CraftTweaker 未安装，类加载审计通过。

用户日志中的 `appliedpneumatics:air_network` 重复注册来自应用气动插件在三个注册位置分别创建 storyboard；应用气动修复包复用同一个对象，保持 26 个空气网络物品关联、全部 33 个关联和三个完整时间线。此修复位于应用气动本体，须同时替换其 jar。应用气动另用同一 Ponder 1.6.2 包完成有／无 Ponder 的客户端和专服验收（28／34／9／9 项），全部通过；其验收与源代码随本地分发包保留。

[本版报告、运行指纹、界面截图与构建一致性](docs/verification/release-1.6.2-cleanroom-0.4.0/summary.json)。Linux llvmpipe；Revo 使用专项分类流程及已记录的合法 NFR 最小配置，不扩大到其他页面的后处理问题。本版不重复全部历史可选组合；历史验收仅对应各自安装包。用户整合包日志的 Cleanroom 0.6.13-alpha 与本次 0.4.0 正式版验收分别看待。


## 历史：1.6.1（Markdown 媒体插入面板）

- 日期：2026-11-06；Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25.0.5。
- 生产安装包：`ponder-legacy-1.6.1.jar`；SHA-256：`4c3f8913b9df79220615e747dd76962b9bf2007c783ec4c0d97ce41f16ce3ed7`。
- 314 项单元测试，新增媒体属性转义／来源校验、完整插入撤销、元数据保护、本地导入防覆盖及中文文件名回归；验收使用实际生产 SRG 安装包与独立 harness。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| 无外部 Mod 客户端，完整教程／编辑器／投影流程 | PASS | 223 |
| 无外部 Mod 专服 | PASS | 10 |
| AE2S／FTBQ／Media 客户端，完整流程 | PASS | 229 |
| 上述组合并安装 Revo／NFR／ModularUI 的专服 | PASS | 10 |
| 全部可选 Mod 联合安装，Revo 下的 Markdown 媒体面板专项 | PASS | 35 |

实机点击编辑器的图片／视频按钮，进入文件选择器并导入原始文件，核对文件字节与可移植的本地引用。验证空来源／非法尺寸不改变文档、取消保留草稿、一次撤销／重做、标题特殊字符、封面、尺寸、保存后索引重载及 F5 预览。真实解码插入的图片和视频；未安装 Media API 时保留编辑与封面回退。中文／英文和 320 × 240 界面检查工具栏换行、输入框边界、滚动与 Tab 定位。

可选组合使用 AE2S / Supergiant 1.1.0、教程附属 0.2.0、FTB Quests 1202.10.0-modern.10、FTB Library 5.5.4-modern.1、Media API 1.0.2、ItemFilters 1.0.4.2，以及 Revo UI 0.8.7 / NFR 0.8.0 / ModularUI 3.2.0-nfr.2。CraftTweaker 未安装，类加载审计通过。

Revo 的专项调试用驱动回调定位到其背景模糊状态恢复中的 `glPopAttrib / GL_COLOR_ATTACHMENT0` 错误；进入 Ponder 页面时跳过该后处理并清除过渡，最终专项未报告驱动错误。修复前中间包的预期失败日志和真实包哈希一并归档。这一适配只处理 Ponder 页面；其他模组／原版页面中的 Revo 后处理问题不包含在此结论中。

[报告、运行指纹、界面截图和构建一致性](docs/verification/release-1.6.1-cleanroom-0.4.0/summary.json)。本地开发目录与独立发布目录安装包逐字节一致。Linux llvmpipe；未覆盖所有显卡、光影和任意整合包。视频使用独立 Linux VLC，不验收音频，不发布测试片段或解码依赖。历史验收仅对应各自安装包。


## 历史：1.6.0（世界内投影放置与可见性）

- 日期：2026-11-06；Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25.0.5。
- 生产安装包：`ponder-legacy-1.6.0.jar`；SHA-256：`a7863b678e2a43f43722690032c89d65c243356f970671b6aefa7b34073cad10`。
- 307 项单元测试，新增实际底板判定及最低机器层保留回归；所有运行验收使用生产 SRG 安装包和独立 harness。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| core-client | PASS | 194 |
| core-server | PASS | 10 |
| ae2s-ftbq-media-client | PASS | 200 |
| ae2s-ftbq-media-server | PASS | 10 |
| revo-client | PASS | 70 |

世界内检查使用真实草地方块和原生相机：点击投影后没有屏幕阻挡，准星带动预览，左右键、Shift、滚轮输入经过放置处理器，固定后原点保持不动；世界方块未被放置或破坏。蹲下中键重新抓取、Esc 拦截菜单并恢复原位置／旋转／镜像。实际 framebuffer 核对地形仍可见、蓝色投影轮廓存在，另搭建墙体验证遮挡时的轮廓和透明工具面板。

AE2S / Supergiant 1.1.0 + 教程附属 0.2.0 + FTB Quests 1202.10.0-modern.10 + FTB Library 5.5.4-modern.1 + Media API 1.0.2 + ItemFilters 1.0.4.2 组合检查真实机器、线缆与终端投影及材料分解，并回归视频解码、任务跳转；未修改依赖 jar。Revo UI 0.8.7 / NFR 0.8.0 / ModularUI 3.2.0-nfr.2 组合通过原有提示框、静态页及字体重排。CraftTweaker 未安装，类加载审计通过。

[原始报告、运行指纹和实机截图](docs/verification/release-1.6.0-cleanroom-0.4.0/summary.json)。Linux llvmpipe，未覆盖所有硬件、光影或任意整合包。视频使用外置 Linux VLC，不验收音频。投影仅辅助建造，不自动放置方块。历史组合结论仍对应各自安装包。


## 历史：1.5.1（FTB Quests 加载顺序循环修复）

- 日期：2026-11-06；Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25.0.5。
- 生产安装包：`ponder-legacy-1.5.1.jar`；SHA-256：`c452c409358f2facf87b2a5f402492be6930365dc4fb7eb82912835d02469456`。
- 305 项单元测试，包括 5 项 Forge ModSorter 排序与元数据回归。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| ftbq-ae2s-combined-client | PASS | 168 |
| ftbq-ae2s-combined-server | PASS | 10 |
| without-optionals-client | PASS | 166 |
| without-optionals-server | PASS | 10 |

已用原 1.5.0 实际生产包 + FTBQ 1202.10.0-modern.10 + AE2S / Supergiant 1.1.0 + AE2S Ponder Legacy 0.2.0 复现截图对应的 FML 排序循环；旧包日志和运行指纹作为预期失败保留。该 FTBQ 声明 `after:ponder-legacy`，与 1.5.0 的 `after:ftbquests` 互相冲突。1.5.0 的早先媒体组合使用不含这一声明的 FTBQ modern.8，不能覆盖此次组合。

1.5.1 移除 Ponder 反向约束，在相同 modern.10 组合上通过客户端、专服及原生任务链接／媒体／投影回归；另检查未安装可选联动的两端。FTB Quests、Media API 和 CraftTweaker 保持可选，原 AE2S 附属 0.2.0 无需重新打包。FTB Library 5.5.4-modern.1、ItemFilters 1.0.4.2 和 Media API 1.0.2 按原 jar 验证，未修改依赖本体。

[本版报告、运行指纹、失败复现与截图](docs/verification/release-1.5.1-cleanroom-0.4.0/summary.json)。Linux llvmpipe；未覆盖任意整合包或全部硬件。视频测试使用外置 Linux VLC，不验收音频。本版没有重跑所有旧版可选模组组合，历史记录仅对应其各自的包。


## 历史：1.5.0（双展示、Markdown、投影与材料目标）

- 日期：2026-11-06；Minecraft 1.12.2 / **Cleanroom 0.4.0 正式版（无 -alpha）** / Java 25.0.5。
- 生产安装包：`ponder-legacy-1.5.0.jar`；SHA-256：`bf668461d714313326ecf095614b58eee5b6fa5e5317c7ee5cb5de3bd92176d6`。
- 300 项单元测试，生产 SRG 安装包与独立 harness；不使用 MCP / userdev 验收。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| core-client | PASS | 166 |
| core-server | PASS | 10 |
| quests-media-client | PASS | 168 |
| revo-client | PASS | 70 |
| crt-server | PASS | 11 |
| mmce-client | PASS | 34 |
| mmce-server | PASS | 29 |

覆盖 W 默认展示、共享 Markdown 编译、滚动与视图切换、游戏内校验保存／预览、原生结构投影的真实 framebuffer、材料统计／已建扣减、HUD 与背包独立拖拽位置及磁盘重载。FTB Quests 1202.10.0-modern.8 / FTB Library 5.5.4-modern.1 / Media API 1.0.2 组合验证原生任务跳转、本地视频实际解码与跳转；Revo UI 0.8.7 / NFR 0.8.0 / ModularUI 3.2.0-nfr.2 / AE2S 1.1.0 组合验证提示框与静态页字体重排。MMCE 2.3.2 客户端／专服回归通过，其自身仍需 CraftTweaker。

[原始报告、环境指纹与截图](docs/verification/release-1.5.0-cleanroom-0.4.0/summary.json)。无 CrT 组合类加载审计通过；专服检查不加载客户端渲染与可选桥。视频测试使用外置 Linux VLC，未验收音频；素材仅存在测试实例，不打入发布包。Linux llvmpipe，未覆盖所有硬件、任意整合包和任意 Mod 覆盖层。材料只计算主背包／副手，不计算容器内物品和机器 NBT；投影不代建。


## 历史：1.4.1（CraftTweaker 可选前置）

- 验收日期：2026-11-06。
- Minecraft 1.12.2；**Cleanroom 0.4.0 正式版（无 `-alpha`）**；Java 25.0.5。
- 安装包：`ponder-legacy-1.4.1.jar`。
- SHA-256：`fb6dc08c9f9cdd36deb7f610df0e751957fec8d354cd4c904ef793ab1d1839a7`。
- 279 项单元测试通过；实际运行使用生产 SRG jar 与独立 harness，不使用 MCP / userdev。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| 无 CraftTweaker 客户端 | PASS | 126 |
| 无 CraftTweaker 物理专服 | PASS | 10 |
| CraftTweaker 4.1.20.698 客户端 | PASS | 127 |
| CraftTweaker 4.1.20.698 物理专服 | PASS | 11 |
| 无 CraftTweaker，安装 Revo UI / NFR / ModularUI / AE2S 的客户端 | PASS | 68 |
| 无 CraftTweaker，安装 Revo UI / NFR / ModularUI 的物理专服 | PASS | 10 |
| MMCE 2.3.2 + CraftTweaker 客户端 / 物理专服 | PASS | 34 / 29 |

未安装 CrT 时，完整编辑器、框选捕获、NBT 保留、动画、目录与多场景导航通过；Java 场景注册、JSON 自定义 codec、保存重载、场景编译/跳转/重播及同步快照通过。类加载审计确认没有加载 CraftTweaker、ZenScript 或 CrT 诊断桥；专服不加载 Ponder 客户端适配类。涉及 `IData` 的 ZenScript 专用方法由 Forge 可选方法转换器移除，公共类可正常枚举方法。

安装 CrT 时，真实 `.zs` fixture 在启动阶段注册场景，并执行 `modifyTileNBT`、自定义 codec 与播放验证。Gradle 依赖改为 `compileOnly`，发布 POM 不携带 CrT 传递依赖；开发时通过 `-PwithCraftTweaker` 显式启用。

官方 MMCE 2.3.2 **自身**要求 CraftTweaker 4.0.4+，因此该组合不能省去 CrT；Ponder 不修改第三方 jar 或依赖约束。该限制的实际加载器错误与全部独立报告、运行指纹、截图见 [1.4.1 验收证据](docs/verification/release-1.4.1-cleanroom-0.4.0/summary.json)。Revo UI 组合使用 [NFR 合法最小配置](docs/REVO-UI.md)。渲染使用 Linux llvmpipe，未覆盖所有硬件与整合包。

复现核心组合：

```bash
./tools/dev.sh build reobfDevHarnessJar reobfServerHarnessJar
python3 tools/verify-cleanroom.py client --crafttweaker absent
python3 tools/verify-cleanroom.py server --crafttweaker absent
python3 tools/verify-cleanroom.py client --crafttweaker present
python3 tools/verify-cleanroom.py server --crafttweaker present
```

## 历史：1.4.0 正式版（含 Revo UI 可选适配）

- 验收日期：2026-11-04。
- Minecraft 1.12.2；**Cleanroom 0.4.0 正式版（无 `-alpha`）**；Java 25.0.5；CraftTweaker 4.1.20.698。
- 安装包：`ponder-legacy-1.4.0.jar`。
- SHA-256：`4c02d70c6f38ddf5305fa4be47a5e410e693c57f2cd8576ed5f78f8b7f32f14b`。
- 277 项单元测试通过，全部运行验收使用该生产 SRG 安装包与独立开发 harness，不使用 MCP / userdev 重映射。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| Revo UI 0.8.7 + NFR 0.8.0 full + ModularUI 3.2.0-nfr.2 + AE2S / Supergiant 1.1.0 客户端 | PASS | 68 |
| 未安装 Revo UI、NFR、ModularUI 的客户端 | PASS | 63 |
| 编辑器、场景播放、目录与多场景菜单客户端 | PASS | 116 |
| 已安装 Revo UI / NFR / ModularUI 的物理专服 | PASS | 启动、场景注册与执行；不加载 Ponder 的 Revo 客户端适配类 |
| 未安装 Revo UI 的物理专服 | PASS | 启动、场景注册与执行 |
| MMCE 2.3.2 客户端 | PASS | 34 |
| MMCE 2.3.2 物理专服 | PASS | 29 |

Revo UI 像素检查覆盖中文/英文、AE2S 灰色 ME 智能线缆、长标题换行、854 × 480 小窗口、字号 0.75 / 1.5、行距 8 / 10 / 12 / 16、标题图标/稀有度/模型预览、关闭预览、关闭现代提示框及无 Revo 的原版回退。进度条按实际预留行绘制，占满文字区，避开模型，不覆盖标题与提示文字。截图的进度固定为 60%，并核对实际 framebuffer 像素与比例。

独立报告、运行库指纹及原始截图见 [1.4.0 验收证据](docs/verification/release-1.4.0-cleanroom-0.4.0/summary.json)。包未包含 Revo UI、NFR、ModularUI、AE2S、MMCE 本体或开发 harness；不会自动注入测试教程。

CleanMix 自动生成的兼容信息已按键排序，消除注解处理顺序造成的包哈希差异。修复后的生产包已重新完成上表验收；本地开发目录、独立发布目录与 GitHub Actions 产物逐字节一致，见 [构建一致性记录](docs/verification/release-1.4.0-cleanroom-0.4.0/reproducibility.json)。发行仍强制匹配实际验收包的 SHA-256。

NFR 0.8.0 首次生成的默认 TOML 含重复 `[compat]`，会使官方包在初始化时退出；隔离验收实例预置合法最小配置，依赖 jar 保持原样。初次失败记录与实际配置均归档，详见 [Revo UI 适配说明](docs/REVO-UI.md)。渲染使用 Linux llvmpipe 软件 OpenGL，未覆盖全部硬件、其他 Revo 版本、所有整合包与人工输入路径。历史包的 0.4.0-alpha 与 MMCE ABI 模拟验收不自动背书本包。

## 历史：1.4.0-cleanroom-alpha.1（含 MMCE 软适配）

- Minecraft 1.12.2；Java 25.0.5；CraftTweaker 4.1.20.698。
- **Cleanroom 0.4.0-alpha** 与 **0.4.0 正式版（无 -alpha）**分别验收，两个版本不视为相同版本。
- 安装包：`ponder-legacy-1.4.0-cleanroom-alpha.1.jar`。
- SHA-256：`9be55d18dff404d74b886521395c47cc6d2c5ec1ff3d22d73fecb1553f9ee4ab`。
- 277 项单元测试通过，两版分别完成 MMCE 2.3.2 已安装、未安装和模拟 ABI 不兼容的客户端及物理专服验证，各 85 项运行检查通过。
- 包内不含 MMCE 本体、旧 `ponder_mmce` 附属入口或开发 harness；不自动安装示例或生成机器教程。

MMCE 覆盖静态/动态结构、preview NBT、命名组、指纹、蓝图入口、Java / ZenScript / JSON 与编辑器编译及播放。
精确 `0.4.0-alpha` / `0.4.0` 专服的 Kirino 发现调用修复仅在对应物理专服启用；不修改加载器 jar。
完整报告、运行库指纹、截图及限制见 [MMCE 验收记录](docs/CLEANROOM-VERIFICATION.md)。CI 发行必须匹配上述 SHA-256，重新构建不能自动继承运行验收。

## 历史：改名、作者与图标更新后的正式基线验收

本记录对应改名、作者与图标更新后的安装包。验收使用 Minecraft 1.12.2、**Cleanroom 0.4.0 正式版（无 -alpha）**、Java 25.0.5；编译 SDK 为 0.7.14-alpha，不作为运行兼容证据。

- 安装包：`ponder-legacy-1.4.0-cleanroom-alpha.1.jar`。
- SHA-256：`82798859f97bf3837a2fb7e07e23a4cc23457f1076f604343e6b50a33ecebc99`。
- 必需运行库：CraftTweaker 4.1.20.698；校验值见各 runtime.json。
- 验收日期：2026-11-03。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| client | PASS | 116 |
| server | PASS | startup + scene registration |

客户端覆盖正常 FML 安装包发现、无默认示例、结构及 NBT 捕获、动画编辑/预览/保存重载、Java/JSON 场景注册、教程导航、目录分类/搜索/滚动、中文/英文与小窗口渲染。专服覆盖启动及服务端场景插件注册。

正式 0.4.0 专服的 Kirino 模组发现兼容修复仅在该物理端和该版本启用；不修改官方加载器 jar。渲染验收使用 Linux llvmpipe 软件渲染，未覆盖全部硬件/整合包组合。

报告、运行库指纹与截图见 [客户端证据](docs/verification/legacy-identity/cleanroom-0.4.0/client/report.json) 和 [专服证据](docs/verification/legacy-identity/cleanroom-0.4.0/server/report.json)。开发验证模组未包含在发行包中。
