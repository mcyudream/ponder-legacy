# Sources and notices

This Cleanroom fork is based on MCMGGF/ponder-for-1.12.2, revision
`c494bf7692c7b7363ef86ed82954874be6e49074` (MIT), and retains its Ponder and
Catnip implementation, API, translations, visual assets, scripts and tests.

- 1.12.2 base: https://github.com/MCMGGF/ponder-for-1.12.2
- Original Ponder: https://github.com/Creators-of-Create/Ponder
- Original copyright: Copyright (c) 2022 The Create Team
- MIT license: `LICENSE`

The optional Modern Ponder UI theme adapts the visual composition and border
palette from Creators-of-Create/Ponder's `mc1.21.1/dev` branch at revision
`89819291b726708d119b118dea01667eae0b64c9` (MIT). The existing upstream widget
atlas is reused for matching modern navigation icons. The 1.12 renderer and
theme loading, configuration, JSON and ZenScript APIs are implemented in this fork.

The authoring workflow takes inspiration from Ponderer (MIT):
https://github.com/Nobodiiiii/Ponderer . Its modern Minecraft implementation is
not a runtime dependency of this fork.

Cleanroom and CraftTweaker are external dependencies, not embedded in the mod
jar. Cleanroom supplies its MixinBooter compatibility API.

The older Forge release instructions and tools in docs/, tools/ and verification/
are inherited reference material. See README.md and docs/CLEANROOM.md for this
fork's build and validation results.

## GuideME Markdown module

The relocated parser in `third_party/guideme-markdown/` is taken from the MIT-licensed
`markdown/` module of AppliedEnergistics/GuideME, revision
`cb12bc543ff051e1dbc3828bfeed521a5e3cdd59`. Its original MIT copyright and
permission notice are preserved in that directory and in the installation jar at
`META-INF/licenses/guideme-markdown-MIT.txt`. Changes are recorded in UPSTREAM.json.
The native 1.12.2 guide renderer, tutorial compiler and editor are maintained here.
GuideME's LGPL Minecraft/NeoForge integration is not included in this distribution.
