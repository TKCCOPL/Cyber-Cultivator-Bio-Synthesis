package com.TKCCOPL.block;

import com.TKCCOPL.Config;
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
                sendMachineStatus(player, blockEntity, Component.translatable("message.cybercultivator.incubator.seed_extracted"));
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.PURIFIED_WATER_BOTTLE.get())) {
            blockEntity.addPurity(Config.purityInjectAmount);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.addItem(bottle)) {
                    player.drop(bottle, false);
                }
            }
            sendMachineStatus(player, blockEntity, Component.translatable("message.cybercultivator.incubator.inject_purity", Config.purityInjectAmount));
            return InteractionResult.CONSUME;
        }

        if (held.is(ModItems.BIOCHEMICAL_SOLUTION.get())) {
            blockEntity.addNutrition(Config.nutritionInjectAmount);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            sendMachineStatus(player, blockEntity, Component.translatable("message.cybercultivator.incubator.inject_nutrition", Config.nutritionInjectAmount));
            return InteractionResult.CONSUME;
        }

        if (held.is(ModItems.SILICON_SHARD.get())) {
            blockEntity.addDataSignal(Config.dataSignalInjectAmount);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            sendMachineStatus(player, blockEntity, Component.translatable("message.cybercultivator.incubator.inject_signal", Config.dataSignalInjectAmount));
            return InteractionResult.CONSUME;
        }

        if (held.getItem() instanceof GeneticSeedItem && !blockEntity.hasSeed()) {
            ItemStack seed = held.copy();
            seed.setCount(1);
            if (blockEntity.tryInsertSeed(seed)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                sendMachineStatus(player, blockEntity, Component.translatable("message.cybercultivator.incubator.seed_inserted"));
                return InteractionResult.CONSUME;
            }
        }

        sendMachineStatus(player, blockEntity, Component.translatable("message.cybercultivator.incubator.inspect"));
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

    private static void sendMachineStatus(Player player, BioIncubatorBlockEntity blockEntity, Component action) {
        Component message = Component.translatable("message.cybercultivator.incubator.status", action,
                blockEntity.getNutrition(), blockEntity.getPurity(), blockEntity.getDataSignal());

        if (blockEntity.hasSeed()) {
            int percent = blockEntity.getGrowthPercent();
            int eta = blockEntity.getEstimatedSecondsRemaining();
            message = message.copy().append(Component.translatable("message.cybercultivator.incubator.growth", percent));
            if (eta >= 0) {
                message = message.copy().append(Component.translatable("message.cybercultivator.incubator.eta", eta));
            } else {
                message = message.copy().append(Component.translatable("message.cybercultivator.incubator.insufficient"));
            }
        }

        player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
    }
}
