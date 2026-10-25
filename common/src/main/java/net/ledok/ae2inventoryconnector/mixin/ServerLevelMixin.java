package net.ledok.ae2inventoryconnector.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import net.ledok.ae2inventoryconnector.storage.ConnectorIndex;

/**
 * Block change notification. Every setBlock that updates neighbors (placing, breaking, state changes, pistons)
 * reaches {@code ServerLevel#updateNeighborsAt} through {@code blockUpdated}; it is where Forge fires its
 * neighbor-notify event, so one hook serves both loaders. ({@code ServerLevel} overrides the {@code Level} method
 * without calling super, hence the target.)
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V", at = @At("HEAD"))
    private void ae2ic$onUpdateNeighbors(BlockPos pos, Block block, CallbackInfo ci) {
        ConnectorIndex.onBlockUpdated((ServerLevel) (Object) this, pos);
    }
}
