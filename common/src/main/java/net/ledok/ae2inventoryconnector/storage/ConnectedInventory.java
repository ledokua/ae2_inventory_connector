package net.ledok.ae2inventoryconnector.storage;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;

import net.ledok.ae2inventoryconnector.platform.PlatformInventory;

/**
 * One inventory found by a connector, plus a per-slot snapshot of what it held the last time it was read.
 * <p>
 * The snapshot is only re-read when the inventory is marked {@link #dirty} (by a setChanged() of its block entity, a
 * block update, or our own insert/extract), so an idle chest costs nothing.
 */
public final class ConnectedInventory {
    final BlockPos pos;
    final Direction side;
    final BlockState state;
    private final PlatformInventory inventory;

    private AEItemKey[] keys = new AEItemKey[0];
    private long[] counts = new long[0];
    private boolean alive = true;
    boolean dirty = true;

    ConnectedInventory(BlockPos pos, Direction side, BlockState state, PlatformInventory inventory) {
        this.pos = pos;
        this.side = side;
        this.state = state;
        this.inventory = inventory;
    }

    /**
     * Re-reads all slots and applies the difference to {@code totals}.
     *
     * @return true if some key may now have a zero amount in {@code totals}
     */
    boolean refresh(KeyCounter totals) {
        dirty = false;
        int slots = alive ? inventory.beginRead() : 0;
        boolean removed = false;

        if (slots < keys.length) {
            for (int i = slots; i < keys.length; i++) {
                if (keys[i] != null) {
                    totals.remove(keys[i], counts[i]);
                    removed = true;
                }
            }
            keys = Arrays.copyOf(keys, slots);
            counts = Arrays.copyOf(counts, slots);
        } else if (slots > keys.length) {
            keys = Arrays.copyOf(keys, slots);
            counts = Arrays.copyOf(counts, slots);
        }

        for (int i = 0; i < slots; i++) {
            long count = inventory.count(i);
            AEItemKey old = keys[i];
            if (count <= 0) {
                if (old != null) {
                    totals.remove(old, counts[i]);
                    keys[i] = null;
                    counts[i] = 0;
                    removed = true;
                }
                continue;
            }

            if (old != null && inventory.holds(i, old)) {
                if (count != counts[i]) {
                    totals.add(old, count - counts[i]);
                    counts[i] = count;
                }
                continue;
            }

            if (old != null) {
                totals.remove(old, counts[i]);
                removed = true;
            }
            var key = inventory.key(i);
            keys[i] = key;
            counts[i] = key == null ? 0 : count;
            if (key != null) {
                totals.add(key, count);
            }
        }
        return removed;
    }

    /** Removes this inventory's contribution from {@code totals} and stops using it. */
    void detach(KeyCounter totals) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                totals.remove(keys[i], counts[i]);
            }
        }
        keys = new AEItemKey[0];
        counts = new long[0];
        alive = false;
    }

    /** Whether the last snapshot saw {@code what} here. */
    boolean contains(AEItemKey what) {
        for (var k : keys) {
            if (what.equals(k)) {
                return true;
            }
        }
        return false;
    }

    long insert(AEItemKey what, long amount, boolean simulate) {
        if (!alive) {
            return 0;
        }
        long inserted = inventory.insert(what, amount, simulate);
        if (inserted > 0 && !simulate) {
            dirty = true;
        }
        return inserted;
    }

    long extract(AEItemKey what, long amount, boolean simulate) {
        if (!alive) {
            return 0;
        }
        long extracted = inventory.extract(what, amount, simulate);
        if (extracted > 0 && !simulate) {
            dirty = true;
        }
        return extracted;
    }

    int slotCount() {
        return keys.length;
    }

    int usedSlots() {
        int used = 0;
        for (var k : keys) {
            if (k != null) {
                used++;
            }
        }
        return used;
    }
}
