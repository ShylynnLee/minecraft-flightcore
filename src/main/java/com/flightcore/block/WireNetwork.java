package com.flightcore.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 电网（逐段限流版）：电线本身不存电，只是通路。
 *
 * 规则：
 *  - 每根线本 tick 有「配额」= 该线类型的导电速率（这就是“额定载流量”）
 *  - 电力从电源端按路径送到负载端，**经过哪几段线，就扣那几段线的配额**
 *  - 因此：金线干线可以跑 32,000，同时并挂多条铁线支路（各 2,000），干线不会被拖累；
 *    而同一根线被多条路径共用时，配额是共享的（这才是那根线的真实载流量）
 *
 * 与「全网取最细」的区别：不再有“一段铁线拖慢整张网”的行为。
 */
public final class WireNetwork {

    private static final int MAX_WIRES = 512;
    private static final int MAX_SOURCES = 32;
    private static final int MAX_AUGMENTATIONS = 64;
    private static final int SINGLE_SHOT_LIMIT = 1_000_000;

    /** 端点：网络上某个线方块旁边的一个非线方块（电源或负载） */
    public record Endpoint(BlockPos pos, Direction side) {}

    private final List<BlockPos> wires = new ArrayList<>();
    private final Map<BlockPos, List<BlockPos>> wireNeighbors = new HashMap<>();
    private final Map<BlockPos, List<Endpoint>> wireEndpoints = new HashMap<>();
    private final Map<BlockPos, Integer> wireRates = new HashMap<>();
    private BlockPos controller;

    private WireNetwork() {}

    public static WireNetwork build(Level level, BlockPos start) {
        WireNetwork net = new WireNetwork();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        BlockPos minPos = start;

        while (!queue.isEmpty() && visited.size() <= MAX_WIRES) {
            BlockPos pos = queue.poll();
            if (pos.compareTo(minPos) < 0) {
                minPos = pos;
            }

            net.wires.add(pos);
            if (level.getBlockEntity(pos) instanceof WireBlockEntity wire) {
                net.wireRates.put(pos, wire.getRate());
            } else {
                net.wireRates.put(pos, 0);
            }

            List<BlockPos> neighbors = new ArrayList<>();
            List<Endpoint> endpoints = new ArrayList<>();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (level.getBlockState(next).getBlock() instanceof WireBlock) {
                    neighbors.add(next);
                    if (!visited.contains(next)) {
                        visited.add(next);
                        queue.add(next);
                    }
                } else if (level.getCapability(Capabilities.EnergyStorage.BLOCK, next, dir.getOpposite()) != null) {
                    endpoints.add(new Endpoint(next, dir));
                }
            }
            net.wireNeighbors.put(pos, neighbors);
            net.wireEndpoints.put(pos, endpoints);
        }

