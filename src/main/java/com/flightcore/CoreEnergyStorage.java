package com.flightcore;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 物品的 FE 储能实现。暴露给所有 FE 装置（充电器/线缆/蓄电池）用于充能。
 * canExtract() 返回 false：这是“飞行核心”，不是可被抽干的电池。
 */
public class CoreEnergyStorage implements IEnergyStorage {

    private final ItemStack stack;

    public CoreEnergyStorage(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }
        int stored = FlightCoreItem.getEnergy(stack);
        int accepted = Math.min(Math.min(toReceive, FlightCoreItem.MAX_RECEIVE), FlightCoreItem.CAPACITY - stored);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            FlightCoreItem.setEnergy(stack, stored + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        // 内部飞行消耗不走这里，但允许模拟查询
        int stored = FlightCoreItem.getEnergy(stack);
        return Math.min(Math.max(toExtract, 0), stored);
    }

    @Override
    public int getEnergyStored() {
        return FlightCoreItem.getEnergy(stack);
    }

    @Override
    public int getMaxEnergyStored() {
        return FlightCoreItem.CAPACITY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return FlightCoreItem.getEnergy(stack) < FlightCoreItem.CAPACITY;
    }
}
