package com.flightcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 电线方块实体：**不存电**（没有能量能力，也不写自己的电量 NBT）。
 * 它只做两件事：
 *   1. 维护/查询所属电网（坐标最小的那根线当主控）
 *   2. 由主控线每 tick 执行一次“电源端 → 负载端”的直送
 */
public class WireBlockEntity extends BlockEntity {

    private static final int REBUILD_INTERVAL = 10;

    private final int rate;
    private WireNetwork network;
    private int cooldown;

    /** 由主控线每秒写入全网：本网「负载总需求」与「实际送出」（FE/s） */
    private int demandRate;
    private int deliveredRate;

    public WireBlockEntity(BlockPos pos, BlockState state) {
        super(com.flightcore.ModBlockEntities.WIRE.get(), pos, state);
        this.rate = state.getBlock() instanceof WireBlock wire ? wire.getType().getRate() : WireType.IRON.getRate();
    }

    public int getRate() {
        return rate;
    }

    /** 连接状态自检周期（防止"上面没方块却长出连接臂"的鬼影） */
    private static final int SHAPE_CHECK_INTERVAL = 20;
    private int shapeCheck = SHAPE_CHECK_INTERVAL;

    @Override
    public void onLoad() {
        super.onLoad();
        // 区块加载后第一 tick 立刻自检一次
        shapeCheck = SHAPE_CHECK_INTERVAL;
    }

    private void verifyConnections(Level level, BlockPos pos, BlockState state) {
        BlockState fixed = WireBlock.fixConnections(level, pos, state);
        if (fixed != state) {
            level.setBlock(pos, fixed, Block.UPDATE_CLIENTS);
        }
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (++shapeCheck >= SHAPE_CHECK_INTERVAL) {
            shapeCheck = 0;
            verifyConnections(level, pos, state);
        }
        if (--cooldown <= 0 || network == null) {
            cooldown = REBUILD_INTERVAL;
            network = WireNetwork.build(level, pos);
        }
        if (!network.isController(pos)) {
            return;
        }
        WireNetwork.Result result = network.transfer(level);
        demandAccum = Math.min(demandAccum + result.demand(), Integer.MAX_VALUE / 2);
        deliveredAccum = Math.min(deliveredAccum + result.delivered(), Integer.MAX_VALUE / 2);
        if (++statTicker >= 20) {
            statTicker = 0;
            demandRate = demandAccum;
            deliveredRate = deliveredAccum;
            demandAccum = 0;
            deliveredAccum = 0;
            network.publishStats(level, demandRate, deliveredRate);
        }
    }

    private int demandAccum;
    private int deliveredAccum;
    private int statTicker;

    public void setNetworkStats(int demand, int delivered) {
        this.demandRate = demand;
        this.deliveredRate = delivered;
    }

    /** 全网负载总需求（FE/s） */
    public int getDemandRate() {
        return demandRate;
    }

    /** 全网实际送出（FE/s） */
    public int getDeliveredRate() {
        return deliveredRate;
    }

    /** 本线型号的额定载流量（FE/s） */
    public int getRatePerSecond() {
        return rate * 20;
    }

    /** 电网结构变动（放/拆线）时立即重建 */
    public void invalidateNetwork() {
        cooldown = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 电线不存电（只保存统计数字，方便诊断/显示）
        tag.putInt("Demand", demandRate);
        tag.putInt("Delivered", deliveredRate);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        demandRate = tag.getInt("Demand");
        deliveredRate = tag.getInt("Delivered");
    }
}
