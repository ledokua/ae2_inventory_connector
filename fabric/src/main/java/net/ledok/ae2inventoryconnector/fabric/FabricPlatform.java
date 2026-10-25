package net.ledok.ae2inventoryconnector.fabric;

import java.nio.file.Path;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import net.ledok.ae2inventoryconnector.platform.Platform;
import net.ledok.ae2inventoryconnector.platform.PlatformInventory;

public final class FabricPlatform implements Platform {
    @Override
    public boolean hasItemStorage(ServerLevel level, BlockPos pos, BlockState state, Direction side) {
        return ItemStorage.SIDED.find(level, pos, state, null, side) != null;
    }

    @Override
    public PlatformInventory openItemStorage(ServerLevel level, BlockPos pos, Direction side,
            Runnable onInvalidated) {
        // Fabric has no invalidation callback; block changes and chunk unloads are reported by the common hooks.
        return new FabricInventory(BlockApiCache.create(ItemStorage.SIDED, level, pos), side);
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
