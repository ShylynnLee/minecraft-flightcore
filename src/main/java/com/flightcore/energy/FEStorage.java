package com.flightcore.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * 带“进/出计数”的 FE 储能，用于蓄电池管理模块统计输入/输出功率。
 */
public class FEStorage extends EnergyStorage {

    /** 累计接收（用于统计窗口内输入） */
    private int received = 0;
    /** 累计抽取（用于统计窗口内输出） */
    private int extracted = 0;

    public FEStorage(int capacity) {
        this(capacity, capacity, capacity);
    }

    public FEStorage(int capacity, int maxTransfer) {
        this(capacity, maxTransfer, maxTransfer);
    }

    public FEStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract, 0);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int accepted = super.receiveEnergy(toReceive, simulate);
        if (!simulate) {
            received += accepted;
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int taken = super.extractEnergy(toExtract, simulate);
        if (!simulate) {
            extracted += taken;
        }
        return taken;
    }

    /** 内部直接注入（发电机自产电），不计入“外部输入”统计 */
    public void addInternal(int amount) {
        energy = Math.min(capacity, Math.max(0, energy + amount));
    }

    /** 内部直接消耗（机器自己用电），不计入“外部输出”统计 */
    public int consumeInternal(int amount) {
        int taken = Math.min(energy, Math.max(0, amount));
        energy -= taken;
        return taken;
    }

    public void setEnergyStored(int value) {
        energy = Math.min(capacity, Math.max(0, value));
    }

    /** 读取并清空统计计数（每个统计周期调用一次） */
    public int takeReceived() {
        int v = received;
        received = 0;
        return v;
    }

    public int takeExtracted() {
        int v = extracted;
        extracted = 0;
        return v;
    }

    public int getFreeSpace() {
        return capacity - energy;
    }
}
