package com.flightcore;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 物品上用数据组件存能量（1.21.1 的正规做法，自动随物品栏同步到客户端）。 */
public final class ModDataComponents {

    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FlightCoreMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            DATA_COMPONENTS.registerComponentType("energy",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {}

    public static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
    }
}
