# AE2 Inventory Connector (NeoForge 1.21.1)

Tom's Storage **Inventory Connector** + an AE2 **Storage Bus** is a popular way to put a wall of chests
into an ME network, and a common cause of server lag. Tom's connector re-scans its area every second,
and the storage bus re-reads every slot of every connected chest whenever it runs. While a terminal is
open, AE2 also asks every storage for its full item list every tick.

**ME Inventory Connector** (`ae2_inventory_connector:me_inventory_connector`) does the same job with
one block and no ticking. It finds inventories the way Tom's connector does (through inventories and
Tom's trims, range 16), but it **is an AE2 device**: attach an ME cable to it, like a storage bus or
pattern provider. It uses one channel and mounts everything it finds as ME storage.

## How it stays cheap

| | Tom's connector + storage bus | ME Inventory Connector |
|---|---|---|
| Finding inventories | flood-fill every 20 ticks | only after a watched block changes, a watched chunk loads/unloads, or (Forge/NeoForge) an inventory capability goes away |
| Noticing item changes | storage bus re-reads **every slot of every inventory** each time it runs | a `BlockEntity#setChanged` hook marks just that inventory; it is re-read once, the next time AE2 asks |
| Answering AE2's item list | builds keys for every slot | copies a cached total (cost = number of item types) |
| Ticking | Tom's block entity + storage bus | none; pending rescans are merged and run once at the end of the server tick |

Right-click the connector to see used slots, inventories and item types; that also forces a rescan
(useful after WorldEdit-style edits that skip block updates).

## Choosing what connects

- `ae2_inventory_connector:trims` (block tag): blocks the scan passes through. Includes Tom's trims.
- `ae2_inventory_connector:connector_skip` (block tag): never used as inventories. By default: the
  connector itself; Tom's connectors, cable connectors and proxies (they expose a whole Tom's network,
  which would be counted twice); AE2's cable bus, interface, pattern provider, ME chest, drive and IO port.
- `connectorBlacklist` (config): the same as the skip tag, without a datapack. Entries are a block id
  (`minecraft:furnace`), a block tag (`#c:barrels`) or a whole mod (`@sophisticatedstorage`).

## Config: `serverconfig/ae2_inventory_connector-server.toml` (per world)

| Key | Default | Meaning |
|---|---|---|
| `scanRange` | 16 | max distance of discovered inventories (Tom's `inventoryConnectorRange`) |
| `maxScannedBlocks` | 8192 | hard cap on blocks visited per scan |
| `onlyTrimsConnect` | false | scan continues only through trims, not through inventories |
| `idlePowerUsage` | 1.0 | AE energy per tick per connector |
| `safetyRefreshTicks` | 200 | every N ticks, flag all inventories for a lazy re-read (for mods that skip `setChanged`); 0 = off |
| `connectorBlacklist` | `[]` | e.g. `["minecraft:furnace", "#c:barrels", "@sophisticatedstorage"]` |

NeoForge watches the file: saving it applies the change and rescans every connector. `/reload` rescans
too (tags may have changed).

## Install

Needs NeoForge 21.1+ for Minecraft 1.21.1 and Applied Energistics 2 19.2+. Tom's Simple Storage is
optional. Install on the server **and** the clients.

## Build

`./gradlew build` produces `build/libs/ae2_inventory_connector-1.21.1-<version>.jar`.
The 1.20.1 Forge + Fabric version lives on the [`1.20.1` branch](https://github.com/ledokua/ae2_inventory_connector/tree/1.20.1).

See [PUBLISHING.md](PUBLISHING.md) for the Modrinth/CurseForge page text.
