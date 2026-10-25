package net.ledok.ae2inventoryconnector.storage;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;

import appeng.api.stacks.KeyCounter;

import net.ledok.ae2inventoryconnector.Config;
import net.ledok.ae2inventoryconnector.ModTags;
import net.ledok.ae2inventoryconnector.storage.ConnectorIndex.Kind;
import net.ledok.ae2inventoryconnector.storage.ConnectorIndex.Watch;

/**
 * Everything a connector knows about the inventories around it: which ones are connected (topology) and what they
 * hold (contents). Both are only recomputed after an event said they might have changed.
 */
public final class InventorySet {
    private final BlockEntity owner;
    private final KeyCounter totals = new KeyCounter();
    private Map<BlockPos, ConnectedInventory> byPos = new LinkedHashMap<>();
    private List<ConnectedInventory> list = List.of();
    private boolean topologyDirty = true;
    private boolean contentDirty;
    private boolean disposed;

    public InventorySet(BlockEntity owner) {
        this.owner = owner;
    }

    List<ConnectedInventory> list() {
        return list;
    }

    KeyCounter totals() {
        return totals;
    }

    /** Brings topology and contents up to date. Cheap when nothing changed. */
    public void flush() {
        if (topologyDirty) {
            rescan();
        }
        if (!contentDirty) {
            return;
        }
        contentDirty = false;
        boolean removed = false;
        for (var inv : list) {
            if (inv.dirty) {
                removed |= inv.refresh(totals);
            }
        }
        if (removed) {
            totals.removeZeros();
        }
    }

    public void markTopologyDirty() {
        if (disposed) {
            return;
        }
        topologyDirty = true;
        ConnectorIndex.queueRescan(this);
    }

    /** Called after our own insert/extract, which flag the touched inventories themselves. */
    void markContentChecked() {
        contentDirty = true;
    }

    void markContentDirty(@Nullable ConnectedInventory inv) {
        if (inv != null) {
            inv.dirty = true;
            contentDirty = true;
        }
    }

    void markAllContentDirty() {
        for (var inv : list) {
            inv.dirty = true;
        }
        contentDirty = !list.isEmpty();
    }

    void rescanIfDirty() {
        if (topologyDirty) {
            rescan();
        }
    }

    /** A watched block was updated (placed, broken, changed state). Decide what has to be redone. */
    void onWatchedBlockChanged(Watch w, ServerLevel level, BlockPos pos, BlockState state) {
        if (topologyDirty || disposed) {
            return;
        }
        switch (w.kind()) {
            case INVENTORY -> {
                if (state.getBlock() != w.state().getBlock() || chestTypeChanged(w.state(), state)) {
                    markTopologyDirty();
                } else {
                    markContentDirty(w.inv());
                }
            }
            case TRIM -> {
                if (!state.is(ModTags.TRIMS)) {
                    markTopologyDirty();
                }
            }
            case NONE -> {
                if (classify(level, pos, state, w.side()) != Kind.NONE) {
                    markTopologyDirty();
                }
            }
        }
    }

    private void rescan() {
        topologyDirty = false;
        if (disposed || !(owner.getLevel() instanceof ServerLevel level) || owner.isRemoved()) {
            return;
        }
        var index = ConnectorIndex.get(level);
        index.unwatchAll(this);

        BlockPos origin = owner.getBlockPos();
        int range = Config.SCAN_RANGE.getAsInt();
        long maxDistSqr = (long) range * range;
        int budget = Config.MAX_SCANNED_BLOCKS.getAsInt();
        boolean onlyTrims = Config.ONLY_TRIMS_CONNECT.getAsBoolean();

        var old = byPos;
        var next = new LinkedHashMap<BlockPos, ConnectedInventory>();
        var visited = new LongOpenHashSet();
        visited.add(origin.asLong());
        var queue = new ArrayDeque<BlockPos>();
        queue.add(origin);
        boolean removed = false;

        while (!queue.isEmpty() && budget > 0) {
            var cur = queue.poll();
            for (var d : Direction.values()) {
                var p = cur.relative(d);
                if (!visited.add(p.asLong()) || p.distSqr(origin) > maxDistSqr) {
                    continue;
                }
                budget--;
                if (!level.isLoaded(p)) {
                    // Rescan when it loads; never load chunks ourselves.
                    index.watchChunk(this, p);
                    continue;
                }

                var side = d.getOpposite();
                var state = level.getBlockState(p);
                var kind = classify(level, p, state, side);
                ConnectedInventory inv = null;
                if (kind == Kind.INVENTORY) {
                    // Both halves of a double chest expose the same combined inventory: count it once.
                    var partner = chestPartner(p, state);
                    inv = partner != null ? next.get(partner) : null;
                    if (inv == null) {
                        inv = old.remove(p);
                        if (inv != null && (inv.side != side || inv.state != state)) {
                            inv.detach(totals);
                            removed = true;
                            inv = null;
                        }
                        if (inv == null) {
                            inv = new ConnectedInventory(level, p, side, state, () -> !owner.isRemoved(),
                                    this::markTopologyDirty);
                        }
                        next.put(p, inv);
                    }
                    if (!onlyTrims) {
                        queue.add(p);
                    }
                } else if (kind == Kind.TRIM) {
                    queue.add(p);
                }
                index.watch(this, p, new Watch(this, kind, side, state, inv));
            }
        }

        for (var gone : old.values()) {
            gone.detach(totals);
            removed = true;
        }
        if (removed) {
            totals.removeZeros();
        }
        byPos = next;
        list = List.copyOf(next.values());
        for (var inv : list) {
            if (inv.dirty) {
                contentDirty = true;
                break;
            }
        }
    }

    static Kind classify(Level level, BlockPos pos, BlockState state, Direction side) {
        if (state.isAir()) {
            return Kind.NONE;
        }
        if (state.is(ModTags.TRIMS)) {
            return Kind.TRIM;
        }
        if (state.is(ModTags.CONNECTOR_SKIP)) {
            return Kind.NONE;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) != null ? Kind.INVENTORY : Kind.NONE;
    }

    @Nullable
    private static BlockPos chestPartner(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return pos.relative(ChestBlock.getConnectedDirection(state));
        }
        return null;
    }

    private static boolean chestTypeChanged(BlockState before, BlockState after) {
        return before.hasProperty(ChestBlock.TYPE) && after.hasProperty(ChestBlock.TYPE)
                && before.getValue(ChestBlock.TYPE) != after.getValue(ChestBlock.TYPE);
    }

    /** Forget everything; the owner is gone or unloaded. */
    public void dispose() {
        disposed = true;
        if (owner.getLevel() instanceof ServerLevel level) {
            ConnectorIndex.remove(level, this);
        }
        for (var inv : list) {
            inv.detach(totals);
        }
        byPos = new LinkedHashMap<>();
        list = List.of();
        totals.clear();
    }

    public Usage usage() {
        flush();
        int slots = 0;
        int used = 0;
        for (var inv : list) {
            slots += inv.slotCount();
            used += inv.usedSlots();
        }
        return new Usage(list.size(), slots, used, totals.size());
    }

    public record Usage(int inventories, int slots, int usedSlots, int itemTypes) {
    }
}
