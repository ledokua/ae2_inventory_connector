package net.ledok.ae2inventoryconnector.platform;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEItemKey;

/**
 * A loader's item inventory (Forge {@code IItemHandler}, Fabric {@code Storage<ItemVariant>}), seen as numbered
 * slots for snapshotting plus whole-inventory insert/extract.
 */
public interface PlatformInventory {
    /** Prepares a read pass and returns the slot count. Slot indices stay valid until the next call. */
    int beginRead();

    /** Amount in the slot; 0 when empty. */
    long count(int slot);

    /** Whether the slot holds {@code key} (cheaper than building a key). */
    boolean holds(int slot, AEItemKey key);

    @Nullable
    AEItemKey key(int slot);

    long insert(AEItemKey what, long amount, boolean simulate);

    long extract(AEItemKey what, long amount, boolean simulate);
}
