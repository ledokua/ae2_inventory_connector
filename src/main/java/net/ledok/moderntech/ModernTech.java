package net.ledok.moderntech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.api.AECapabilities;

import net.ledok.moderntech.block.MEInventoryConnectorBlock;
import net.ledok.moderntech.block.MEInventoryConnectorBlockEntity;
import net.ledok.moderntech.storage.ConnectorIndex;

@Mod(ModernTech.MOD_ID)
public class ModernTech {
    public static final String MOD_ID = "modern_tech";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<MEInventoryConnectorBlock> ME_INVENTORY_CONNECTOR = BLOCKS.register(
            "me_inventory_connector",
            () -> new MEInventoryConnectorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .strength(2.2f, 11f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> ME_INVENTORY_CONNECTOR_ITEM = ITEMS.registerSimpleBlockItem(
            ME_INVENTORY_CONNECTOR, new Item.Properties());

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MEInventoryConnectorBlockEntity>> ME_INVENTORY_CONNECTOR_BE = BLOCK_ENTITIES
            .register("me_inventory_connector", () -> BlockEntityType.Builder
                    .of(MEInventoryConnectorBlockEntity::new, ME_INVENTORY_CONNECTOR.get())
                    .build(null));

    public ModernTech(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        modBus.addListener(ModernTech::registerCapabilities);
        modBus.addListener(ModernTech::addCreative);
        ConnectorIndex.registerEvents(NeoForge.EVENT_BUS);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ME_INVENTORY_CONNECTOR_BE.get(),
                (be, context) -> be);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ME_INVENTORY_CONNECTOR_ITEM);
        }
    }
}
