package com.TKCCOPL.block;

import com.TKCCOPL.advancement.ModTriggers;
import com.TKCCOPL.block.entity.GeneSplicerBlockEntity;
import com.TKCCOPL.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.NetworkHooks;
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

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            ItemStack out = blockEntity.extractOutput();
            if (!out.isEmpty()) {
                // 潜行右键领取子代也必须触发进度，与 GUI onTake 路径行为一致；
                // 漏斗自动抽取不走此路径，不会错误获得玩家进度。
                ModTriggers.triggerForOutput(player, out);
                giveToPlayer(player, out);
                player.displayClientMessage(Component.translatable(
                        "message.cybercultivator.splicer.output_extracted"), true);
                return InteractionResult.CONSUME;
            }
            ItemStack inputOut = blockEntity.extractLastInput();
            if (!inputOut.isEmpty()) {
                giveToPlayer(player, inputOut);
                player.displayClientMessage(Component.translatable(
                        "message.cybercultivator.splicer.input_retrieved"), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        NetworkHooks.openScreen((ServerPlayer) player, blockEntity, pos);
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
        return createTickerHelper(blockEntityType, ModBlockEntities.GENE_SPLICER.get(), GeneSplicerBlockEntity::tick);
    }

    private static void giveToPlayer(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

}
