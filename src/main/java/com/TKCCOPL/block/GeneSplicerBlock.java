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
                sendStatus(player, Component.translatable("message.cybercultivator.splicer.output_extracted"), blockEntity);
                return InteractionResult.CONSUME;
            }
            ItemStack inputOut = blockEntity.extractLastInput();
            if (!inputOut.isEmpty()) {
                giveToPlayer(player, inputOut);
                sendStatus(player, Component.translatable("message.cybercultivator.splicer.input_retrieved"), blockEntity);
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
                sendStatus(player, Component.translatable("message.cybercultivator.splicer.seed_inserted"), blockEntity);
                return InteractionResult.CONSUME;
            }
        }

        if (held.isEmpty() && blockEntity.hasOutput()) {
            ItemStack out = blockEntity.extractOutput();
            if (!out.isEmpty()) {
                giveToPlayer(player, out);
                sendStatus(player, Component.translatable("message.cybercultivator.splicer.output_extracted"), blockEntity);
                return InteractionResult.CONSUME;
            }
        }

        sendStatus(player, Component.translatable("message.cybercultivator.splicer.inspect"), blockEntity);
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

    private static void sendStatus(Player player, Component action, GeneSplicerBlockEntity blockEntity) {
        Component output = Component.translatable(blockEntity.hasOutput()
                ? "message.cybercultivator.state.yes"
                : "message.cybercultivator.state.no");
        player.displayClientMessage(Component.translatable("message.cybercultivator.splicer.status",
                action, blockEntity.getInputCount(), output).withStyle(ChatFormatting.GRAY), true);
    }
}
