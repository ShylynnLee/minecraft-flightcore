package com.flightcore.item;

import java.util.List;

import com.flightcore.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 随身蓄电池：放在背包里，自动给身上（饰品/护甲/手持）的 FE 物品供电。
 */
public class PortableBatteryItem extends Item {

    public static final int CAPACITY = 600_000;
    public static final int MAX_RECEIVE = 6_000;
    /** 每 tick 向外供电上限（飞行核心只需 50/tick，绰绰有余） */
    public static final int OUTPUT_RATE = 2_400;

    public PortableBatteryItem(Properties properties) {
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
        tooltip.add(Component.translatable("item.flightcore.portable_battery.energy", energy, CAPACITY)
                .withStyle(energy > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable("item.flightcore.portable_battery.hint").withStyle(ChatFormatting.GRAY));
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
