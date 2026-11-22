# Ponder 界面主题

`1.8.0` 新增主题系统。内置 `ponder:classic`（经典）和
`ponder:modern`（现代 Ponder）。现代主题参考官方 Ponder 的 Minecraft 1.21.1
分支，使用淡紫色渐变边框、横向渐隐标题条、物品图标框、细进度条和悬浮播放控件。
现代播放器的主要控件按官方位置排列：关闭键居中，识别和重播分列两侧。
按钮显示当前绑定的快捷键；旁白使用细金色边框、暖色文字和指向场景的水平引线。
现代背景使用 1.12 原生后处理模糊，隐藏页面背后的热栏与玩家手臂；不支持后处理时回退为纯色背景。

外观参照固定为官方 `mc1.21.1/dev` 的
`89819291b726708d119b118dea01667eae0b64c9`。主要控件为 20×20 逻辑像素，
外框向四周各扩展 4 像素；标题图标为 30×30，物品以 2 倍绘制。
进度条为 220×1，旁白每行间隔 9 像素。指南、投影、材料和主题等扩展入口放在两侧，
GUI 动画会为侧栏留出空间。场景相机继续使用本实现的 1.12 渲染方式。

场景播放器、索引、场景菜单、Markdown 指南和编辑器的界面颜色随主题变化。
主题保留场景作者指定的标注语义颜色、物品/方块模型和机器 GUI 贴图。
`layout` 选择经典或现代两种播放器布局，其他页面沿用各自的操作布局。

## 切换和重载

- 点击播放器、索引或指南右上角的调色板按钮，立即切换并保存。
- `/pondertheme` 打开主题选择界面；上下键选中，回车应用，Esc 返回。
- `/pondertheme ponder:modern` 直接选择主题。
- `/pondertheme auto` 使用整合包默认主题；没有指定时使用经典主题。
- `/pondertheme list` 列出 ID，`/pondertheme reload` 重载资源和本地 JSON 主题定义。
- F3+T 重载资源包时也会重载主题。

玩家选择保存在 `config/ponder-client.cfg` 的 `client.uiTheme`。
默认值为 `auto`。切换主题和返回播放器会保留当前场景、播放时间、暂停状态及视角。
主题只影响本地客户端；服务器不会向玩家强制同步界面外观。

## 本地 JSON：新建、继承和修改

创建 `config/ponder/themes.json`，不需要 CraftTweaker：

```json
{
  "default": "mypack:night",
  "themes": {
    "mypack:night": {
      "parent": "ponder:modern",
      "name": "夜间",
      "description": "以现代 Ponder 为基础的蓝色主题",
      "colors": {
        "accent": "#88BBFF",
        "border": "#7088BBFF",
        "border_bottom": "#3088BBFF",
        "progress_fill": "#88BBFF"
      }
    },
    "ponder:classic": {
      "colors": { "accent": "#99CCBB" }
    },
    "mypack:custom": {
      "name": "自定义主题",
      "layout": "classic",
      "colors": { "text": "#FFEECC" }
    }
  }
}
```

`themes` 中使用现有 ID 会合并覆盖指定字段，`colors` 按颜色角色逐项合并。
使用新 ID 会注册新主题。`parent` 可以继承任意已注册主题；未写 `parent`
的新主题使用经典默认配色。`name`、`description` 可以是文字或语言文件中的翻译键。
主题 ID 必须包含命名空间，例如 `mypack:night`。

`default` 在玩家使用 `auto` 时生效。玩家显式选择的主题优先于整合包默认值。
修改后执行 `/pondertheme reload`。JSON 语法或字段校验失败时会保留上次有效的本地文件，
并在日志中给出错误；缺失父主题、继承循环等无效定义会被跳过，内置主题始终可用。
文件限制为 256 KiB，最多 256 个本地主题。

## ZenScript：同样的主题能力

安装可选的 CraftTweaker 后，把脚本放进 `scripts/`：

