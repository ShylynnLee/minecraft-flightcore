package com.flightcore.block;

import com.flightcore.battery.BatteryGroup;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BatteryManagerBlockEntity extends BlockEntity {

    /** 单次扫描的最大方块数，避免巨型阵列卡服 */
    private static final int MAX_SCAN = 512;
    private static final int SCAN_INTERVAL = 20;

    private BatteryGroup.Stats stats = BatteryGroup.EMPTY;
    private int ticker;

    public BatteryManagerBlockEntity(BlockPos pos, BlockState state) {
        super(com.flightcore.ModBlockEntities.BATTERY_MANAGER.get(), pos, state);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (++ticker >= SCAN_INTERVAL) {
            ticker = 0;
            stats = BatteryGroup.scan(level, pos, MAX_SCAN);
        }
    }

    public BatteryGroup.Stats getStats() {
        return stats;
    }

    public void printStatus(Player player) {
        BatteryGroup.Stats s = stats;
        player.displayClientMessage(Component.translatable("block.flightcore.battery_manager.status",
                s.stored(), s.capacity(), s.inputRate(), s.outputRate(), s.modules()).withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Stored", stats.stored());
        tag.putInt("Capacity", stats.capacity());
        tag.putInt("Input", stats.inputRate());
        tag.putInt("Output", stats.outputRate());
        tag.putInt("Modules", stats.modules());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stats = new BatteryGroup.Stats(tag.getInt("Stored"), tag.getInt("Capacity"),
                tag.getInt("Input"), tag.getInt("Output"), tag.getInt("Modules"));
    }
}
