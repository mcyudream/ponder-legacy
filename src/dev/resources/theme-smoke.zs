import mods.ponder.Theme;

Theme.extend("theme_smoke:zen", "ponder:modern")
    .name("ZenScript theme")
    .color("accent", "#FFCC66")
    .register();
Theme.modify("theme_smoke:zen").color("text", "#EEF2FF").register();
Theme.create("theme_smoke:fresh").name("Fresh theme").layout("classic")
    .color("button", "#DD202010").register();
Theme.setDefault("theme_smoke:zen");
