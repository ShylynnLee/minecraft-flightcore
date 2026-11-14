package com.flightcore.block;

import com.flightcore.ModBlockEntities;
import com.flightcore.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 塑料制作器：橡胶 ×2 + 乙烯 ×1 + 8000 FE → 塑料 ×2
 * 槽位：0 = 橡胶，1 = 乙烯，2 = 塑料输出
 */
public class PlasticMakerBlockEntity extends AbstractProcessorBlockEntity {

    public static final int CAPACITY = 40_000;
    public static final int ENERGY_PER_OP = 8_000;
    public static final int TICKS_PER_OP = 60;

    public PlasticMakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLASTIC_MAKER.get(), pos, state, CAPACITY, 2, 1);
    }

    @Override
    protected int inputSlots() {
        return 2;
    }

    @Override
    protected boolean isValidInput(int slot, ItemStack stack) {
        return slot == 0 ? stack.is(ModItems.RUBBER.get()) : stack.is(ModItems.ETHYLENE.get());
    }

    @Override
    protected int energyPerOperation() {
        return ENERGY_PER_OP;
    }

    @Override
    protected int ticksPerOperation() {
        return TICKS_PER_OP;
    }

    @Override
    protected boolean canProcess() {
        if (items.getStackInSlot(0).getCount() < 2 || items.getStackInSlot(1).getCount() < 1) {
            return false;
        }
        ItemStack out = items.getStackInSlot(2);
        return out.isEmpty() || (out.is(ModItems.PLASTIC.get()) && out.getCount() + 2 <= out.getMaxStackSize());
    }

    @Override
    protected void finishProcess() {
        items.extractItem(0, 2, false);
        items.extractItem(1, 1, false);
        ItemStack out = items.getStackInSlot(2);
        if (out.isEmpty()) {
            items.setStackInSlot(2, new ItemStack(ModItems.PLASTIC.get(), 2));
        } else {
            out.grow(2);
        }
    }
}
