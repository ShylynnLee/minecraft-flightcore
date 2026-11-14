package com.flightcore.compat.jade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flightcore.FlightCoreMod;
import com.flightcore.block.BatteryManagerBlock;
import com.flightcore.block.BatteryManagerBlockEntity;
import com.flightcore.block.BatteryModuleBlock;
import com.flightcore.block.BatteryModuleBlockEntity;
import com.flightcore.block.ChargerBlock;
import com.flightcore.block.ChargerBlockEntity;
import com.flightcore.block.GeneratorBlock;
import com.flightcore.block.GeneratorBlockEntity;
import com.flightcore.block.WireBlock;
import com.flightcore.block.WireBlockEntity;
import com.flightcore.energy.EnergyTransfer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * 玉（Jade）集成：
 *  - 蓄电池管理模块：整组总电量/容量 + 输入/输出功率
 *  - 发电机 / 充电器 / 蓄电池模块 / 电线：各自能量与「需求 X / 实际 Y」，用来诊断电网瓶颈
 */
@WailaPlugin(FlightCoreMod.MOD_ID)
public class FlightCoreJadePlugin implements IWailaPlugin {

    public static final ResourceLocation BATTERY_INFO =
            ResourceLocation.fromNamespaceAndPath(FlightCoreMod.MOD_ID, "battery_manager");
    public static final ResourceLocation DEVICE_INFO =
            ResourceLocation.fromNamespaceAndPath(FlightCoreMod.MOD_ID, "device");

