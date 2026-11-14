package com.flightcore.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * FE 传输工具（参考 Create Crafts & Additions 的 outputToSide：主动推给相邻方块的标准 FE capability，
 * 没被接收的部分退回，避免凭空丢电）。
 */
public final class EnergyTransfer {

    private EnergyTransfer() {}

    // ---------------------------------------------------------------- 推电

    /** 向指定方向推电 */
    public static int pushTo(Level level, BlockPos pos, Direction dir, FEStorage source, int maxAmount) {
        if (maxAmount <= 0 || source.getEnergyStored() <= 0) {
            return 0;
        }
        BlockPos target = pos.relative(dir);
        IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, dir.getOpposite());
        if (neighbor == null || !neighbor.canReceive()) {
            return 0;
        }
        int extracted = source.extractEnergy(maxAmount, true);
        if (extracted <= 0) {
            return 0;
        }
        int accepted = neighbor.receiveEnergy(extracted, true);
        if (accepted <= 0) {
            return 0;
        }
        int actuallyExtracted = source.extractEnergy(accepted, false);
        int actuallyAccepted = neighbor.receiveEnergy(actuallyExtracted, false);
        if (actuallyAccepted < actuallyExtracted) {
            source.addInternal(actuallyExtracted - actuallyAccepted);
        }
        return actuallyAccepted;
    }

    /**
     * 按总预算向六个方向推电（“贴贴供电”）。
     *
     * @param budget  本 tick 总预算
     * @param exclude 排除的方向（长度 6，按 Direction.ordinal()），可为 null
     */
    public static int pushToNeighbors(Level level, BlockPos pos, FEStorage source, int budget, boolean[] exclude) {
        int sent = 0;
        for (Direction dir : Direction.values()) {
            if (sent >= budget) {
                break;
            }
            if (exclude != null && exclude[dir.ordinal()]) {
                continue;
            }
            sent += pushTo(level, pos, dir, source, budget - sent);
        }
        return sent;
    }

    public static int pushToNeighbors(Level level, BlockPos pos, FEStorage source, int budget) {
        return pushToNeighbors(level, pos, source, budget, null);
    }

    // ---------------------------------------------------------------- 抽电

    /** 从指定方向抽电 */
    public static int pullFrom(Level level, BlockPos pos, Direction dir, FEStorage target, int maxAmount) {
        int space = target.getFreeSpace();
        int want = Math.min(maxAmount, space);
        if (want <= 0) {
            return 0;
        }
        BlockPos srcPos = pos.relative(dir);
        IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, srcPos, dir.getOpposite());
        if (neighbor == null || !neighbor.canExtract()) {
            return 0;
        }
        int available = neighbor.extractEnergy(want, true);
        if (available <= 0) {
            return 0;
        }
        int actuallyTaken = neighbor.extractEnergy(available, false);
        int accepted = target.receiveEnergy(actuallyTaken, false);
        if (accepted < actuallyTaken) {
            neighbor.receiveEnergy(actuallyTaken - accepted, false);
        }
        return accepted;
    }

    /** 按总预算从六个方向抽电 */
    public static int pullFromNeighbors(Level level, BlockPos pos, FEStorage target, int budget) {
        int got = 0;
        for (Direction dir : Direction.values()) {
            if (got >= budget) {
                break;
            }
            got += pullFrom(level, pos, dir, target, budget - got);
        }
        return got;
    }

    // ---------------------------------------------------------------- 物品

    /** 把电充进物品 */
    public static int chargeItem(ItemStack stack, int amount) {
        if (stack.isEmpty()) {
            return 0;
        }
        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (itemEnergy == null || !itemEnergy.canReceive()) {
            return 0;
        }
        return itemEnergy.receiveEnergy(amount, false);
    }

    public static int getItemEnergy(ItemStack stack) {
        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return itemEnergy == null ? 0 : itemEnergy.getEnergyStored();
    }

    public static int getItemCapacity(ItemStack stack) {
        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return itemEnergy == null ? 0 : itemEnergy.getMaxEnergyStored();
    }

    public static boolean isItemFull(ItemStack stack) {
        IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return itemEnergy == null || itemEnergy.getEnergyStored() >= itemEnergy.getMaxEnergyStored();
    }
}
