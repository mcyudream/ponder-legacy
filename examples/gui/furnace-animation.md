---
id: example:furnace_gui_markdown
title: 熔炉：认识操作界面
item_ids: [minecraft:furnace]
structure: ponder:demo/smelting
presentation: both
step_ticks: 100
---
# 熔炉：认识操作界面

## 放置机器

为输入、燃料与输出留出连接空间。

```ponder
[
  {"op":"world.set_blocks","selection":{"type":"layers_from","y":1},"state":"minecraft:air","particles":false},
  {"op":"world.set_block","x":2,"y":1,"z":2,"state":"minecraft:furnace","particles":false}
]
```

## 认识面板

上方放入原料，下方放入燃料，右侧取出成品。

<GuiTutorial id="ponder:examples/furnace" duration={300} side="right" pointX={2.5} pointY={1.5} pointZ={2.5} />

## 观察冶炼进度

箭头逐渐填满，完成后产物出现在右侧。

<GuiTutorial id="ponder:examples/furnace" duration={100} side="right" from={232} to={232} />

## 继续自动化

将原料、燃料和成品接到各自的路线。

```ponder
[{"op":"scene.rotate_camera","degrees":45}]
```
