package com.flightcore.compat.jei;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/** JEI 展示用配方（与机器的实际逻辑保持一致） */
public record ProcessorDisplayRecipe(List<ItemStack> inputs, List<ItemStack> outputs, int energy) {}
