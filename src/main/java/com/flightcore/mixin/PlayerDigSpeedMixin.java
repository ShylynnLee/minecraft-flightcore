package com.flightcore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.flightcore.FlightHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原版在"不在地面"时会把挖掘速度 ÷5（Player#getDigSpeed）。
 * 飞行时永远不在地面 → 挖掘恒为 1/5 速度。
 * 这里在"由飞行核心提供飞行"时把惩罚抵消（×5 还原），其他情况不受影响。
 */
@Mixin(Player.class)
public abstract class PlayerDigSpeedMixin {

    @Inject(method = "getDigSpeed", at = @At("RETURN"), cancellable = true)
    private void flightcore$cancelAirMiningPenalty(BlockState state, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        if (self.getAbilities().flying && FlightHandler.isCoreFlightActive(self)) {
            cir.setReturnValue(cir.getReturnValueF() * 5.0F);
        }
    }
}
