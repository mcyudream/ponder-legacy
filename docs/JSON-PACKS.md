# Ponder JSON scene packs

Ponder Legacy 1.3.0 Alpha 1 adds a reloadable JSON authoring frontend. JSON and
ZenScript compile to the same immutable scene IR, use the same validator and
use snapshot protocol v3.

## Directory layout

```text
scripts/ponder/
├─ packs/
│  └─ **/*.ponder.json
└─ structures/
   └─ <namespace>/<path>.nbt
```

The loader scans pack files recursively in normalized path order. A pack may
contain scenes, tags and component associations, shared text and index
exclusions. Structure NBT remains external to the JSON file.

## Reload behavior

- `/ponder reload` reloads JSON packs and structure caches, then resends the
  server snapshot.
- ZenScript remains startup-only. Editing `.zs` files still requires a client
  or server restart.
- Each JSON file has its own last-known-good contribution. If an edited file is
  invalid, its previous valid contribution remains active while unrelated
  valid files update normally.
- Deleting a file removes its contribution.
- JSON may not replace a ZenScript scene, tag or shared-text key with the same
  ID. Conflicting JSON files are rejected per file.
- On a connected client, reload clears the previous server layer, refreshes
  local JSON and then atomically applies the newly received server snapshot.

## Pack envelope

```json
{
  "$schema": "ponder:schemas/ponder-pack-v1",
  "format": 1,
  "id": "mypack:machines",
  "tags": [],
  "sharedText": {},
  "indexExclusions": [],
  "scenes": []
}
```

Scene instructions use stable, human-facing names such as
`scene.idle`, `world.show_section`, `overlay.show_text` and
`effects.indicate_success`. Internal underscore-only IR names are not accepted.
The versioned operation contract is
`schemas/ponder-operations-v1.json`; the Draft-07 schema is
`schemas/ponder-pack-v1.schema.json`.

Selections are JSON objects:

```json
{ "type": "position", "pos": [2, 1, 2] }
{ "type": "from_to", "from": [1, 1, 1], "to": [3, 2, 3] }
{ "type": "layers", "y": 1, "height": 2 }
{ "type": "structure_group", "name": "mmce:controller" }
```

Entity NBT, tile NBT and custom codec payloads are SNBT compound strings. They must use a
compound envelope, for example:

```json
{
  "op": "world.tile_nbt",
  "selection": { "type": "position", "pos": [3, 1, 2] },
  "nbt": "{Lock:\"mypack\"}",
  "replace": false,
  "redraw": true
}
```

## Entities

Since 1.7.3, `world.create_entity` creates a registered vanilla or mod entity using
scene-local coordinates. `nbt` is a required SNBT compound string; use `"{}"` for
the entity's defaults. The explicit `entity`, `x`, `y` and `z` fields take
precedence over the payload's ID and position.

```json
{
  "op": "world.create_entity",
  "handle": "pool_spark",
  "entity": "botania:spark",
  "x": 2.5,
  "y": 2.5,
  "z": 2.5,
  "nbt": "{upgrade:1}"
}
```

For Botania r1.10-364.4, the normal spark ID is `botania:spark`; `upgrade` is
`0` (normal), `1` (dispersive), `2` (dominant), `3` (recessive), or `4` (isolated).
Corporea sparks use `botania:corporea_spark`, with `{master:1b}` for a master spark.
Botania remains optional; only scenes using its entities require it.

The equivalent ZenScript calls are:

```zenscript
scene.world.createEntity("pool_spark", "botania:spark", 2.5, 2.5, 2.5);
scene.world.createEntity("upgraded_spark", "botania:spark", 4.5, 2.5, 2.5, {upgrade: 1});
scene.world.createEntity("corporea_master", "botania:corporea_spark", 6.5, 2.5, 2.5, {master: true});
```

Vanilla structure NBT imports and editor captures retain these entities and their
upgrade/master data. Include the air layer containing the sparks in the selection.
Previously captured files with an empty `entities` list must be captured again.

Since `1.8.0`, captured/imported entities start hidden at tick zero and
follow the visibility and movement of their schematic section. An entity in air
immediately above a block follows that supporting block; otherwise it follows
its own cell. `world.show_section` with `everywhere` also includes floating entity
cells. Seeking back to zero and replaying restore the hidden state. Explicit
`world.create_entity` instructions still create visible entities when executed,
independently of the schematic sections.

## Offline tool

Validate a pack without starting Minecraft:

```powershell
.\tools\ponder-json.ps1 validate .\scripts\ponder\packs\demo.ponder.json
```

Write a separate canonical copy:

```powershell
.\tools\ponder-json.ps1 migrate .\demo.ponder.json `
  -OutputPath .\demo.migrated.ponder.json
```

`migrate` never overwrites the input file. The offline tool validates the pack
envelope, required operation fields, selection shapes, resource IDs, limits and
the SNBT compound envelope. Minecraft remains the authoritative validator for
full SNBT syntax, block states, enum values, handle lifecycles and custom
instruction codecs.

The installable example is built as
`build/distributions/Ponder-JSON-Examples-1.3.0-alpha.1.zip`. The standalone schema and
PowerShell author tools are built as
`build/distributions/Ponder-JSON-Tools-1.3.0-alpha.1.zip`.
