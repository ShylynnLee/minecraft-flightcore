package com.flightcore.block;

import com.flightcore.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;

public class ChargerBlock extends BaseEntityBlock {

    public static final MapCodec<ChargerBlock> CODEC = simpleCodec(ChargerBlock::new);

    public ChargerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHARGER.get(),
                (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }

    /** 拆掉方块时把槽里的物品掉出来（否则玩家的物品会凭空消失） */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChargerBlockEntity charger) {
            charger.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 手持可充电物品右键 = 放进去充电 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof ChargerBlockEntity charger) {
            // 1) 槽里有物品 → 不管手里拿着什么，先把它取回来
            ItemStack taken = charger.extract();
            if (!taken.isEmpty()) {
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            // 2) 槽是空的 + 手持可充电物品 → 放进去充电
            if (!stack.isEmpty() && stack.getCapability(Capabilities.EnergyStorage.ITEM) != null
                    && charger.insert(stack)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            charger.printStatus(player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 空手右键 = 取回物品 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof ChargerBlockEntity charger) {
            ItemStack taken = charger.extract();
            if (!taken.isEmpty()) {
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
            } else {
                charger.printStatus(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
