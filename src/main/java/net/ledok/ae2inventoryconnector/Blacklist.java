package net.ledok.ae2inventoryconnector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks the server admin excluded in the config ({@code connectorBlacklist}), on top of the
 * {@code ae2_inventory_connector:connector_skip} tag. Entries: {@code minecraft:furnace} (block),
 * {@code #c:barrels} (block tag), {@code @sophisticatedstorage} (every block of a mod).
 */
public final class Blacklist {
    public static final Blacklist EMPTY = new Blacklist(Set.of(), List.of(), Set.of());

    private final Set<ResourceLocation> blocks;
    private final List<TagKey<Block>> tags;
    private final Set<String> mods;
    private final Map<Block, Boolean> cache = new IdentityHashMap<>();

    private Blacklist(Set<ResourceLocation> blocks, List<TagKey<Block>> tags, Set<String> mods) {
        this.blocks = blocks;
        this.tags = tags;
        this.mods = mods;
    }

    public static Blacklist parse(Iterable<? extends String> entries) {
        var blocks = new HashSet<ResourceLocation>();
        var tags = new ArrayList<TagKey<Block>>();
        var mods = new HashSet<String>();
        for (var raw : entries) {
            var entry = raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.startsWith("@")) {
                mods.add(entry.substring(1));
                continue;
            }
            boolean tag = entry.startsWith("#");
            var id = ResourceLocation.tryParse(tag ? entry.substring(1) : entry);
            if (id == null) {
                AE2InventoryConnector.LOGGER.warn("Ignoring invalid connectorBlacklist entry '{}'", entry);
            } else if (tag) {
                tags.add(TagKey.create(Registries.BLOCK, id));
            } else {
                blocks.add(id);
            }
        }
        return blocks.isEmpty() && tags.isEmpty() && mods.isEmpty() ? EMPTY : new Blacklist(blocks, tags, mods);
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public boolean contains(BlockState state) {
        if (this == EMPTY) {
            return false;
        }
        var block = state.getBlock();
        var cached = cache.get(block);
        if (cached == null) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            cached = blocks.contains(id) || mods.contains(id.getNamespace());
            for (int i = 0; !cached && i < tags.size(); i++) {
                cached = state.is(tags.get(i));
            }
            cache.put(block, cached);
        }
        return cached;
    }
}
