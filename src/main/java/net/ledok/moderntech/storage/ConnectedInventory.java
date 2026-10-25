package net.ledok.moderntech.storage;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;

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
    private final BlockCapabilityCache<IItemHandler, @Nullable Direction> cache;

    private AEItemKey[] keys = new AEItemKey[0];
    private int[] counts = new int[0];
    private boolean alive = true;
    boolean dirty = true;

    ConnectedInventory(ServerLevel level, BlockPos pos, Direction side, BlockState state, BooleanSupplier ownerValid,
            Runnable onInvalidate) {
        this.pos = pos;
        this.side = side;
        this.state = state;
        this.cache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, pos, side,
                () -> alive && ownerValid.getAsBoolean(), onInvalidate);
    }

    @Nullable
    private IItemHandler handler() {
        return alive ? cache.getCapability() : null;
    }

    /**
     * Re-reads all slots and applies the difference to {@code totals}.
     *
     * @return true if some key may now have a zero amount in {@code totals}
     */
    boolean refresh(KeyCounter totals) {
        dirty = false;
        var h = handler();
        int slots = h == null ? 0 : h.getSlots();
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
            ItemStack stack = h.getStackInSlot(i);
            AEItemKey old = keys[i];
            if (stack.isEmpty()) {
                if (old != null) {
                    totals.remove(old, counts[i]);
                    keys[i] = null;
                    counts[i] = 0;
                    removed = true;
                }
                continue;
            }

            int count = stack.getCount();
            if (old != null && old.matches(stack)) {
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
            var key = AEItemKey.of(stack);
            keys[i] = key;
            counts[i] = count;
            totals.add(key, count);
        }
        return removed;
    }

    /** Removes this inventory's contribution from {@code totals} and stops using its capability. */
    void detach(KeyCounter totals) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                totals.remove(keys[i], counts[i]);
            }
        }
        keys = new AEItemKey[0];
        counts = new int[0];
        alive = false;
    }

    /**
     * @param intoEmpty false: only slots that already hold {@code what}; true: only slots that were empty
     */
    int insert(AEItemKey what, int amount, boolean simulate, boolean intoEmpty) {
        var h = handler();
        if (h == null) {
            return 0;
        }
        int inserted = 0;
        int n = Math.min(keys.length, h.getSlots());
        for (int i = 0; i < n && inserted < amount; i++) {
            var k = keys[i];
            if (intoEmpty ? k != null : !what.equals(k)) {
                continue;
            }
            int want = amount - inserted;
            int done = want - h.insertItem(i, what.toStack(want), simulate).getCount();
            if (done > 0) {
                inserted += done;
                if (!simulate) {
                    dirty = true;
                }
            }
        }
        return inserted;
    }

    int extract(AEItemKey what, int amount, boolean simulate) {
        var h = handler();
        if (h == null) {
            return 0;
        }
        int maxStack = what.getMaxStackSize();
        int extracted = 0;
        int n = Math.min(keys.length, h.getSlots());
        for (int i = 0; i < n && extracted < amount; i++) {
            if (!what.equals(keys[i])) {
                continue;
            }
            // The snapshot may be stale: never extract a slot without checking what is really in it.
            ItemStack inSlot = h.getStackInSlot(i);
            if (!what.matches(inSlot)) {
                dirty = true;
                continue;
            }

            if (simulate) {
                int inSlotCount = inSlot.getCount();
                int want = amount - extracted;
                int got = clamp(h.extractItem(i, want, true).getCount(), want);
                // Simulating again would return the same thing, so guess for oversized slots (drawers, etc.)
                if (got == maxStack && want > maxStack && inSlotCount > maxStack) {
                    got = Math.min(inSlotCount, want);
                }
                extracted += got;
            } else {
                while (extracted < amount) {
                    int want = amount - extracted;
                    int got = clamp(h.extractItem(i, want, false).getCount(), want);
                    if (got <= 0) {
                        break;
                    }
                    extracted += got;
                    dirty = true;
                    if (!what.matches(h.getStackInSlot(i))) {
                        break;
                    }
                }
            }
        }
        return extracted;
    }

    private static int clamp(int got, int requested) {
        // Broken handlers sometimes return more than asked; never report more than that.
        return Math.min(got, requested);
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
