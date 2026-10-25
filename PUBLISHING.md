# Publishing on Modrinth and CurseForge

Copy-paste material for the project pages. One project per site covers every loader and Minecraft
version. Upload each jar as a separate file/version.

## Basics

| Field | Value |
|---|---|
| Name | AE2 Inventory Connector |
| Slug / URL | `ae2-inventory-connector` |
| Icon | `assets/ae2_inventory_connector/icon.png` (425×425, transparent background) |
| License | MIT |
| Source | https://github.com/ledokua/ae2_inventory_connector |
| Issues | https://github.com/ledokua/ae2_inventory_connector/issues |
| Environment | **Client and server** (it adds a block, so both sides need it) |

**Summary** (Modrinth allows ~256 characters):

> A Tom's Storage style inventory connector that plugs straight into AE2 cables. Mounts chests and other inventories as ME storage, event driven, with no per-tick scanning. Built to fix TPS lag from Inventory Connector + Storage Bus setups.

**Short summary** (for CurseForge's short field):

> Lag-free inventory connector for AE2 networks

## Categories / tags

- **Modrinth:** Storage, Technology, Optimization, Utility. Loaders: Fabric, Forge, NeoForge.
- **CurseForge:** main category *Storage*; also *Technology*, *Server Utility* and, if listed, the
  *Applied Energistics 2* addon category.

## Dependencies to declare

| Dependency | Minecraft 1.20.1 Forge | 1.20.1 Fabric | 1.21.1 NeoForge |
|---|---|---|---|
| Applied Energistics 2 | required (15.0+) | required (15.0+) | required (19.2+) |
| Fabric API | — | required | — |
| Tom's Simple Storage | optional (its trims are supported) | optional | optional |

## Files / versions

| File | Loader | Game version | Version name | Version number |
|---|---|---|---|---|
| `ae2_inventory_connector-forge-1.0.0.jar` | Forge | 1.20.1 | AE2 Inventory Connector 1.0.0 (Forge 1.20.1) | `1.0.0+1.20.1-forge` |
| `ae2_inventory_connector-fabric-1.0.0.jar` | Fabric | 1.20.1 | AE2 Inventory Connector 1.0.0 (Fabric 1.20.1) | `1.0.0+1.20.1-fabric` |
| `ae2_inventory_connector-1.21.1-1.0.0.jar` | NeoForge | 1.21.1 | AE2 Inventory Connector 1.0.0 (NeoForge 1.21.1) | `1.0.0+1.21.1-neoforge` |

Upload only the main jars, not the `-sources` jars or the `-common` jar (that one isn't a mod).
Release channel: *Release* (or *Beta* until it has run on a busy server for a while).

**Changelog for 1.0.0:**

> First release. Adds the ME Inventory Connector: an AE2 device that mounts nearby inventories (through Tom's Storage trims) as ME storage without ticking. Config option `connectorBlacklist` and the `ae2_inventory_connector:connector_skip` block tag control which blocks are used.

## Description (Markdown; works on Modrinth and in CurseForge's Markdown editor)

```markdown
# AE2 Inventory Connector

Tom's Storage **Inventory Connector** + an AE2 **Storage Bus** is a popular way to put a wall of chests
into an ME network, and one of the most common causes of server lag. Tom's connector re-scans its
area every second, and the storage bus re-reads every slot of every connected chest whenever it
runs.

**AE2 Inventory Connector** replaces both with one block that does the same job without ticking.

## How it works

Place the **ME Inventory Connector** next to your chests (or connect them with Tom's trims), then
attach an ME cable to it. It uses one channel, like a storage bus. Every chest, barrel and modded
inventory it finds shows up in your ME network as storage.

- **No ticking.** The block reacts to events: an inventory changing, a block being placed or broken,
  a chunk loading. If nothing changes, it costs nothing.
- **Only re-reads what changed.** When a chest changes, only that chest is read again, and only
  when AE2 actually asks.
- **Cheap for AE2.** AE2 asks for the item list every tick while a terminal is open. The connector
  answers from a cached total, so the cost depends on how many item types you have, not how many slots.
- **Works with Tom's Storage.** Its trims connect inventories just like for Tom's own connector.
  Tom's Storage itself is optional.

Right-click the connector to see used slots, connected inventories and item types. Right-clicking
also forces a rescan.

## Configuration

- **Range, scan limit, power use and "only trims connect"** are in the server config.
- **`connectorBlacklist`** lists blocks the connector must never use: a block id (`minecraft:furnace`),
  a block tag (`#c:barrels`) or a whole mod (`@sophisticatedstorage`). On 1.20.1 edit
  `config/ae2_inventory_connector.properties` and run `/reload`. On 1.21.1 edit
  `serverconfig/ae2_inventory_connector-server.toml`; saving the file applies it.
- Pack makers can also use the `ae2_inventory_connector:connector_skip` and
  `ae2_inventory_connector:trims` block tags.

## Recipe

Iron ingots, two AE2 Storage Buses, two chests and an Engineering Processor.

## Versions

- Minecraft **1.20.1**: Forge and Fabric (AE2 15.x)
- Minecraft **1.21.1**: NeoForge (AE2 19.x)

Source and issues: https://github.com/ledokua/ae2_inventory_connector
```

## Screenshot ideas for the gallery

1. A connector between a wall of chests and an ME cable, with an ME terminal open showing the items.
2. The right-click message with slot and inventory counts.
3. A Tom's trim line connecting chests that are spread apart.
4. The recipe in JEI/EMI.
