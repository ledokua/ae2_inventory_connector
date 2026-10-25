package net.ledok.ae2inventoryconnector.forge;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

import net.ledok.ae2inventoryconnector.AE2InventoryConnector;
import net.ledok.ae2inventoryconnector.block.MEInventoryConnectorBlockEntity;
import net.ledok.ae2inventoryconnector.storage.ConnectorIndex;

@Mod(AE2InventoryConnector.MOD_ID)
public final class AE2InventoryConnectorForge {
    private static Block block;
    private static Item item;

    public AE2InventoryConnectorForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(AE2InventoryConnectorForge::register);
        modBus.addListener(AE2InventoryConnectorForge::addCreative);

        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener((ChunkEvent.Load e) -> ConnectorIndex.onChunkChanged(levelOf(e), e.getChunk().getPos()));
        bus.addListener((ChunkEvent.Unload e) -> ConnectorIndex.onChunkChanged(levelOf(e), e.getChunk().getPos()));
        bus.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                ConnectorIndex.onServerTickEnd();
            }
        });
        bus.addListener((LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof net.minecraft.world.level.Level level) {
                ConnectorIndex.onLevelUnload(level);
            }
        });
        bus.addListener((ServerStartingEvent e) -> AE2InventoryConnector.onServerStarting());
        bus.addListener((ServerStoppedEvent e) -> AE2InventoryConnector.onServerStopped());
        // Posted at the end of /reload with no player (joins carry a player).
        bus.addListener((OnDatapackSyncEvent e) -> {
            if (e.getPlayer() == null) {
                AE2InventoryConnector.onDataReloaded();
            }
        });
    }

    private static net.minecraft.world.level.Level levelOf(ChunkEvent e) {
        return e.getLevel() instanceof net.minecraft.world.level.Level level ? level : null;
    }

    private static void register(RegisterEvent event) {
        var id = AE2InventoryConnector.CONNECTOR_ID;
        event.register(Registries.BLOCK, helper -> {
            block = AE2InventoryConnector.createConnectorBlock();
            helper.register(id, block);
        });
        event.register(Registries.ITEM, helper -> {
            item = new BlockItem(block, new Item.Properties());
            helper.register(id, item);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            @SuppressWarnings("DataFlowIssue")
            var type = BlockEntityType.Builder.of(MEInventoryConnectorBlockEntity::new, block).build(null);
            helper.register(id, type);
            AE2InventoryConnector.bind(block, item, type);
        });
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(item);
        }
    }
}
