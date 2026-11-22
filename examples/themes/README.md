# 主题示例

这里的三种方式可以单独使用，也可以叠加。请按需要复制对应目录到游戏实例。
示例不会由 Mod 自动安装。

- **本地 JSON**：复制 `config/ponder/themes.json`。执行 `/pondertheme reload` 后生效，无需 CraftTweaker。
- **ZenScript**：安装 CraftTweaker，复制 `scripts/ponder/ui_themes.zs`，重启游戏。
- **资源包**：复制 `resourcepacks/PonderThemeExample/` 到实例的 `resourcepacks/`，在资源包设置中启用。

`example:midnight` 继承现代主题；`example:custom` 创建自己的经典布局主题；
示例也展示了如何修改内置 `ponder:classic`。资源包额外提供 `example:sand`。

用 `/pondertheme` 打开选择界面，或直接输入 `/pondertheme example:midnight`。
`/pondertheme auto` 恢复整合包默认主题。显式的玩家选择优先于整合包默认。

同一字段按“内置 → 资源包 → 本地 JSON → ZenScript”的顺序覆盖。
ZenScript 的 `Theme.setDefault` 优先于 JSON 的 `default`。
主题配置只在本地生效，不会由服务器同步给玩家。

完整颜色角色和接口见项目的 `docs/UI-THEMES.md`。
