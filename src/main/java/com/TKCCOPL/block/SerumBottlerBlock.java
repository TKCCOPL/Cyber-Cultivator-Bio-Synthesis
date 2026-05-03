package com.TKCCOPL.block;

import com.TKCCOPL.block.entity.SerumBottlerBlockEntity;
import com.TKCCOPL.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SerumBottlerBlock extends MachineBlock {
    public SerumBottlerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SerumBottlerBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Shift-right-click: extract output or view status
        if (player.isShiftKeyDown()) {
            ItemStack out = blockEntity.extractOutput();
            if (!out.isEmpty()) {
                giveToPlayer(player, out);
                sendStatus(player, blockEntity, "已取出血清");
                return InteractionResult.CONSUME;
            }
            sendStatus(player, blockEntity, "状态查看");
            return InteractionResult.CONSUME;
        }

        // Right-click with item: try to insert into input slots
        if (!held.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                if (blockEntity.getItem(i).isEmpty()) {
                    ItemStack one = held.copy();
                    one.setCount(1);
                    blockEntity.setItem(i, one);
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    sendStatus(player, blockEntity, "已放入材料");
                    return InteractionResult.CONSUME;
                }
            }
            sendStatus(player, blockEntity, "输入槽已满");
            return InteractionResult.CONSUME;
        }

        // Empty hand: extract output first, then input materials
        ItemStack out = blockEntity.extractOutput();
        if (!out.isEmpty()) {
            giveToPlayer(player, out);
            sendStatus(player, blockEntity, "已取出血清");
            return InteractionResult.CONSUME;
        }

        ItemStack input = blockEntity.extractLastInput();
        if (!input.isEmpty()) {
            giveToPlayer(player, input);
            sendStatus(player, blockEntity, "已取回材料");
            return InteractionResult.CONSUME;
        }

        sendStatus(player, blockEntity, "状态查看");
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SerumBottlerBlockEntity blockEntity) {
            for (int i = 0; i < 3; i++) {
                ItemStack input = blockEntity.getItem(i);
                if (!input.isEmpty()) {
                    popResource(level, pos, input);
                }
            }
            ItemStack out = blockEntity.getOutput();
            if (!out.isEmpty()) {
                popResource(level, pos, out);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SerumBottlerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.SERUM_BOTTLER.get(), SerumBottlerBlockEntity::tick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SerumBottlerBlockEntity blockEntity) {
            if (blockEntity.getMaxProgress() > 0) {
                return (int) (15.0 * blockEntity.getProgress() / blockEntity.getMaxProgress());
            }
            return blockEntity.getOutput().isEmpty() ? 0 : 15;
        }
        return 0;
    }

    private static void giveToPlayer(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static void sendStatus(Player player, SerumBottlerBlockEntity blockEntity, String action) {
        String progress = blockEntity.getMaxProgress() > 0
                ? String.format("加工中 %d%%", (int) (100.0 * blockEntity.getProgress() / blockEntity.getMaxProgress()))
                : "待机";
        String msg = String.format("[Bottler] %s | %s | 输出:%s", action, progress, blockEntity.getOutput().isEmpty() ? "无" : "有");
        player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY), true);
    }
}