        net.controller = minPos;
        return net;
    }

    public boolean isController(BlockPos pos) {
        return controller != null && controller.equals(pos);
    }

    public List<BlockPos> getWires() {
        return wires;
    }

    /** 电网本 tick 的结果：实际送出 / 负载总需求（都按 1 tick 计） */
    public record Result(int delivered, int demand) {}

    /** 主控线每 tick 调用：按路径把电从电源端送到负载端，逐段扣配额 */
    public Result transfer(Level level) {
        if (wires.isEmpty()) {
            return new Result(0, 0);
        }

        // 每 tick 重置配额（额定载流量）
        Map<BlockPos, Integer> quota = new HashMap<>();
        for (BlockPos wire : wires) {
            quota.put(wire, wireRates.getOrDefault(wire, 0));
        }

        // 收集端点能力
        Map<BlockPos, IEnergyStorage> caps = new HashMap<>();
        List<BlockPos> sources = new ArrayList<>();
        List<BlockPos> sinks = new ArrayList<>();
        for (BlockPos wire : wires) {
            for (Endpoint endpoint : wireEndpoints.getOrDefault(wire, List.of())) {
                if (caps.containsKey(endpoint.pos())) {
                    continue;
                }
                IEnergyStorage cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, endpoint.pos(),
                        endpoint.side().getOpposite());
                if (cap == null) {
                    continue;
                }
                caps.put(endpoint.pos(), cap);
                if (cap.canExtract() && cap.extractEnergy(1, true) > 0) {
                    sources.add(endpoint.pos());
                }
                if (cap.canReceive() && cap.receiveEnergy(1, true) > 0) {
                    sinks.add(endpoint.pos());
                }
            }
        }

        if (sinks.isEmpty()) {
            return new Result(0, 0);
        }

        // 负载端本 tick 一共想「吃」多少（不受供给限制，用于诊断电网瓶颈）
        long appetite = 0;
        for (BlockPos sinkPos : sinks) {
            IEnergyStorage sink = caps.get(sinkPos);
            if (sink != null) {
                appetite += sink.receiveEnergy(SINGLE_SHOT_LIMIT, true);
            }
        }
        int demand = (int) Math.min(appetite, Integer.MAX_VALUE);

        if (sources.isEmpty()) {
            return new Result(0, demand);
        }

        int delivered = 0;
        int augmentations = 0;
        int processed = 0;

        for (BlockPos sourcePos : sources) {
            if (processed++ >= MAX_SOURCES || augmentations >= MAX_AUGMENTATIONS) {
                break;
            }
            IEnergyStorage source = caps.get(sourcePos);
            while (augmentations < MAX_AUGMENTATIONS) {
                PathResult path = findPath(sourcePos, quota, caps);
                if (path == null) {
                    break;
                }

                int bottleneck = Integer.MAX_VALUE;
                for (BlockPos wire : path.wires()) {
                    bottleneck = Math.min(bottleneck, quota.getOrDefault(wire, 0));
                }
                if (bottleneck <= 0) {
                    break;
                }

                IEnergyStorage sink = caps.get(path.sinkPos());
                int available = source.extractEnergy(Math.min(bottleneck, SINGLE_SHOT_LIMIT), true);
                int space = sink.receiveEnergy(bottleneck, true);
                int amount = Math.min(Math.min(available, space), bottleneck);
                if (amount <= 0) {
                    break;
                }

                int taken = source.extractEnergy(amount, false);
                int put = sink.receiveEnergy(taken, false);
                if (put < taken) {
                    // 负载没吃下，退回电源，绝不丢电
                    source.receiveEnergy(taken - put, false);
                }
                if (put <= 0) {
                    break;
                }

                for (BlockPos wire : path.wires()) {
                    quota.merge(wire, -put, Integer::sum);
                }
                delivered += put;
                augmentations++;
            }
        }
        return new Result(delivered, demand);
    }

    /** 主控线每秒把全网统计写给每一根线（这样随便悬浮哪根线都能看到电网状况） */
    public void publishStats(Level level, int demandPerSecond, int deliveredPerSecond) {
        for (BlockPos wire : wires) {
            if (level.getBlockEntity(wire) instanceof WireBlockEntity be) {
                be.setNetworkStats(demandPerSecond, deliveredPerSecond);
            }
        }
    }

    /** 从「与电源相邻的线」出发 BFS，找一条通往某个有空位的负载的路径 */
    private PathResult findPath(BlockPos sourcePos, Map<BlockPos, Integer> quota, Map<BlockPos, IEnergyStorage> caps) {
        List<BlockPos> starts = new ArrayList<>();
        for (BlockPos wire : wires) {
            if (quota.getOrDefault(wire, 0) <= 0) {
                continue;
            }
            for (Endpoint endpoint : wireEndpoints.getOrDefault(wire, List.of())) {
                if (endpoint.pos().equals(sourcePos)) {
                    starts.add(wire);
                    break;
                }
            }
        }
        if (starts.isEmpty()) {
            return null;
        }

        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos start : starts) {
            parent.put(start, null);
            queue.add(start);
        }

        while (!queue.isEmpty()) {
            BlockPos wire = queue.poll();

            // 这根线上挂着可接收的负载吗？
            for (Endpoint endpoint : wireEndpoints.getOrDefault(wire, List.of())) {
                if (endpoint.pos().equals(sourcePos)) {
                    continue; // 不把自己抽出来的电再灌回自己
                }
                IEnergyStorage cap = caps.get(endpoint.pos());
                if (cap != null && cap.canReceive() && cap.receiveEnergy(1, true) > 0) {
                    List<BlockPos> route = new ArrayList<>();
                    BlockPos cursor = wire;
                    while (cursor != null) {
                        route.add(cursor);
                        cursor = parent.get(cursor);
                    }
                    return new PathResult(route, endpoint.pos());
                }
            }

            for (BlockPos next : wireNeighbors.getOrDefault(wire, List.of())) {
                if (quota.getOrDefault(next, 0) <= 0 || parent.containsKey(next)) {
                    continue;
                }
                parent.put(next, wire);
                queue.add(next);
            }
        }
        return null;
    }

    private record PathResult(List<BlockPos> wires, BlockPos sinkPos) {}
}
