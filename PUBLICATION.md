# Ponder Legacy 发布状态


## 当前开发版：1.7.1

- 提供完整 GUI 的分组布局、绘制层级、裁切、动态可见性与共享高亮坐标；修正分数缩放的边缘取整。
- 336 项单元测试、正式 Cleanroom 0.4.0 核心客户端 243 项、GUI 专项 62 项与专服 11 项检查通过。独立 API jar 编译检查通过。
- 生产 SHA-256：`737dbd1488377a4464e8817f1e2465cb06883b34ca93559ac157802d6589dbf7`；[验收记录](docs/verification/development-1.7.1-cleanroom-0.4.0/summary.json)。
- main CI 构建；本次未创建标签或 GitHub Release。


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
