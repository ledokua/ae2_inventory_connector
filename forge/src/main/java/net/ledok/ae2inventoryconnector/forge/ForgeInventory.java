package net.ledok.ae2inventoryconnector.forge;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import appeng.api.stacks.AEItemKey;

import net.ledok.ae2inventoryconnector.platform.PlatformInventory;

/** {@code IItemHandler} capability of a block entity, re-fetched after the capability is invalidated. */
final class ForgeInventory implements PlatformInventory {
    private final ServerLevel level;
    private final BlockPos pos;
    private final Direction side;
    private final Runnable onInvalidated;
    private LazyOptional<IItemHandler> capability = LazyOptional.empty();
    @Nullable
    private IItemHandler reading;

    ForgeInventory(ServerLevel level, BlockPos pos, Direction side, Runnable onInvalidated) {
        this.level = level;
        this.pos = pos;
        this.side = side;
        this.onInvalidated = onInvalidated;
    }

    @Nullable
    private IItemHandler handler() {
        if (!capability.isPresent()) {
            var be = level.getBlockEntity(pos);
            if (be == null) {
                return null;
            }
            capability = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
            if (capability.isPresent()) {
                capability.addListener(lo -> onInvalidated.run());
            }
        }
        return capability.resolve().orElse(null);
    }

    @Override
    public int beginRead() {
        reading = handler();
        return reading == null ? 0 : reading.getSlots();
    }

    @Override
    public long count(int slot) {
        return reading.getStackInSlot(slot).getCount();
    }

    @Override
    public boolean holds(int slot, AEItemKey key) {
        return key.matches(reading.getStackInSlot(slot));
    }

    @Nullable
    @Override
    public AEItemKey key(int slot) {
        return AEItemKey.of(reading.getStackInSlot(slot));
    }

    @Override
    public long insert(AEItemKey what, long amount, boolean simulate) {
        var h = handler();
        if (h == null) {
            return 0;
        }
        int want = (int) Math.min(amount, Integer.MAX_VALUE);
        int inserted = 0;
        // Top up existing stacks first, then fill empty slots.
        for (int pass = 0; pass < 2 && inserted < want; pass++) {
            boolean intoEmpty = pass == 1;
            for (int i = 0; i < h.getSlots() && inserted < want; i++) {
                var inSlot = h.getStackInSlot(i);
                if (intoEmpty ? !inSlot.isEmpty() : !what.matches(inSlot)) {
                    continue;
                }
                int rest = want - inserted;
                inserted += rest - h.insertItem(i, what.toStack(rest), simulate).getCount();
            }
        }
        return inserted;
    }

    @Override
    public long extract(AEItemKey what, long amount, boolean simulate) {
        var h = handler();
        if (h == null) {
            return 0;
        }
        int want = (int) Math.min(amount, Integer.MAX_VALUE);
        int maxStack = what.getMaxStackSize();
        int extracted = 0;
        for (int i = 0; i < h.getSlots() && extracted < want; i++) {
            ItemStack inSlot = h.getStackInSlot(i);
            if (!what.matches(inSlot)) {
                continue;
            }
            if (simulate) {
                int rest = want - extracted;
                int got = Math.min(h.extractItem(i, rest, true).getCount(), rest);
                // Simulating again returns the same thing, so estimate oversized slots (drawers etc.)
                if (got == maxStack && rest > maxStack && inSlot.getCount() > maxStack) {
                    got = Math.min(inSlot.getCount(), rest);
                }
                extracted += got;
            } else {
                while (extracted < want) {
                    int rest = want - extracted;
                    int got = Math.min(h.extractItem(i, rest, false).getCount(), rest);
                    if (got <= 0) {
                        break;
                    }
                    extracted += got;
                    if (!what.matches(h.getStackInSlot(i))) {
                        break;
                    }
                }
            }
        }
        return extracted;
    }
}
