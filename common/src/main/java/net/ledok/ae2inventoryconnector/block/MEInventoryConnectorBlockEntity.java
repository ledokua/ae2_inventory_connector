package net.ledok.ae2inventoryconnector.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.util.AECableType;

import net.ledok.ae2inventoryconnector.AE2InventoryConnector;
import net.ledok.ae2inventoryconnector.Config;
import net.ledok.ae2inventoryconnector.storage.ConnectorStorage;
import net.ledok.ae2inventoryconnector.storage.InventorySet;

/**
 * Connects to AE cables like any other device (needs a channel) and mounts the inventories around it, found the same
 * way Tom's Storage inventory connector finds them, directly as ME storage. It does not tick.
 * <p>
 * AE2 finds the grid node because this class implements {@link IInWorldGridNodeHost} (both loaders check that
 * before their capability/lookup), so no registration is needed.
 */
public class MEInventoryConnectorBlockEntity extends BlockEntity implements IInWorldGridNodeHost, IStorageProvider {
    private final InventorySet inventories = new InventorySet(this);
    private final ConnectorStorage storage = new ConnectorStorage(inventories);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, NodeListener.INSTANCE)
            .setFlags(GridFlags.REQUIRE_CHANNEL)
            .setInWorldNode(true)
            .setTagName("proxy")
            .addService(IStorageProvider.class, this);
    private boolean nodeCreated;
    private boolean wasActive;

    public MEInventoryConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(AE2InventoryConnector.connectorBlockEntity(), pos, state);
    }

    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    public InventorySet getInventories() {
        return inventories;
    }

    /**
     * The block entity joined a server level. Forge calls this itself (IForgeBlockEntity#onLoad); the Fabric glue
     * calls it from ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.
     */
    public void onLoad() {
        if (level != null && !level.isClientSide) {
            GridHelper.onFirstTick(this, MEInventoryConnectorBlockEntity::onReady);
        }
    }

    private void onReady() {
        if (nodeCreated || isRemoved()) {
            return;
        }
        nodeCreated = true;
        mainNode.setIdlePowerUsage(Config.idlePowerUsage);
        mainNode.setVisualRepresentation(AE2InventoryConnector.connectorItem());
        mainNode.create(level, worldPosition);
        inventories.markTopologyDirty();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        shutdown();
    }

    /**
     * The chunk unloaded. Forge calls this itself (IForgeBlockEntity#onChunkUnloaded); the Fabric glue calls it
     * from ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.
     */
    public void onChunkUnloaded() {
        shutdown();
    }

    private void shutdown() {
        mainNode.destroy();
        nodeCreated = false;
        inventories.dispose();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mainNode.loadFromNBT(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        mainNode.saveToNBT(tag);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        if (mainNode.isActive()) {
            mounts.mount(storage, 0);
        }
    }

    private void onNodeStateChanged() {
        boolean active = mainNode.isActive();
        if (active != wasActive) {
            wasActive = active;
            IStorageProvider.requestUpdate(mainNode);
        }
    }

    private enum NodeListener implements IGridNodeListener<MEInventoryConnectorBlockEntity> {
        INSTANCE;

        @Override
        public void onSaveChanges(MEInventoryConnectorBlockEntity be, IGridNode node) {
            be.setChanged();
        }

        @Override
        public void onStateChanged(MEInventoryConnectorBlockEntity be, IGridNode node, State state) {
            be.onNodeStateChanged();
        }

        @Override
        public void onGridChanged(MEInventoryConnectorBlockEntity be, IGridNode node) {
            be.onNodeStateChanged();
        }
    }
}
