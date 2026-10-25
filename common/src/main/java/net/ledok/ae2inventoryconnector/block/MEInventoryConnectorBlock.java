package net.ledok.ae2inventoryconnector.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MEInventoryConnectorBlock extends BaseEntityBlock {
    public MEInventoryConnectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MEInventoryConnectorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof MEInventoryConnectorBlockEntity be) {
            be.getMainNode().setOwningPlayer(player);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MEInventoryConnectorBlockEntity be) {
            // Right-click also rescans: a manual fix for blocks changed without block updates (e.g. WorldEdit).
            be.getInventories().markTopologyDirty();
            var u = be.getInventories().usage();
            var key = be.getMainNode().isActive()
                    ? "chat.ae2_inventory_connector.me_inventory_connector.usage"
                    : "chat.ae2_inventory_connector.me_inventory_connector.usage_offline";
            player.displayClientMessage(Component.translatable(key, u.usedSlots(), u.slots(), u.inventories(),
                    u.itemTypes()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ae2_inventory_connector.me_inventory_connector")
                .withStyle(ChatFormatting.GRAY));
    }
}
