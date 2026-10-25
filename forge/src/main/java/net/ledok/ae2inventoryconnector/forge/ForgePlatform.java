package net.ledok.ae2inventoryconnector.forge;

import java.nio.file.Path;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.loading.FMLPaths;

import net.ledok.ae2inventoryconnector.platform.Platform;
import net.ledok.ae2inventoryconnector.platform.PlatformInventory;

public final class ForgePlatform implements Platform {
    @Override
    public boolean hasItemStorage(ServerLevel level, BlockPos pos, BlockState state, Direction side) {
        if (!state.hasBlockEntity()) {
            return false;
        }
        var be = level.getBlockEntity(pos);
        return be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent();
    }

    @Override
    public PlatformInventory openItemStorage(ServerLevel level, BlockPos pos, Direction side,
            Runnable onInvalidated) {
        return new ForgeInventory(level, pos, side, onInvalidated);
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
