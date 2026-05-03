package com.TKCCOPL.block;

import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BioIncubatorBlock extends MachineBlock {
    public BioIncubatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BioIncubatorBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            ItemStack seedOut = blockEntity.extractSeed();
            if (!seedOut.isEmpty()) {
                if (!player.addItem(seedOut)) {
                    player.drop(seedOut, false);
                }
                player.displayClientMessage(Component.literal("[Bio-Incubator] 已取出种子").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (held.is(Items.WATER_BUCKET)) {
            blockEntity.addPurity(20);
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            sendMachineStatus(player, blockEntity, "注入纯净水 +20");
            return InteractionResult.CONSUME;
        }

        if (held.is(ModItems.BIOCHEMICAL_SOLUTION.get())) {
            blockEntity.addNutrition(25);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            sendMachineStatus(player, blockEntity, "注入营养液 +25");
            return InteractionResult.CONSUME;
        }

        if (held.is(ModItems.SILICON_SHARD.get())) {
            blockEntity.addDataSignal(15);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            sendMachineStatus(player, blockEntity, "注入数据信号 +15");
            return InteractionResult.CONSUME;
        }

        if (held.getItem() instanceof GeneticSeedItem && !blockEntity.hasSeed()) {
            ItemStack seed = held.copy();
            seed.setCount(1);
            if (blockEntity.tryInsertSeed(seed)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                sendMachineStatus(player, blockEntity, "已放入种子");
                return InteractionResult.CONSUME;
            }
        }

        sendMachineStatus(player, blockEntity, "状态查看");
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BioIncubatorBlockEntity blockEntity) {
            ItemStack seed = blockEntity.extractSeed();
            if (!seed.isEmpty()) {
                popResource(level, pos, seed);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BioIncubatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.BIO_INCUBATOR.get(), BioIncubatorBlockEntity::tick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BioIncubatorBlockEntity blockEntity) {
            return Math.min(15, blockEntity.getNutrition() / 7);
        }
        return 0;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 2;
    }

    private static void sendMachineStatus(Player player, BioIncubatorBlockEntity blockEntity, String action) {
        StringBuilder msg = new StringBuilder();
        msg.append(String.format("[Bio-Incubator] %s | N:%d P:%d D:%d", action, blockEntity.getNutrition(), blockEntity.getPurity(), blockEntity.getDataSignal()));

        if (blockEntity.hasSeed()) {
            int percent = blockEntity.getGrowthPercent();
            int eta = blockEntity.getEstimatedSecondsRemaining();
            msg.append(String.format(" | 生长: %d%%", percent));
            if (eta >= 0) {
                msg.append(String.format(" (约%ds)", eta));
            } else {
                msg.append(" (资源不足)");
            }
        }

        player.displayClientMessage(Component.literal(msg.toString()).withStyle(ChatFormatting.GRAY), true);
    }
}
