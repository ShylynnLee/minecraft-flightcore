package com.flightcore.block;

import com.flightcore.energy.EnergyTransfer;
import com.flightcore.energy.FEStorage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 发电机：烧任何“可燃烧物品”（原版燃料表），燃烧期间每 tick 产电，并主动向相邻方块供电。
 */
public class GeneratorBlockEntity extends BlockEntity {

    public static final int CAPACITY = 100_000;
    public static final int GENERATION_PER_TICK = 288;
    public static final int PUSH_PER_SIDE = 6_000;
    public static final int MAX_OUT = 6_000;

    private final FEStorage energy = new FEStorage(CAPACITY, 0, MAX_OUT);
    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return burnTicks(stack) > 0;
        }
    };

    private int burnTime = 0;
    private int burnTimeTotal = 0;

    private int totalBurnSeconds;
    private int outputAccum;
    private int producedAccum;
    private int outputRate;
    private int producedRate;
    private int statTicker;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(com.flightcore.ModBlockEntities.GENERATOR.get(), pos, state);
    }

    /**
     * 燃料燃烧时长：本机烧得比原版熔炉快（65/1600 倍），
     * 目的是让「一组煤 = 恰好一个满核心 = 20 分钟飞行」成立。
     * 例：煤炭 1600 tick × 65/1600 = 65 tick（约 3.25 秒）；煤炭块 16000 → 650 tick。
     */
    private static final double FUEL_SCALE = 65.0 / 1600.0;

    public static int burnTicks(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int vanilla = AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
        if (vanilla <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(vanilla * FUEL_SCALE));
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        boolean dirty = false;

        // 1) 没在燃烧就尝试点火
        if (burnTime <= 0 && energy.getFreeSpace() > 0) {
            ItemStack stack = fuel.getStackInSlot(0);
            int ticks = burnTicks(stack);
            if (ticks > 0) {
                fuel.extractItem(0, 1, false);
                burnTime = ticks;
                burnTimeTotal = ticks;
                dirty = true;
            }
        }

        // 2) 燃烧发电
        if (burnTime > 0) {
            burnTime--;
            energy.addInternal(GENERATION_PER_TICK);
            producedAccum += GENERATION_PER_TICK;
            dirty = true;
        }

        // 3) 主动向相邻方块推电（“贴贴供电”）
        int sent = EnergyTransfer.pushToNeighbors(level, pos, energy, PUSH_PER_SIDE);
        if (sent > 0) {
            dirty = true;
        }
        // 统计口径：实际“送出”= 自己的主动推送 + 被电网抽走的量（走的是同一套 extractEnergy 计数）
        outputAccum += sent + energy.takeExtracted();
        if (++statTicker >= 20) {
            statTicker = 0;
            outputRate = outputAccum;
            producedRate = producedAccum;
            outputAccum = 0;
            producedAccum = 0;
            totalBurnSeconds = getTotalBurnSeconds();
        }

        if (dirty) {
            setChanged();
        }
    }

    public FEStorage getEnergy() {
        return energy;
    }

    /** 每秒实际送出（FE/s） */
    public int getOutputRate() {
        return outputRate;
    }

    /** 每秒实际产能（本秒烧了多少）（FE/s） */
    public int getProductionRate() {
        return producedRate;
    }

    /** 每秒产能上限（FE/s） */
    public int getMaxProductionRate() {
        return GENERATION_PER_TICK * 20;
    }

    public int getBurnSeconds() {
        return burnTime / 20;
    }

    public int getTotalBurnSecondsCached() {
        return totalBurnSeconds;
    }

    public ItemStackHandler getFuelHandler() {
        return fuel;
    }

    /**
     * 投入燃料。all=true 时把手里整叠加进去（上限 64）。
     * 返回是否成功投入。
     */
    public boolean insertFuel(ItemStack stack, boolean all) {
        ItemStack inSlot = fuel.getStackInSlot(0);
        int free = inSlot.isEmpty() ? 64 : 64 - inSlot.getCount();
        if (free <= 0) {
            return false;
        }
        int amount = all ? Math.min(free, stack.getCount()) : 1;
        ItemStack rest = fuel.insertItem(0, stack.copyWithCount(amount), false);
        int moved = amount - rest.getCount();
        if (moved <= 0) {
            return false;
        }
        stack.shrink(moved);
        setChanged();
        return true;
    }

    /** 燃料合计还能烧多少秒（正在烧的 + 槽里剩余的全部） */
    public int getTotalBurnSeconds() {
        int ticks = burnTime;
        for (int i = 0; i < fuel.getSlots(); i++) {
            ItemStack stack = fuel.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ticks += burnTicks(stack) * stack.getCount();
            }
        }
        return ticks / 20;
    }

    public ItemStack extractFuel() {
        ItemStack taken = fuel.extractItem(0, 64, false);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    /** 把没烧完的燃料掉落在方块位置 */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < fuel.getSlots(); i++) {
            ItemStack stack = fuel.extractItem(i, 64, false);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack);
            }
        }
    }

    public void printStatus(Player player) {
        player.displayClientMessage(Component.translatable("block.flightcore.fe_generator.status",
                energy.getEnergyStored(), CAPACITY, getTotalBurnSeconds()).withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
        tag.putInt("Output", outputRate);
        tag.putInt("TotalBurn", totalBurnSeconds);
        tag.put("Fuel", fuel.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergyStored(tag.getInt("Energy"));
        burnTime = tag.getInt("BurnTime");
        burnTimeTotal = tag.getInt("BurnTimeTotal");
        outputRate = tag.getInt("Output");
        totalBurnSeconds = tag.getInt("TotalBurn");
        if (tag.contains("Fuel")) {
            fuel.deserializeNBT(registries, tag.getCompound("Fuel"));
        }
    }
}