    private static final Logger LOGGER = LoggerFactory.getLogger("flightcore/jade");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BatteryDataProvider.INSTANCE, BatteryManagerBlockEntity.class);
        registration.registerBlockDataProvider(DeviceDataProvider.INSTANCE, GeneratorBlockEntity.class);
        registration.registerBlockDataProvider(DeviceDataProvider.INSTANCE, ChargerBlockEntity.class);
        registration.registerBlockDataProvider(DeviceDataProvider.INSTANCE, BatteryModuleBlockEntity.class);
        registration.registerBlockDataProvider(DeviceDataProvider.INSTANCE, WireBlockEntity.class);
        registration.registerBlockDataProvider(DeviceDataProvider.INSTANCE, com.flightcore.block.RubberMakerBlockEntity.class);
        registration.registerBlockDataProvider(DeviceDataProvider.INSTANCE, com.flightcore.block.PlasticMakerBlockEntity.class);
        LOGGER.info("Jade 集成已注册：蓄电池管理模块统计 + 设备 需求/实际 诊断");
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BatteryComponentProvider.INSTANCE, BatteryManagerBlock.class);
        registration.registerBlockComponent(DeviceComponentProvider.INSTANCE, GeneratorBlock.class);
        registration.registerBlockComponent(DeviceComponentProvider.INSTANCE, ChargerBlock.class);
        registration.registerBlockComponent(DeviceComponentProvider.INSTANCE, BatteryModuleBlock.class);
        registration.registerBlockComponent(DeviceComponentProvider.INSTANCE, WireBlock.class);
        registration.registerBlockComponent(DeviceComponentProvider.INSTANCE, com.flightcore.block.RubberMakerBlock.class);
        registration.registerBlockComponent(DeviceComponentProvider.INSTANCE, com.flightcore.block.PlasticMakerBlock.class);
    }

    // ------------------------------------------------------------------ 管理模块

    public enum BatteryDataProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof BatteryManagerBlockEntity manager) {
                var stats = manager.getStats();
                tag.putInt("Stored", stats.stored());
                tag.putInt("Capacity", stats.capacity());
                tag.putInt("Demand", stats.inputRate());
                tag.putInt("Delivered", stats.outputRate());
                tag.putInt("Modules", stats.modules());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return BATTERY_INFO;
        }
    }

    public enum BatteryComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Stored")) {
                return;
            }
            tooltip.add(Component.translatable("jade.flightcore.battery.stored",
                    data.getInt("Stored"), data.getInt("Capacity")));
            tooltip.add(Component.translatable("jade.flightcore.battery.input", data.getInt("Demand")));
            tooltip.add(Component.translatable("jade.flightcore.battery.output", data.getInt("Delivered")));
            tooltip.add(Component.translatable("jade.flightcore.battery.modules", data.getInt("Modules")));
        }

        @Override
        public ResourceLocation getUid() {
            return BATTERY_INFO;
        }
    }

    // ------------------------------------------------------------------ 各类设备

    public enum DeviceDataProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof GeneratorBlockEntity generator) {
                tag.putString("Kind", "generator");
                tag.putInt("Energy", generator.getEnergy().getEnergyStored());
                tag.putInt("Capacity", GeneratorBlockEntity.CAPACITY);
                tag.putInt("Supply", generator.getProductionRate());
                tag.putInt("Actual", generator.getOutputRate());
                tag.putInt("Burn", generator.getTotalBurnSeconds());
            } else if (accessor.getBlockEntity() instanceof ChargerBlockEntity charger) {
                tag.putString("Kind", "charger");
                tag.putInt("Energy", charger.getEnergy().getEnergyStored());
                tag.putInt("Capacity", ChargerBlockEntity.CAPACITY);
                tag.putInt("Supply", charger.getDemandRate());
                tag.putInt("Actual", charger.getDeliveredRate());
                ItemStack stack = charger.getSlotHandler().getStackInSlot(0);
                tag.putInt("ItemEnergy", EnergyTransfer.getItemEnergy(stack));
                tag.putInt("ItemCapacity", EnergyTransfer.getItemCapacity(stack));
            } else if (accessor.getBlockEntity() instanceof BatteryModuleBlockEntity module) {
                tag.putString("Kind", "module");
                tag.putInt("Energy", module.getEnergy().getEnergyStored());
                tag.putInt("Capacity", BatteryModuleBlockEntity.CAPACITY);
                tag.putInt("Supply", module.getInputRate());
                tag.putInt("Actual", module.getOutputRate());
            } else if (accessor.getBlockEntity() instanceof com.flightcore.block.AbstractProcessorBlockEntity machine) {
                tag.putString("Kind", "processor");
                tag.putInt("Energy", machine.getEnergy().getEnergyStored());
                tag.putInt("Capacity", machine.getEnergy().getMaxEnergyStored());
                tag.putInt("Supply", machine.getDemandRate());
                tag.putInt("Actual", machine.getDeliveredRate());
                tag.putInt("Progress", machine.getProgress() * 100 / machine.getProgressTotal());
            } else if (accessor.getBlockEntity() instanceof WireBlockEntity wire) {
                tag.putString("Kind", "wire");
                tag.putInt("Supply", wire.getDemandRate());
                tag.putInt("Actual", wire.getDeliveredRate());
                tag.putInt("Rate", wire.getRatePerSecond());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return DEVICE_INFO;
        }
    }

    public enum DeviceComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            String kind = data.getString("Kind");
            if (kind.isEmpty()) {
                return;
            }

            if (kind.equals("wire")) {
                int demand = data.getInt("Supply");
                int actual = data.getInt("Actual");
                tooltip.add(Component.translatable("jade.flightcore.grid.demand", demand));
                tooltip.add(Component.translatable("jade.flightcore.grid.delivered", actual));
                if (demand > actual) {
                    tooltip.add(Component.translatable("jade.flightcore.grid.shortage")
                            .withStyle(ChatFormatting.RED));
                }
                tooltip.add(Component.translatable("jade.flightcore.grid.rate", data.getInt("Rate"))
                        .withStyle(ChatFormatting.DARK_GRAY));
                return;
            }

            // 发电机 / 充电器 / 蓄电池模块
            tooltip.add(Component.translatable("jade.flightcore.energy",
                    data.getInt("Energy"), data.getInt("Capacity")));

            if (kind.equals("generator")) {
                int burn = data.getInt("Burn");
                if (burn > 0) {
                    tooltip.add(Component.translatable("jade.flightcore.burn", burn));
                } else {
                    tooltip.add(Component.translatable("jade.flightcore.idle"));
                }
                tooltip.add(Component.translatable("jade.flightcore.generator.rate",
                        data.getInt("Supply"), data.getInt("Actual")));
                return;
            }

            if (kind.equals("charger")) {
                int demand = data.getInt("Supply");
                int actual = data.getInt("Actual");
                tooltip.add(Component.translatable("jade.flightcore.supply", demand, actual)
                        .withStyle(demand > actual ? ChatFormatting.RED : ChatFormatting.GREEN));
                if (demand > actual) {
                    tooltip.add(Component.translatable("jade.flightcore.shortage").withStyle(ChatFormatting.RED));
                }
                int item = data.getInt("ItemCapacity");
                if (item > 0) {
                    tooltip.add(Component.translatable("jade.flightcore.item",
                            data.getInt("ItemEnergy"), item));
                }
                return;
            }

            if (kind.equals("processor")) {
                int demand = data.getInt("Supply");
                int actual = data.getInt("Actual");
                tooltip.add(Component.translatable("jade.flightcore.supply", demand, actual)
                        .withStyle(demand > actual ? ChatFormatting.RED : ChatFormatting.GREEN));
                tooltip.add(Component.translatable("jade.flightcore.progress", data.getInt("Progress")));
                if (demand > actual) {
                    tooltip.add(Component.translatable("jade.flightcore.shortage").withStyle(ChatFormatting.RED));
                }
                return;
            }

            // 蓄电池模块
            tooltip.add(Component.translatable("jade.flightcore.module.rate",
                    data.getInt("Supply"), data.getInt("Actual")));
        }

        @Override
        public ResourceLocation getUid() {
            return DEVICE_INFO;
        }
    }
}
