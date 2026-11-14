package com.flightcore.compat.jei;

import java.util.List;

import com.flightcore.FlightCoreMod;
import com.flightcore.ModItems;
import com.flightcore.block.PlasticMakerBlockEntity;
import com.flightcore.block.RubberMakerBlockEntity;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI 集成：让「橡胶制作器 / 塑料制作器」的配方在 JEI 里可见，
 * 这样按 U（用途）看材料、按 R（来源）看产物都能找到获取路径。
 */
@JeiPlugin
public class FlightCoreJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(FlightCoreMod.MOD_ID, "jei_plugin");

    public static final RecipeType<ProcessorDisplayRecipe> RUBBER_MAKER =
            RecipeType.create(FlightCoreMod.MOD_ID, "rubber_maker", ProcessorDisplayRecipe.class);
    public static final RecipeType<ProcessorDisplayRecipe> PLASTIC_MAKER =
            RecipeType.create(FlightCoreMod.MOD_ID, "plastic_maker", ProcessorDisplayRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new ProcessorCategory(guiHelper, RUBBER_MAKER, "block.flightcore.rubber_maker",
                        new ItemStack(ModItems.RUBBER_MAKER.get())),
                new ProcessorCategory(guiHelper, PLASTIC_MAKER, "block.flightcore.plastic_maker",
                        new ItemStack(ModItems.PLASTIC_MAKER.get())));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RUBBER_MAKER, List.of(new ProcessorDisplayRecipe(
                List.of(new ItemStack(Items.SUGAR_CANE, 2)),
                List.of(new ItemStack(ModItems.RUBBER.get()), new ItemStack(ModItems.ETHYLENE.get())),
                RubberMakerBlockEntity.ENERGY_PER_OP)));

        registration.addRecipes(PLASTIC_MAKER, List.of(new ProcessorDisplayRecipe(
                List.of(new ItemStack(ModItems.RUBBER.get(), 2), new ItemStack(ModItems.ETHYLENE.get())),
                List.of(new ItemStack(ModItems.PLASTIC.get(), 2)),
                PlasticMakerBlockEntity.ENERGY_PER_OP)));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(RUBBER_MAKER, ModItems.RUBBER_MAKER.get());
        registration.addRecipeCatalysts(PLASTIC_MAKER, ModItems.PLASTIC_MAKER.get());
    }
}
