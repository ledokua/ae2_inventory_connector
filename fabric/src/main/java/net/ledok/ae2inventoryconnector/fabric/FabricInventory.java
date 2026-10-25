package net.ledok.ae2inventoryconnector.fabric;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;

import appeng.api.stacks.AEItemKey;

import net.ledok.ae2inventoryconnector.platform.PlatformInventory;

/** {@code Storage<ItemVariant>} seen as slots: the storage's slots if it is slotted, otherwise its views. */
final class FabricInventory implements PlatformInventory {
    private final BlockApiCache<Storage<ItemVariant>, Direction> cache;
    private final Direction side;
    private final List<StorageView<ItemVariant>> views = new ArrayList<>();

    FabricInventory(BlockApiCache<Storage<ItemVariant>, Direction> cache, Direction side) {
        this.cache = cache;
        this.side = side;
    }

    @Nullable
    private Storage<ItemVariant> storage() {
        return cache.find(side);
    }

    @Override
    public int beginRead() {
        views.clear();
        var storage = storage();
        if (storage instanceof SlottedStorage<ItemVariant> slotted) {
            int n = slotted.getSlotCount();
            for (int i = 0; i < n; i++) {
                views.add(slotted.getSlot(i));
            }
        } else if (storage != null) {
            for (var view : storage) {
                views.add(view);
            }
        }
        return views.size();
    }

    @Override
    public long count(int slot) {
        var view = views.get(slot);
        return view.isResourceBlank() ? 0 : view.getAmount();
    }

    @Override
    public boolean holds(int slot, AEItemKey key) {
        var variant = views.get(slot).getResource();
        return variant.isOf(key.getItem()) && variant.nbtMatches(key.getTag());
    }

    @Nullable
    @Override
    public AEItemKey key(int slot) {
        var view = views.get(slot);
        return view.isResourceBlank() ? null : AEItemKey.of(view.getResource());
    }

    @Override
    public long insert(AEItemKey what, long amount, boolean simulate) {
        var storage = storage();
        if (storage == null || !storage.supportsInsertion()) {
            return 0;
        }
        try (var tx = openTransaction()) {
            long inserted = storage.insert(what.toVariant(), amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return inserted;
        }
    }

    @Override
    public long extract(AEItemKey what, long amount, boolean simulate) {
        var storage = storage();
        if (storage == null || !storage.supportsExtraction()) {
            return 0;
        }
        try (var tx = openTransaction()) {
            long extracted = storage.extract(what.toVariant(), amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }

    /** AE may already be inside a transaction (e.g. another mod moving items through the grid). */
    private static Transaction openTransaction() {
        var current = Transaction.getCurrentUnsafe();
        return current != null ? current.openNested() : Transaction.openOuter();
    }
}
