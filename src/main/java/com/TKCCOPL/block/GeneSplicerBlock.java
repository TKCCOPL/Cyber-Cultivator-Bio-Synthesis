package com.TKCCOPL.block;

import com.TKCCOPL.block.entity.GeneSplicerBlockEntity;
import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.item.GeneticSeedItem;
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

public class GeneSplicerBlock extends MachineBlock {
    public GeneSplicerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof GeneSplicerBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            ItemStack out = blockEntity.extractOutput();
            if (!out.isEmpty()) {
                giveToPlayer(player, out);
                sendStatus(player, "已取出拼接种子", blockEntity);
                return InteractionResult.CONSUME;
            }
            ItemStack inputOut = blockEntity.extractLastInput();
            if (!inputOut.isEmpty()) {
                giveToPlayer(player, inputOut);
                sendStatus(player, "已取回输入种子", blockEntity);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        if (held.getItem() instanceof GeneticSeedItem) {
            ItemStack one = held.copy();
            one.setCount(1);
            if (blockEntity.tryInsertSeed(one, level.getRandom())) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                sendStatus(player, "已放入种子", blockEntity);
                return InteractionResult.CONSUME;
            }
        }

        if (held.isEmpty() && blockEntity.hasOutput()) {
            ItemStack out = blockEntity.extractOutput();
            if (!out.isEmpty()) {
                giveToPlayer(player, out);
                sendStatus(player, "已取出拼接种子", blockEntity);
                return InteractionResult.CONSUME;
            }
        }

        sendStatus(player, "状态查看", blockEntity);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GeneSplicerBlockEntity blockEntity) {
            ItemStack out = blockEntity.extractOutput();
            if (!out.isEmpty()) {
                popResource(level, pos, out);
            }
            ItemStack inputOut;
            while (!(inputOut = blockEntity.extractLastInput()).isEmpty()) {
                popResource(level, pos, inputOut);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneSplicerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
    }

    private static void giveToPlayer(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static void sendStatus(Player player, String action, GeneSplicerBlockEntity blockEntity) {
        String msg = String.format("[Gene-Splicer] %s | 输入:%d 输出:%s", action, blockEntity.getInputCount(), blockEntity.hasOutput() ? "有" : "无");
        player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY), true);
    }
}
