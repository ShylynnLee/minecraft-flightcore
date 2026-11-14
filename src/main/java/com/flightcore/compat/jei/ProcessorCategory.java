package com.flightcore.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 加工机配方分类：左输入 → 右输出，下方标注耗电 */
public class ProcessorCategory implements IRecipeCategory<ProcessorDisplayRecipe> {

    private final RecipeType<ProcessorDisplayRecipe> type;
    private final Component title;
    private final IDrawable icon;

    public ProcessorCategory(IGuiHelper guiHelper, RecipeType<ProcessorDisplayRecipe> type,
                             String titleKey, ItemStack machineIcon) {
        this.type = type;
        this.title = Component.translatable(titleKey);
        this.icon = guiHelper.createDrawableItemStack(machineIcon);
    }

    @Override
    public RecipeType<ProcessorDisplayRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 108;
    }

    @Override
    public int getHeight() {
        return 40;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProcessorDisplayRecipe recipe, IFocusGroup focuses) {
        int x = 1;
        for (ItemStack input : recipe.inputs()) {
            builder.addInputSlot(x, 1).addItemStack(input);
            x += 18;
        }
        int outX = 71;
        for (ItemStack output : recipe.outputs()) {
            builder.addOutputSlot(outX, 1).addItemStack(output);
            outX += 18;
        }
    }

    @Override
    public void draw(ProcessorDisplayRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, "→", 42, 5, 0xFF808080, false);
        graphics.drawString(font, Component.translatable("jei.flightcore.energy", recipe.energy()), 1, 26, 0xFF404040, false);
    }
}
