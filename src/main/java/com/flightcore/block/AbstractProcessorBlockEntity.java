package com.flightcore.block;

import com.flightcore.energy.EnergyTransfer;
import com.flightcore.energy.FEStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 通用加工机：输入物品 + 消耗 FE → 产出物品。
 * 电不够时"有多少电干多少活"（按 tick 分摊耗电，电量不足就暂停推进）。
 */
public abstract class AbstractProcessorBlockEntity extends BlockEntity {

    public static final int MAX_IN = 12_000;

    protected final FEStorage energy;
    protected final ItemStackHandler items;
    protected int progress;

    /** 需求 / 实际（每秒，供 Jade 诊断） */
    private int demandAccum;
    private int deliveredAccum;
    private int demandRate;
    private int deliveredRate;
    private int statTicker;

    protected AbstractProcessorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                           int capacity, int inputSlots, int outputSlots) {
        super(type, pos, state);
        this.energy = new FEStorage(capacity, MAX_IN, 0);
        this.items = new ItemStackHandler(inputSlots + outputSlots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < inputSlots && isValidInput(slot, stack);
            }
        };
    }

    protected abstract int inputSlots();

    protected abstract boolean isValidInput(int slot, ItemStack stack);

    /** 每次加工需要的总电 */
    protected abstract int energyPerOperation();

    /** 每次加工需要的 tick */
    protected abstract int ticksPerOperation();

    /** 输入是否满足 + 输出是否有空间 */
    protected abstract boolean canProcess();

    /** 扣输入、产出（在加工完成那一刻调用） */
    protected abstract void finishProcess();

    public FEStorage getEnergy() {
        return energy;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public int getProgress() {
        return progress;
    }

    public int getProgressTotal() {
        return Math.max(1, ticksPerOperation());
    }

    public int getDemandRate() {
        return demandRate;
    }

    public int getDeliveredRate() {
        return deliveredRate;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        boolean dirty = false;

        // 从相邻方块抽电（也能被外部推电）
        if (EnergyTransfer.pullFromNeighbors(level, pos, energy, MAX_IN) > 0) {
            dirty = true;
        }

        int perTick = Math.max(1, (int) Math.ceil((double) energyPerOperation() / Math.max(1, ticksPerOperation())));

        if (progress > 0) {
            // 加工中：有多少电干多少活
            demandAccum += perTick;
            if (energy.getEnergyStored() >= perTick) {
                energy.consumeInternal(perTick);
                deliveredAccum += perTick;
                progress++;
                if (progress > ticksPerOperation()) {
                    finishProcess();
                    progress = 0;
                }
                dirty = true;
            }
        } else if (canProcess() && energy.getEnergyStored() >= perTick) {
            progress = 1;
            dirty = true;
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

    /** 右键提示：电量 / 进度 / 供需 */
    public net.minecraft.network.chat.Component statusComponent() {
        return net.minecraft.network.chat.Component.translatable("block.flightcore.processor.status",
                energy.getEnergyStored(), energy.getMaxEnergyStored(),
                progress * 100 / getProgressTotal(), demandRate, deliveredRate)
                .withStyle(net.minecraft.ChatFormatting.AQUA);
    }

    /** 把全部物品掉落（拆方块时调用） */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.extractItem(i, 64, false);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("Demand", demandRate);
        tag.putInt("Delivered", deliveredRate);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergyStored(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        demandRate = tag.getInt("Demand");
        deliveredRate = tag.getInt("Delivered");
        if (tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }
    }
}
