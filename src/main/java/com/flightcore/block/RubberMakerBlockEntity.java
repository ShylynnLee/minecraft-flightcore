package com.flightcore.block;

import com.flightcore.ModBlockEntities;
import com.flightcore.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 橡胶制作器：甘蔗 ×2 + 2000 FE → 橡胶 ×1 + 乙烯 ×1（副产）
 * 槽位：0 = 甘蔗输入，1 = 橡胶输出，2 = 乙烯输出
 */
public class RubberMakerBlockEntity extends AbstractProcessorBlockEntity {

    public static final int CAPACITY = 20_000;
    public static final int ENERGY_PER_OP = 2_000;
    public static final int TICKS_PER_OP = 40;

    public RubberMakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUBBER_MAKER.get(), pos, state, CAPACITY, 1, 2);
    }

    @Override
    protected int inputSlots() {
        return 1;
    }

    @Override
    protected boolean isValidInput(int slot, ItemStack stack) {
        return stack.is(Items.SUGAR_CANE);
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
        if (items.getStackInSlot(0).getCount() < 2) {
            return false;
        }
        return hasRoom(1, ModItems.RUBBER.get(), 1) && hasRoom(2, ModItems.ETHYLENE.get(), 1);
    }

    private boolean hasRoom(int slot, Item item, int amount) {
        ItemStack out = items.getStackInSlot(slot);
        if (out.isEmpty()) {
            return true;
        }
        return out.is(item) && out.getCount() + amount <= out.getMaxStackSize();
    }

    @Override
    protected void finishProcess() {
        items.extractItem(0, 2, false);
        addTo(1, ModItems.RUBBER.get(), 1);
        addTo(2, ModItems.ETHYLENE.get(), 1);
    }

    private void addTo(int slot, Item item, int amount) {
        ItemStack out = items.getStackInSlot(slot);
        if (out.isEmpty()) {
            items.setStackInSlot(slot, new ItemStack(item, amount));
        } else {
            out.grow(amount);
        }
    }
}
