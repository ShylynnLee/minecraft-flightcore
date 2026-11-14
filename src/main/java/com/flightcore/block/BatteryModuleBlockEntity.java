package com.flightcore.block;

import com.flightcore.energy.EnergyTransfer;
import com.flightcore.energy.FEStorage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 蓄电池模块：每块独立存 500k FE。
 * - 相邻模块之间自动均衡/溢流（从外面看就是一整套电池）
 * - 主动向相邻负载/电线供电，10,000 FE/t
 * - 自带输入/输出功率统计（供管理模块汇总）
 */
public class BatteryModuleBlockEntity extends BlockEntity {

    public static final int CAPACITY = 200_000;
    public static final int RATE = 1_200;
    private static final int BALANCE_THRESHOLD = 500;

    private final FEStorage energy = new FEStorage(CAPACITY, RATE, RATE);

    private int inputRate;
    private int outputRate;
    private int slowTicker;

    public BatteryModuleBlockEntity(BlockPos pos, BlockState state) {
        super(com.flightcore.ModBlockEntities.BATTERY_MODULE.get(), pos, state);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        boolean dirty = false;

        dirty |= balanceWithNeighbors(level, pos);

        int sent = EnergyTransfer.pushToNeighbors(level, pos, energy, RATE);
        if (sent > 0) {
            dirty = true;
        }

        if (++slowTicker >= 20) {
            slowTicker = 0;
            inputRate = energy.takeReceived();
            outputRate = energy.takeExtracted();
        }

        if (dirty) {
            setChanged();
        }
    }

    /** 相邻模块之间均衡：把富余的搬给亏空的（不算作对外输入/输出） */
    private boolean balanceWithNeighbors(Level level, BlockPos pos) {
        boolean moved = false;
        for (Direction dir : Direction.values()) {
            if (!(level.getBlockEntity(pos.relative(dir)) instanceof BatteryModuleBlockEntity other)) {
                continue;
            }
            int mine = energy.getEnergyStored();
            int theirs = other.getEnergy().getEnergyStored();
            int diff = mine - theirs;
            if (diff <= BALANCE_THRESHOLD) {
                continue;
            }
            int amount = Math.min(Math.min(RATE, diff / 2), mine);
            int taken = energy.consumeInternal(amount);
            if (taken > 0) {
                other.getEnergy().addInternal(taken);
                other.setChanged();
                moved = true;
            }
        }
        return moved;
    }

    public FEStorage getEnergy() {
        return energy;
    }

    public int getInputRate() {
        return inputRate;
    }

    public int getOutputRate() {
        return outputRate;
    }

    public void printStatus(Player player) {
        player.displayClientMessage(Component.translatable("block.flightcore.battery_module.status",
                energy.getEnergyStored(), CAPACITY, inputRate, outputRate).withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergyStored(tag.getInt("Energy"));
    }
}
