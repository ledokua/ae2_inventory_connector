package net.ledok.ae2inventoryconnector.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import net.ledok.ae2inventoryconnector.AE2InventoryConnector;
import net.ledok.ae2inventoryconnector.block.MEInventoryConnectorBlockEntity;
import net.ledok.ae2inventoryconnector.storage.ConnectorIndex;

public final class AE2InventoryConnectorFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        var id = AE2InventoryConnector.CONNECTOR_ID;
        var block = Registry.register(BuiltInRegistries.BLOCK, id, AE2InventoryConnector.createConnectorBlock());
        var item = Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
        var type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.create(MEInventoryConnectorBlockEntity::new, block).build());
        AE2InventoryConnector.bind(block, item, type);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> e.accept(item));

        // Fabric has no BlockEntity#onLoad/onChunkUnloaded; these events are the equivalent.
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
            if (be instanceof MEInventoryConnectorBlockEntity connector) {
                connector.onLoad();
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, level) -> {
            if (be instanceof MEInventoryConnectorBlockEntity connector) {
                connector.onChunkUnloaded();
            }
        });

        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> ConnectorIndex.onChunkChanged(level, chunk.getPos()));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> ConnectorIndex.onChunkChanged(level, chunk.getPos()));
        ServerTickEvents.END_SERVER_TICK.register(server -> ConnectorIndex.onServerTickEnd());
        ServerWorldEvents.UNLOAD.register((server, level) -> ConnectorIndex.onLevelUnload(level));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> AE2InventoryConnector.onServerStarting());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> AE2InventoryConnector.onServerStopped());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) {
                AE2InventoryConnector.onDataReloaded();
            }
        });
    }
}