```zenscript
import mods.ponder.Theme;

Theme.extend("mypack:night", "ponder:modern")
    .name("夜间")
    .description("基于现代 Ponder")
    .color("accent", "#88BBFF")
    .color("progress_fill", "#88BBFF")
    .register();

Theme.create("mypack:custom")
    .name("自定义主题")
    .layout("classic")
    .color("text", "#FFEECC")
    .register();

Theme.modify("ponder:modern")
    .color("border_hover", "#F0DDEEFF")
    .register();

Theme.modify("mypack:night").color("text", "#EEF4FF").register();
Theme.setDefault("mypack:night");
```

- `Theme.create(id)`：新建，不指定父主题时使用经典默认值。
- `Theme.extend(id, parent)`：继承主题并创建一个新 ID。
- `Theme.modify(id)`：覆盖现有主题中指定的字段；可以多次修改。
- Builder 方法：`parent(id)`、`name(text)`、`description(text)`、
  `layout("classic"|"modern")`、`color(role, value)`，最后调用 `register()`。
- `Theme.setDefault(id)`：设置整合包默认主题，尊重玩家已保存的选择。

ZenScript 与现有场景脚本一样，在游戏启动时执行；修改后重启游戏。
这些声明可以安全出现在专用服务器的脚本中，但不会改变连接玩家的主题。
注册和数据解析不依赖客户端类。未安装 CraftTweaker 时，JSON 和主题界面照常工作。

## 资源包

在资源包中提供索引 `assets/ponder/themes.json`：

```json
{ "themes": ["mypack:night"] }
```

对应定义放在 `assets/mypack/themes/night.json`。内容为单个主题对象
（`parent`、`name`、`description`、`layout`、`colors`），不使用外层 `themes`：

```json
{
  "parent": "ponder:modern",
  "name": "mypack.theme.night.name",
  "colors": { "accent": "#88BBFF" }
}
```

索引会合并所有已启用资源包的条目。相同 ID 的主题文件按 Minecraft 资源包优先级读取。
若要替换内置主题，提供 `assets/ponder/themes/modern.json` 等对应文件；
这会替换该主题的资源定义，省略的颜色仍从其父主题或经典默认值继承。
修改现有主题时不要把 `parent` 写成自己的 ID。

定义叠加顺序：内置资源 → 资源包 → 本地 JSON → ZenScript。
本地 JSON 和 ZenScript 按字段合并；继承在合并完成后解析。
自动默认主题的优先级是 ZenScript `setDefault` → JSON `default` → 经典主题。

## 颜色角色

颜色为 `#RRGGBB`（不透明）或 `#AARRGGBB`（含透明度）。
半透明边框的顶部/底部分开定义，以便形成现代渐变。
现代旁白将场景语义色与 `speech_text` 各混合 50%，同时用于文字和水平引线。

| 区域 | `colors` 键 |
| --- | --- |
| 背景和栏 | `background_top`, `background_bottom`, `header`, `footer` |
| 面板 | `panel`, `panel_alt`, `panel_border` |
| 文字 | `text`, `muted`, `subtle`, `accent`, `selected_text`, `error` |
| 按钮背景 | `button`, `button_hover`, `button_active`, `button_disabled` |
| 默认边框 | `border`, `border_bottom` |
| 悬停边框 | `border_hover`, `border_hover_bottom` |
| 激活边框 | `border_active`, `border_active_bottom` |
| 禁用边框 | `border_disabled`, `border_disabled_bottom` |
| 图标 | `icon`, `icon_disabled` |
| 进度 | `progress_track`, `progress_fill`, `progress_fill_bottom`, `progress_border`, `progress_border_bottom`, `keyframe` |
| 菜单 | `menu_hover`, `menu_selected` |
| 滚动条 | `scroll_track`, `scroll_thumb`, `scroll_active` |
| 旁白 | `speech_background`, `speech_text`, `speech_border`, `speech_border_bottom` |
| 标题 | `title_background`, `title_border`, `title_border_bottom`, `streak` |

完整可复制示例见 [`examples/themes/`](../examples/themes/)：
包含本地 JSON、ZenScript 和完整资源包目录，均不会自动安装到玩家实例。
