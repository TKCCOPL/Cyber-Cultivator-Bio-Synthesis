package com.TKCCOPL.block;

import com.TKCCOPL.block.entity.AtmosphericCondenserBlockEntity;
import com.TKCCOPL.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AtmosphericCondenserBlock extends MachineBlock {
    public AtmosphericCondenserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AtmosphericCondenserBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            sendStatus(player, blockEntity, "状态查看");
            return InteractionResult.CONSUME;
        }

        ItemStack out = blockEntity.extractOutput();
        if (!out.isEmpty()) {
            if (!player.addItem(out)) {
                player.drop(out, false);
            }
            sendStatus(player, blockEntity, "已取出纯净水");
            return InteractionResult.CONSUME;
        }

        sendStatus(player, blockEntity, "状态查看");
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AtmosphericCondenserBlockEntity blockEntity) {
            ItemStack out = blockEntity.extractOutput();
            if (!out.isEmpty()) {
                popResource(level, pos, out);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtmosphericCondenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.ATMOSPHERIC_CONDENSER.get(), AtmosphericCondenserBlockEntity::tick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AtmosphericCondenserBlockEntity blockEntity) {
            return blockEntity.hasOutput() ? 15 : 0;
        }
        return 0;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 2;
    }

    private static void sendStatus(Player player, AtmosphericCondenserBlockEntity blockEntity, String action) {
        String msg = String.format("[Condenser] %s | 库存:%d", action, blockEntity.getOutput().getCount());
        player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY), true);
    }
}
