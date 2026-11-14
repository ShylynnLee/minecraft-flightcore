package com.flightcore.block;

import java.util.List;

import com.flightcore.energy.EnergyTransfer;
import com.flightcore.energy.FEStorage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 充电器：把可充电物品“丢进去 / 放到方块上方”就会自动充电，充满后弹出。
 * 电力来源：自动从相邻方块抽电（也能被外部推电）。
 */
public class ChargerBlockEntity extends BlockEntity {

    public static final int CAPACITY = 30_000;
    public static final int MAX_IN = 12_000;
    public static final int PULL_PER_SIDE = 12_000;
    public static final int CHARGE_RATE = 1_200;

    private final FEStorage energy = new FEStorage(CAPACITY, MAX_IN, 0);

    /** 本统计窗口累计（1 秒 = 20 tick） */
    private int demandAccum;
    private int deliveredAccum;
    private int statTicker;
    /** 对外展示：本秒「想要多少」与「实际给到多少」 */
    private int demandRate;
    private int deliveredRate;
    private final ItemStackHandler slot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int index, ItemStack stack) {
            return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
        }
    };

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(com.flightcore.ModBlockEntities.CHARGER.get(), pos, state);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        boolean dirty = false;

        // 1) 从相邻方块抽电
        int got = EnergyTransfer.pullFromNeighbors(level, pos, energy, PULL_PER_SIDE);
        if (got > 0) {
            dirty = true;
        }

        // 2) 吸收上方/内部的掉落物（“丢进去”）
        if (slot.getStackInSlot(0).isEmpty()) {
            if (absorb(level, pos)) {
                dirty = true;
            }
        }

        // 3) 给物品充电：有多少电就干多少活（速率 = min(上限, 缓冲可用)）
        ItemStack stack = slot.getStackInSlot(0);
        if (!stack.isEmpty()) {
            int space = Math.max(0, EnergyTransfer.getItemCapacity(stack) - EnergyTransfer.getItemEnergy(stack));
            int demand = Math.min(CHARGE_RATE, space);                 // 本 tick「想要」
            int accepted = 0;
            if (demand > 0 && energy.getEnergyStored() > 0) {
                accepted = EnergyTransfer.chargeItem(stack, Math.min(demand, energy.getEnergyStored()));
                if (accepted > 0) {
                    energy.consumeInternal(accepted);
                    dirty = true;
                }
            }
            demandAccum += demand;
            deliveredAccum += accepted;
            // 充满 → 弹出
            if (EnergyTransfer.isItemFull(stack)) {
                eject(level, pos);
                dirty = true;
            }
        }

        if (++statTicker >= 20) {
            statTicker = 0;
            demandRate = demandAccum;
            deliveredRate = deliveredAccum;
            demandAccum = 0;
            deliveredAccum = 0;
        }

        if (dirty) {
            setChanged();
        }
    }

    private boolean absorb(Level level, BlockPos pos) {
        AABB area = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);
        for (ItemEntity entity : items) {
            // 刚丢出来的先给玩家一点时间捡回去
            if (entity.tickCount < 20 || entity.getItem().isEmpty()) {
                continue;
            }
            ItemStack one = entity.getItem().copyWithCount(1);
            if (one.getCapability(Capabilities.EnergyStorage.ITEM) == null) {
                continue;
            }
            ItemStack rest = slot.insertItem(0, one, false);
            if (rest.isEmpty()) {
                entity.getItem().shrink(1);
                if (entity.getItem().isEmpty()) {
                    entity.discard();
                }
                return true;
            }
        }
        return false;
    }

    private void eject(Level level, BlockPos pos) {
        ItemStack stack = slot.extractItem(0, 1, false);
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, stack);
        entity.setDeltaMovement(0, 0.1, 0);
        level.addFreshEntity(entity);
    }

    public FEStorage getEnergy() {
        return energy;
    }

    public int getDemandRate() {
        return demandRate;
    }

    public int getDeliveredRate() {
        return deliveredRate;
    }

    public ItemStackHandler getSlotHandler() {
        return slot;
    }

    public boolean insert(ItemStack stack) {
        if (!slot.getStackInSlot(0).isEmpty()) {
            return false;
        }
        ItemStack one = stack.copyWithCount(1);
        ItemStack rest = slot.insertItem(0, one, false);
        if (rest.isEmpty()) {
            stack.shrink(1);
            setChanged();
            return true;
        }
        return false;
    }

    public ItemStack extract() {
        ItemStack taken = slot.extractItem(0, 1, false);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    /** 把槽内物品掉落在方块位置（拆方块/被爆时调用） */
    public void dropContents(Level level, BlockPos pos) {
        ItemStack stack = slot.extractItem(0, 1, false);
        if (!stack.isEmpty()) {
            net.minecraft.world.level.block.Block.popResource(level, pos, stack);
        }
    }

    public void printStatus(Player player) {
        ItemStack stack = slot.getStackInSlot(0);
        int charge = EnergyTransfer.getItemEnergy(stack);
        int max = EnergyTransfer.getItemCapacity(stack);
        player.displayClientMessage(Component.translatable("block.flightcore.fe_charger.status",
                energy.getEnergyStored(), CAPACITY, charge, max).withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Demand", demandRate);
        tag.putInt("Delivered", deliveredRate);
        tag.put("Slot", slot.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergyStored(tag.getInt("Energy"));
        demandRate = tag.getInt("Demand");
        deliveredRate = tag.getInt("Delivered");
        if (tag.contains("Slot")) {
            slot.deserializeNBT(registries, tag.getCompound("Slot"));
        }
    }
}
