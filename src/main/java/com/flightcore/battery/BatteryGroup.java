package com.flightcore.battery;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.flightcore.block.BatteryManagerBlock;
import com.flightcore.block.BatteryModuleBlock;
import com.flightcore.block.BatteryModuleBlockEntity;
import com.flightcore.block.WireBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 遍历一整套蓄电池组（穿过模块与电线），汇总容量与功率。 */
public final class BatteryGroup {

    public record Stats(int stored, int capacity, int inputRate, int outputRate, int modules) {}

    public static final Stats EMPTY = new Stats(0, 0, 0, 0, 0);

    private BatteryGroup() {}

    public static Stats scan(Level level, BlockPos start, int maxBlocks) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        int stored = 0;
        int capacity = 0;
        int input = 0;
        int output = 0;
        int modules = 0;

        while (!queue.isEmpty() && visited.size() <= maxBlocks) {
            BlockPos pos = queue.poll();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BatteryModuleBlockEntity module) {
                stored += module.getEnergy().getEnergyStored();
                capacity += BatteryModuleBlockEntity.CAPACITY;
                input += module.getInputRate();
                output += module.getOutputRate();
                modules++;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (isPartOfGroup(state)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return new Stats(stored, capacity, input, output, modules);
    }

    private static boolean isPartOfGroup(BlockState state) {
        return state.getBlock() instanceof BatteryModuleBlock
                || state.getBlock() instanceof WireBlock
                || state.getBlock() instanceof BatteryManagerBlock;
    }
}
