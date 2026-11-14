package com.flightcore;

import java.util.ArrayList;
import java.util.List;

import com.flightcore.item.PortableBatteryItem;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * 随身蓄电池：每 10 tick 把电送到玩家身上的 FE 装备/饰品上。
 */
public final class PortableBatteryHandler {

    private static final int INTERVAL = 10;

    private PortableBatteryHandler() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % INTERVAL != 0) {
            return;
        }

        List<ItemStack> batteries = findBatteries(player);
        if (batteries.isEmpty()) {
            return;
        }

        List<ItemStack> targets = collectTargets(player);
        if (targets.isEmpty()) {
            return;
        }

        int budget = PortableBatteryItem.OUTPUT_RATE * INTERVAL;
        for (ItemStack battery : batteries) {
            if (budget <= 0) {
                break;
            }
            int stored = PortableBatteryItem.getEnergy(battery);
            if (stored <= 0) {
                continue;
            }
            for (ItemStack target : targets) {
                if (budget <= 0) {
                    break;
                }
                if (target.isEmpty() || target.getItem() instanceof PortableBatteryItem) {
                    continue;
                }
                IEnergyStorage cap = target.getCapability(Capabilities.EnergyStorage.ITEM);
                if (cap == null || !cap.canReceive()) {
                    continue;
                }
                int space = cap.getMaxEnergyStored() - cap.getEnergyStored();
                int want = Math.min(Math.min(budget, stored), space);
                if (want <= 0) {
                    continue;
                }
                int accepted = cap.receiveEnergy(want, false);
                if (accepted > 0) {
                    stored -= accepted;
                    budget -= accepted;
                    PortableBatteryItem.setEnergy(battery, stored);
                }
            }
        }
    }

    private static List<ItemStack> findBatteries(Player player) {
        List<ItemStack> result = new ArrayList<>();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof PortableBatteryItem) {
                result.add(stack);
            }
        }
        return result;
    }

    private static List<ItemStack> collectTargets(Player player) {
        List<ItemStack> targets = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            targets.add(player.getItemBySlot(slot));
        }
        targets.add(player.getMainHandItem());
        targets.add(player.getOffhandItem());

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
                var stacks = stacksHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    targets.add(stacks.getStackInSlot(i));
                }
            }
        });
        return targets;
    }
}
