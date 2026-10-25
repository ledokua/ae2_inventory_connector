package net.ledok.ae2inventoryconnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.ledok.ae2inventoryconnector.block.MEInventoryConnectorBlock;
import net.ledok.ae2inventoryconnector.block.MEInventoryConnectorBlockEntity;
import net.ledok.ae2inventoryconnector.platform.Platform;
import net.ledok.ae2inventoryconnector.storage.ConnectorIndex;

/**
 * Common entry point. The loader glue constructs and registers the content (Forge only allows that inside its
 * registry events) and hands the results back through {@link #bind}.
 */
public final class AE2InventoryConnector {
    public static final String MOD_ID = "ae2_inventory_connector";
    public static final Logger LOGGER = LoggerFactory.getLogger("AE2 Inventory Connector");
    public static final ResourceLocation CONNECTOR_ID = id("me_inventory_connector");

    private static Block connectorBlock;
    private static Item connectorItem;
    private static BlockEntityType<MEInventoryConnectorBlockEntity> connectorBlockEntity;

    private AE2InventoryConnector() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static Block createConnectorBlock() {
        return new MEInventoryConnectorBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(2.2f, 11f)
                .requiresCorrectToolForDrops());
    }

    public static void bind(Block block, Item item, BlockEntityType<MEInventoryConnectorBlockEntity> blockEntity) {
        connectorBlock = block;
        connectorItem = item;
        connectorBlockEntity = blockEntity;
    }

    public static Block connectorBlock() {
        return connectorBlock;
    }

    public static Item connectorItem() {
        return connectorItem;
    }

    public static BlockEntityType<MEInventoryConnectorBlockEntity> connectorBlockEntity() {
        return connectorBlockEntity;
    }

    public static void onServerStarting() {
        Config.load(Platform.INSTANCE.configDir());
    }

    /** {@code /reload} finished: re-read the config and rescan, since the blacklist and tags may have changed. */
    public static void onDataReloaded() {
        Config.load(Platform.INSTANCE.configDir());
        ConnectorIndex.rescanAll();
    }

    public static void onServerStopped() {
        ConnectorIndex.clear();
    }
}
