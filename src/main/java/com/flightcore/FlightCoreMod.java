package com.flightcore;

import com.flightcore.item.PortableBatteryStorage;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 能量核心（FE Core）：飞行饰品 + FE 能源系统
 *   发电机 / 充电器 / 随身电池 / 蓄电池模块+管理模块 / 铜铁金电线
 */
@Mod(FlightCoreMod.MOD_ID)
public class FlightCoreMod {

    public static final String MOD_ID = "flightcore";

    public FlightCoreMod(IEventBus modBus) {
        ModDataComponents.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.register(modBus);

        modBus.addListener(FlightCoreMod::registerCapabilities);
        modBus.addListener(ModItems::addToCreativeTab);

        NeoForge.EVENT_BUS.addListener(FlightHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(FlightHandler::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(PortableBatteryHandler::onPlayerTick);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // ---- 物品：飞行核心 / 随身电池 ----
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new CoreEnergyStorage(stack), ModItems.FLIGHT_CORE.get());
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new PortableBatteryStorage(stack), ModItems.PORTABLE_BATTERY.get());

        // ---- 发电机 ----
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.GENERATOR.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.GENERATOR.get(),
                (be, side) -> be.getFuelHandler());

        // ---- 充电器 ----
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CHARGER.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CHARGER.get(),
                (be, side) -> be.getSlotHandler());

        // ---- 加工机（橡胶/塑料）----
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.RUBBER_MAKER.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RUBBER_MAKER.get(),
                (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PLASTIC_MAKER.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PLASTIC_MAKER.get(),
                (be, side) -> be.getItems());

        // ---- 蓄电池模块 / 电线 ----
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.BATTERY_MODULE.get(),
                (be, side) -> be.getEnergy());
        // 电线是纯导体：不注册能量能力（不存电、也不能被充放电）
    }
}
