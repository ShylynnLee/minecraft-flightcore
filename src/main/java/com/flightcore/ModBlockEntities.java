package com.flightcore;

import com.flightcore.block.BatteryManagerBlockEntity;
import com.flightcore.block.BatteryModuleBlockEntity;
import com.flightcore.block.ChargerBlockEntity;
import com.flightcore.block.GeneratorBlockEntity;
import com.flightcore.block.PlasticMakerBlockEntity;
import com.flightcore.block.RubberMakerBlockEntity;
import com.flightcore.block.WireBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FlightCoreMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR =
            BLOCK_ENTITIES.register("fe_generator",
                    () -> BlockEntityType.Builder.of(GeneratorBlockEntity::new, ModBlocks.GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargerBlockEntity>> CHARGER =
            BLOCK_ENTITIES.register("fe_charger",
                    () -> BlockEntityType.Builder.of(ChargerBlockEntity::new, ModBlocks.CHARGER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryModuleBlockEntity>> BATTERY_MODULE =
            BLOCK_ENTITIES.register("battery_module",
                    () -> BlockEntityType.Builder.of(BatteryModuleBlockEntity::new, ModBlocks.BATTERY_MODULE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryManagerBlockEntity>> BATTERY_MANAGER =
            BLOCK_ENTITIES.register("battery_manager",
                    () -> BlockEntityType.Builder.of(BatteryManagerBlockEntity::new, ModBlocks.BATTERY_MANAGER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubberMakerBlockEntity>> RUBBER_MAKER =
            BLOCK_ENTITIES.register("rubber_maker",
                    () -> BlockEntityType.Builder.of(RubberMakerBlockEntity::new, ModBlocks.RUBBER_MAKER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlasticMakerBlockEntity>> PLASTIC_MAKER =
            BLOCK_ENTITIES.register("plastic_maker",
                    () -> BlockEntityType.Builder.of(PlasticMakerBlockEntity::new, ModBlocks.PLASTIC_MAKER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WireBlockEntity>> WIRE =
            BLOCK_ENTITIES.register("wire",
                    () -> BlockEntityType.Builder.of(WireBlockEntity::new,
                            ModBlocks.WIRE_IRON.get(), ModBlocks.WIRE_COPPER.get(), ModBlocks.WIRE_GOLD.get()).build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
