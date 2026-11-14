package com.flightcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * 飞行状态机。完全照搬 Botania 天翼族之冠验证过的骨架：
 *   1. 每 tick 检查“是否佩戴 + 能量是否够”
 *   2. 够 → 直接把玩家原版飞行开关打开（mayfly），飞行中每 tick 扣能量
 *   3. 不够/没戴 → 复位 mayfly/flying/invulnerable，并把玩家从名单里移除
 *   4. 静态名单只用于“收回自己给出的飞行”，不干扰创造模式和其他来源的飞行
 *   5. 玩家登出必须清理名单，否则下次进服会残留幽灵飞行
 */
public final class FlightHandler {

    /** 每 tick 飞行消耗（FE） */
    public static final int FLY_COST = 50;

    /** 戴核心飞行时的飞行速度（原版默认 0.05，这里慢下来） */
    public static final float FLY_SPEED = 0.03f;
    public static final float VANILLA_FLY_SPEED = 0.05f;

    private static final List<String> playersWithFlight = Collections.synchronizedList(new ArrayList<>());

    private FlightHandler() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        updatePlayerFlyStatus(event.getEntity());
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        String name = event.getEntity().getGameProfile().getName();
        playersWithFlight.remove(name + ":false");
        playersWithFlight.remove(name + ":true");
    }

    private static void updatePlayerFlyStatus(Player player) {
        ItemStack core = findCore(player);

        if (playersWithFlight.contains(playerStr(player))) {
            if (canFly(core)) {
                player.getAbilities().mayfly = true;
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().setFlyingSpeed(FLY_SPEED);
                }
                if (player.getAbilities().flying) {
                    // 只在服务端扣能量，且创造/旁观不消耗
                    if (!player.level().isClientSide() && !player.isCreative() && !player.isSpectator()) {
                        FlightCoreItem.setEnergy(core, FlightCoreItem.getEnergy(core) - FLY_COST);
                    }
                }
            } else {
                if (!player.isSpectator() && !player.getAbilities().instabuild) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.getAbilities().invulnerable = false;
                    player.getAbilities().setFlyingSpeed(VANILLA_FLY_SPEED);
                }
                playersWithFlight.remove(playerStr(player));
            }
        } else if (canFly(core)) {
            playersWithFlight.add(playerStr(player));
            player.getAbilities().mayfly = true;
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().setFlyingSpeed(FLY_SPEED);
            }
        }
    }

    private static ItemStack findCore(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(ModItems.FLIGHT_CORE.get()))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    private static boolean canFly(ItemStack core) {
        return !core.isEmpty() && FlightCoreItem.getEnergy(core) >= FLY_COST;
    }

    /** 该玩家当前是否正由「飞行核心」提供飞行（供 mixin 判断） */
    public static boolean isCoreFlightActive(Player player) {
        return playersWithFlight.contains(playerStr(player));
    }

    private static String playerStr(Player player) {
        return player.getGameProfile().getName() + ":" + player.level().isClientSide();
    }
}
