import mods.ponder.Theme;

// Create a new theme by inheriting all appearance settings from an existing theme.
Theme.extend("example:midnight", "ponder:modern")
    .name("Midnight")
    .description("A modern layout with a blue accent.")
    .color("accent", "#88BBFF")
    .color("border", "#7088BBFF")
    .color("border_bottom", "#3088BBFF")
    .color("progress_fill", "#88BBFF")
    .register();

// Start a theme without specifying a parent. Unspecified colors use the classic defaults.
Theme.create("example:custom")
    .name("My theme")
    .layout("classic")
    .color("background_top", "#17231E")
    .color("background_bottom", "#101714")
    .color("accent", "#88DDBB")
    .register();

// Patch selected fields of an existing theme; other settings remain intact.
Theme.modify("ponder:classic").color("accent", "#99CCBB").register();
Theme.modify("example:midnight").color("text", "#EEF4FF").register();

// Applies when the player selects "Use pack default" (uiTheme=auto).
Theme.setDefault("example:midnight");
