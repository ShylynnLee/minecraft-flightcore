package com.flightcore.block;

import java.util.Map;

import com.flightcore.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 电线（方块形态，细管外观）：邻接自动连通，按类型限流传电。 */
public class WireBlock extends BaseEntityBlock {

    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;

    /** 选中体积用整方块：管子很细，用细体积会点不到、拆不掉 */
    private static final VoxelShape SELECTION = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    private final WireType type;

    public WireBlock(WireType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(defaultBlockState()
                .setValue(CONNECTIONS.get(Direction.NORTH), false)
                .setValue(CONNECTIONS.get(Direction.EAST), false)
                .setValue(CONNECTIONS.get(Direction.SOUTH), false)
                .setValue(CONNECTIONS.get(Direction.WEST), false)
                .setValue(CONNECTIONS.get(Direction.UP), false)
                .setValue(CONNECTIONS.get(Direction.DOWN), false));
    }

    public WireType getType() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // 三种电线共用同一个 codec 会丢失类型，这里用一个简单的包装
        return simpleCodec(props -> new WireBlock(type, props));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTIONS.get(Direction.NORTH), CONNECTIONS.get(Direction.EAST), CONNECTIONS.get(Direction.SOUTH),
                CONNECTIONS.get(Direction.WEST), CONNECTIONS.get(Direction.UP), CONNECTIONS.get(Direction.DOWN));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(dir), connectsTo(level, pos, dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (level instanceof Level realLevel) {
            return state.setValue(CONNECTIONS.get(direction), connectsTo(realLevel, pos, direction));
        }
        return state;
    }

    /**
     * 连接规则：只连「其他导线」和「带电的方块（机器）」。
     * 石头/木板/玻璃这类结构方块不算连接 —— 否则到处会长出竖直的短臂（"鬼影"）。
     */
    public static boolean connectsTo(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof WireBlock) {
            return true;
        }
        if (neighbor.isAir() || !neighbor.getFluidState().isEmpty()) {
            return false;
        }
        // 本模组的 FE 系统方块也算连接对象（管理模块只是统计器，没有储能能力）
        if (neighbor.getBlock() instanceof BatteryManagerBlock) {
            return true;
        }
        // 其他暴露 FE 能力的方块（发电机 / 充电器 / 蓄电池 / CC&A 机器 / 别的 FE mod）
        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                neighborPos, dir.getOpposite()) != null;
    }

    /** 把六个方向的连接状态按当前邻居重算一遍，返回修正后的状态（没变化则原样返回） */
    public static BlockState fixConnections(Level level, BlockPos pos, BlockState state) {
        BlockState fixed = state;
        for (Direction dir : Direction.values()) {
            BooleanProperty property = CONNECTIONS.get(dir);
            boolean want = connectsTo(level, pos, dir);
            if (fixed.getValue(property) != want) {
                fixed = fixed.setValue(property, want);
            }
        }
        return fixed;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SELECTION;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** 周围方块变动（放/拆线、放机器）→ 让电网立刻重建 */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WireBlockEntity wire) {
            wire.invalidateNetwork();
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WireBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.WIRE.get(),
                (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }
}
