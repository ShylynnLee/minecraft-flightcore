package com.flightcore.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class PortableBatteryStorage implements IEnergyStorage {

    private final ItemStack stack;

    public PortableBatteryStorage(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }
        int stored = PortableBatteryItem.getEnergy(stack);
        int accepted = Math.min(Math.min(toReceive, PortableBatteryItem.MAX_RECEIVE), PortableBatteryItem.CAPACITY - stored);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            PortableBatteryItem.setEnergy(stack, stored + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int stored = PortableBatteryItem.getEnergy(stack);
        return Math.min(Math.max(toExtract, 0), stored);
    }

    @Override
    public int getEnergyStored() {
        return PortableBatteryItem.getEnergy(stack);
    }

    @Override
    public int getMaxEnergyStored() {
        return PortableBatteryItem.CAPACITY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return PortableBatteryItem.getEnergy(stack) < PortableBatteryItem.CAPACITY;
    }
}
