package com.TKCCOPL.block;

import com.TKCCOPL.block.entity.MachineRedstoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;

public abstract class MachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected MachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * 邻居方块变化时重新采样红石供电状态（v1.1.7 红石链路）。
     *
     * <p>仅服务端处理；BE 持有 {@link MachineRedstoneBlockEntity} 接口时委托其红石状态采样。
     * 区块加载时的重新采样由 BE 自身在 {@code clearRemoved} / 首次 tick 时处理。</p>
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean isMoving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineRedstoneBlockEntity machine) {
            if (machine.getRedstoneState().resamplePowered(level, pos)
                    && machine instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
                // 供电状态变化，触发 BE 同步给客户端
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 2);
            }
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
    }

    /** v1.1.7 统一比较器链路：所有 MachineBlock 子类默认支持比较器输出。 */
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /**
     * v1.1.7 统一比较器三段语义（0/1-14/15）。
     *
     * <p>委托给 {@link MachineRedstoneBlockEntity#getComparatorSignal()}。
     * 不实现该接口的 BE 返回 0。</p>
     */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineRedstoneBlockEntity machine) {
            return machine.getComparatorSignal();
        }
        return 0;
    }
}

