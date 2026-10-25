# AE2 Inventory Connector (NeoForge 1.21.1)

`ae2_inventory_connector:me_inventory_connector` works like `toms_storage:inventory_connector`: it flood-fills through
inventories and trims (range 16 by default). Instead of feeding a Tom's network, it **is an AE2 device**.
Connect it to an ME cable like a storage bus or pattern provider. It uses one channel and mounts everything
it finds as ME storage.

## Why it's faster than Tom's connector + storage bus

| | Tom's connector + storage bus | ME Inventory Connector |
|---|---|---|
| Find inventories | flood-fill every 20 ticks | only after a block update, capability invalidation or chunk load in the scanned area |
| Find item changes | storage bus re-reads **every slot of every inventory** on each tick it runs (and Tom's tracker reads them all again) | `BlockEntity#setChanged` (mixin) marks only that inventory dirty; it is re-read once, the next time AE asks |
| Listing items for AE | builds keys for every slot | copies a cached `KeyCounter` (cost = number of item types) |
| Ticking | Tom's BE ticks + storage bus ticks | no block entity ticker; topology rescans run in a single end-of-tick handler, only for connectors with pending changes |

## Config (`serverconfig/ae2_inventory_connector-server.toml`)
- `scanRange` (16), `maxScannedBlocks` (8192), `onlyTrimsConnect` (false): same meaning as in Tom's.
- `idlePowerUsage` (1.0 AE/t).
- `safetyRefreshTicks` (200): a few modded inventories change items without calling `setChanged()`.
  Every N ticks, every inventory is *flagged* so it gets re-read on the next AE query. Set to 0 to turn it off.

## Tags
- `ae2_inventory_connector:trims` includes `#toms_storage:trims`, so Tom's trims and painted trims work as connectors.
- `ae2_inventory_connector:connector_skip`: blocks that are never treated as inventories (Tom's connectors/proxies,
  AE cable bus/interface/pattern provider, ...). Add entries with a datapack.

## Build
`./gradlew build`. Output: `build/libs/ae2_inventory_connector-1.21.1-<version>.jar`. Requires AE2 19.2+.
Tom's Storage is optional (only its trims tag is used).
