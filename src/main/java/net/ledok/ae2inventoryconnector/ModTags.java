package net.ledok.ae2inventoryconnector;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Blocks the scan passes through without being inventories themselves. */
    public static final TagKey<Block> TRIMS = BlockTags.create(AE2InventoryConnector.id("trims"));
    /** Blocks never treated as inventories (proxies, other connectors, AE devices...). */
    public static final TagKey<Block> CONNECTOR_SKIP = BlockTags.create(AE2InventoryConnector.id("connector_skip"));

    private ModTags() {
    }
}
