package net.ledok.ae2inventoryconnector.storage;

import com.google.common.primitives.Ints;

import net.minecraft.network.chat.Component;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;

import net.ledok.ae2inventoryconnector.AE2InventoryConnector;

/**
 * The {@link MEStorage} a connector mounts into its grid. Listing items is a copy of the cached totals (O(item types),
 * not O(slots)); only inventories marked dirty are re-read first.
 */
public final class ConnectorStorage implements MEStorage {
    private final InventorySet inventories;

    public ConnectorStorage(InventorySet inventories) {
        this.inventories = inventories;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey itemKey)) {
            return 0;
        }
        inventories.flush();
        int want = Ints.saturatedCast(amount);
        boolean simulate = mode == Actionable.SIMULATE;
        long inserted = 0;
        // Top up existing stacks first, then fill empty slots, so items don't get scattered.
        for (var inv : inventories.list()) {
            if (inserted >= want) {
                break;
            }
            inserted += inv.insert(itemKey, (int) (want - inserted), simulate, false);
        }
        for (var inv : inventories.list()) {
            if (inserted >= want) {
                break;
            }
            inserted += inv.insert(itemKey, (int) (want - inserted), simulate, true);
        }
        inventories.markContentChecked();
        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!(what instanceof AEItemKey itemKey)) {
            return 0;
        }
        inventories.flush();
        if (inventories.totals().get(itemKey) <= 0) {
            return 0;
        }
        int want = Ints.saturatedCast(amount);
        boolean simulate = mode == Actionable.SIMULATE;
        long extracted = 0;
        for (var inv : inventories.list()) {
            if (extracted >= want) {
                break;
            }
            extracted += inv.extract(itemKey, (int) (want - extracted), simulate);
        }
        inventories.markContentChecked();
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        inventories.flush();
        out.addAll(inventories.totals());
    }

    @Override
    public Component getDescription() {
        return AE2InventoryConnector.ME_INVENTORY_CONNECTOR.get().getName();
    }
}
