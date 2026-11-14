package com.flightcore;

import com.flightcore.block.BatteryManagerBlock;
import com.flightcore.block.BatteryModuleBlock;
import com.flightcore.block.ChargerBlock;
import com.flightcore.block.GeneratorBlock;
import com.flightcore.block.PlasticMakerBlock;
import com.flightcore.block.RubberMakerBlock;
import com.flightcore.block.WireBlock;
import com.flightcore.block.WireType;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FlightCoreMod.MOD_ID);

    public static final DeferredBlock<Block> GENERATOR =
            BLOCKS.register("fe_generator", () -> new GeneratorBlock(machineProps(MapColor.METAL)));

    public static final DeferredBlock<Block> CHARGER =
            BLOCKS.register("fe_charger", () -> new ChargerBlock(machineProps(MapColor.COLOR_CYAN)));

    public static final DeferredBlock<Block> BATTERY_MODULE =
            BLOCKS.register("battery_module", () -> new BatteryModuleBlock(machineProps(MapColor.COLOR_BLUE)));

    public static final DeferredBlock<Block> BATTERY_MANAGER =
            BLOCKS.register("battery_manager", () -> new BatteryManagerBlock(machineProps(MapColor.COLOR_LIGHT_BLUE)));

    public static final DeferredBlock<Block> RUBBER_MAKER =
            BLOCKS.register("rubber_maker", () -> new RubberMakerBlock(machineProps(MapColor.COLOR_GRAY)));

    public static final DeferredBlock<Block> PLASTIC_MAKER =
            BLOCKS.register("plastic_maker", () -> new PlasticMakerBlock(machineProps(MapColor.COLOR_LIGHT_GRAY)));

    public static final DeferredBlock<Block> WIRE_IRON =
            BLOCKS.register("wire_iron", () -> new WireBlock(WireType.IRON, wireProps()));

    public static final DeferredBlock<Block> WIRE_COPPER =
            BLOCKS.register("wire_copper", () -> new WireBlock(WireType.COPPER, wireProps()));

    public static final DeferredBlock<Block> WIRE_GOLD =
            BLOCKS.register("wire_gold", () -> new WireBlock(WireType.GOLD, wireProps()));

    private ModBlocks() {}

    private static BlockBehaviour.Properties machineProps(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(5.0F, 6.0F)          // 和铁块一样硬（5.0）
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops(); // 必须用正确的工具（镐）才掉落
    }

    private static BlockBehaviour.Properties wireProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.6F)
                .sound(SoundType.METAL)
                .noCollission()
                .noOcclusion();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
