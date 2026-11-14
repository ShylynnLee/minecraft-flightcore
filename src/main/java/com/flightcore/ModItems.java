package com.flightcore;

import com.flightcore.item.PortableBatteryItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FlightCoreMod.MOD_ID);

    public static final DeferredItem<Item> FLIGHT_CORE =
            ITEMS.register("flight_core", () -> new FlightCoreItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> PORTABLE_BATTERY =
            ITEMS.register("portable_battery", () -> new PortableBatteryItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> FE_GENERATOR =
            ITEMS.register("fe_generator", () -> new BlockItem(ModBlocks.GENERATOR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> FE_CHARGER =
            ITEMS.register("fe_charger", () -> new BlockItem(ModBlocks.CHARGER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> BATTERY_MODULE =
            ITEMS.register("battery_module", () -> new BlockItem(ModBlocks.BATTERY_MODULE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> BATTERY_MANAGER =
            ITEMS.register("battery_manager", () -> new BlockItem(ModBlocks.BATTERY_MANAGER.get(), new Item.Properties()));

    // ---- 化工/材料线 ----
    public static final DeferredItem<Item> RUBBER =
            ITEMS.register("rubber", () -> new HintItem(new Item.Properties()));

    public static final DeferredItem<Item> ETHYLENE =
            ITEMS.register("ethylene", () -> new HintItem(new Item.Properties()));

    public static final DeferredItem<Item> PLASTIC =
            ITEMS.register("plastic", () -> new HintItem(new Item.Properties()));

    public static final DeferredItem<Item> MACHINE_CASING =
            ITEMS.register("machine_casing", () -> new HintItem(new Item.Properties()));

    public static final DeferredItem<Item> MECHANICAL_CORE =
            ITEMS.register("mechanical_core", () -> new HintItem(new Item.Properties()));

    public static final DeferredItem<BlockItem> RUBBER_MAKER =
            ITEMS.register("rubber_maker", () -> new BlockItem(ModBlocks.RUBBER_MAKER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> PLASTIC_MAKER =
            ITEMS.register("plastic_maker", () -> new BlockItem(ModBlocks.PLASTIC_MAKER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> WIRE_IRON =
            ITEMS.register("wire_iron", () -> new BlockItem(ModBlocks.WIRE_IRON.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> WIRE_COPPER =
            ITEMS.register("wire_copper", () -> new BlockItem(ModBlocks.WIRE_COPPER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> WIRE_GOLD =
            ITEMS.register("wire_gold", () -> new BlockItem(ModBlocks.WIRE_GOLD.get(), new Item.Properties()));

    private ModItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(FLIGHT_CORE.get());
            event.accept(PORTABLE_BATTERY.get());
            event.accept(FE_GENERATOR.get());
            event.accept(FE_CHARGER.get());
            event.accept(BATTERY_MODULE.get());
            event.accept(BATTERY_MANAGER.get());
            event.accept(RUBBER.get());
            event.accept(ETHYLENE.get());
            event.accept(PLASTIC.get());
            event.accept(MACHINE_CASING.get());
            event.accept(MECHANICAL_CORE.get());
            event.accept(RUBBER_MAKER.get());
            event.accept(PLASTIC_MAKER.get());
            event.accept(WIRE_IRON.get());
            event.accept(WIRE_COPPER.get());
            event.accept(WIRE_GOLD.get());
        }
    }
}
