package com.flightcore;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class FlightCoreItem extends Item implements ICurioItem {

    /** 能量上限（FE） */
    public static final int CAPACITY = 1_200_000;
    /** 单次最大接收功率（FE/tick） */
    public static final int MAX_RECEIVE = 12_000;

    public FlightCoreItem(Properties properties) {
        super(properties);
    }

    public static int getEnergy(ItemStack stack) {
        Integer stored = stack.get(ModDataComponents.ENERGY.get());
        return stored == null ? 0 : Math.max(0, Math.min(stored, CAPACITY));
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.set(ModDataComponents.ENERGY.get(), Math.max(0, Math.min(energy, CAPACITY)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int energy = getEnergy(stack);
        tooltip.add(Component.translatable("item.flightcore.flight_core.energy", energy, CAPACITY)
                .withStyle(energy > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable("item.flightcore.flight_core.hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.flightcore.flight_core.charge").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean canEquip(top.theillusivec4.curios.api.SlotContext context, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEquipFromUse(top.theillusivec4.curios.api.SlotContext context, ItemStack stack) {
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x33DDFF;
    }
}
