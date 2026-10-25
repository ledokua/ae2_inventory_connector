package net.ledok.ae2inventoryconnector.storage;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.ledok.ae2inventoryconnector.Config;

/**
 * Per-level lookup from block position to the connectors that care about it. This is what replaces ticking: every
 * relevant world change is pushed to us by an event, and we answer with a single hash lookup.
 * <ul>
 * <li>{@code setChanged()} of a block entity (mixin) &rarr; that inventory's contents are re-read when AE next asks.</li>
 * <li>neighbor-notify of a block (placed/broken/state change) &rarr; contents or topology are invalidated.</li>
 * <li>capability invalidation (in {@link ConnectedInventory}) and chunk load &rarr; topology rescan.</li>
 * </ul>
 * Topology rescans are deferred to the end of the server tick and merged, so a burst of changes costs one scan.
 */
public final class ConnectorIndex {
    public enum Kind {
        /** Not part of the network, but a change here may connect something new. */
        NONE,
        TRIM,
        INVENTORY
    }

    record Watch(InventorySet owner, Kind kind, Direction side, BlockState state, @Nullable ConnectedInventory inv) {
    }

    private static final Map<Level, ConnectorIndex> LEVELS = new IdentityHashMap<>();
    private static final Set<InventorySet> PENDING_RESCAN = new ReferenceLinkedOpenHashSet<>();
    private static int safetyTicker;

    private final Long2ObjectOpenHashMap<List<Watch>> watches = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Set<InventorySet>> chunkWatches = new Long2ObjectOpenHashMap<>();
    private final Map<InventorySet, Owned> owned = new IdentityHashMap<>();

    private static final class Owned {
        final LongArrayList positions = new LongArrayList();
        final LongOpenHashSet chunks = new LongOpenHashSet();
    }

    private ConnectorIndex() {
    }

    static ConnectorIndex get(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, l -> new ConnectorIndex());
    }

    static void remove(ServerLevel level, InventorySet set) {
        PENDING_RESCAN.remove(set);
        var index = LEVELS.get(level);
        if (index != null) {
            index.unwatchAll(set);
            index.owned.remove(set);
            if (index.owned.isEmpty()) {
                LEVELS.remove(level);
            }
        }
    }

    static void queueRescan(InventorySet set) {
        PENDING_RESCAN.add(set);
    }

    private Owned owned(InventorySet set) {
        return owned.computeIfAbsent(set, s -> new Owned());
    }

    void watch(InventorySet set, BlockPos pos, Watch watch) {
        long key = pos.asLong();
        var list = watches.get(key);
        if (list == null) {
            list = new ArrayList<>(1);
            watches.put(key, list);
        }
        list.add(watch);
        owned(set).positions.add(key);
    }

    void watchChunk(InventorySet set, BlockPos pos) {
        long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        if (owned(set).chunks.add(chunk)) {
            var sets = chunkWatches.get(chunk);
            if (sets == null) {
                sets = new ReferenceOpenHashSet<>();
                chunkWatches.put(chunk, sets);
            }
            sets.add(set);
        }
    }

    void unwatchAll(InventorySet set) {
        var o = owned.get(set);
        if (o == null) {
            return;
        }
        for (int i = 0; i < o.positions.size(); i++) {
            long key = o.positions.getLong(i);
            var list = watches.get(key);
            if (list != null) {
                list.removeIf(w -> w.owner() == set);
                if (list.isEmpty()) {
                    watches.remove(key);
                }
            }
        }
        o.positions.clear();
        for (long chunk : o.chunks) {
            var sets = chunkWatches.get(chunk);
            if (sets != null) {
                sets.remove(set);
                if (sets.isEmpty()) {
                    chunkWatches.remove(chunk);
                }
            }
        }
        o.chunks.clear();
    }

    // ---------------------------------------------------------------- events

    public static void registerEvents(IEventBus bus) {
        bus.addListener(ConnectorIndex::onNeighborNotify);
        bus.addListener(ConnectorIndex::onChunkLoad);
        bus.addListener(ConnectorIndex::onServerTick);
        bus.addListener(ConnectorIndex::onLevelUnload);
        bus.addListener(ConnectorIndex::onServerStopped);
    }

    /** Called from the BlockEntity#setChanged mixin, i.e. very often: keep the miss path trivial. */
    public static void onBlockEntityChanged(@Nullable Level level, BlockPos pos) {
        if (LEVELS.isEmpty() || !(level instanceof ServerLevel serverLevel)
                || !serverLevel.getServer().isSameThread()) {
            return;
        }
        var index = LEVELS.get(level);
        if (index == null) {
            return;
        }
        var list = index.watches.get(pos.asLong());
        if (list == null) {
            return;
        }
        for (var w : list) {
            w.owner().markContentDirty(w.inv());
        }
    }

    private static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (LEVELS.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var index = LEVELS.get(level);
        if (index == null) {
            return;
        }
        var pos = event.getPos();
        var list = index.watches.get(pos.asLong());
        if (list == null) {
            return;
        }
        var state = event.getState();
        for (var w : List.copyOf(list)) {
            w.owner().onWatchedBlockChanged(w, level, pos, state);
        }
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        if (LEVELS.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var index = LEVELS.get(level);
        if (index == null) {
            return;
        }
        var sets = index.chunkWatches.get(event.getChunk().getPos().toLong());
        if (sets != null) {
            PENDING_RESCAN.addAll(sets);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (!PENDING_RESCAN.isEmpty()) {
            var batch = List.copyOf(PENDING_RESCAN);
            PENDING_RESCAN.clear();
            for (var set : batch) {
                set.rescanIfDirty();
            }
        }

        int interval = Config.SAFETY_REFRESH_TICKS.getAsInt();
        if (interval > 0 && ++safetyTicker >= interval) {
            safetyTicker = 0;
            // Only flags; the actual re-read happens lazily when AE queries the storage.
            for (var index : LEVELS.values()) {
                for (var set : index.owned.keySet()) {
                    set.markAllContentDirty();
                }
            }
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVELS.remove(level);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        LEVELS.clear();
        PENDING_RESCAN.clear();
    }
}
