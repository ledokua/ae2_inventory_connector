package net.ledok.moderntech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.ledok.moderntech.storage.ConnectorIndex;

/**
 * Inventory change notification. The static overload is what both {@code BlockEntity#setChanged()} and hoppers call,
 * so hooking it catches nearly every inventory modification without polling.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Inject(method = "setChanged(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"))
    private static void modernTech$onSetChanged(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        ConnectorIndex.onBlockEntityChanged(level, pos);
    }
}
