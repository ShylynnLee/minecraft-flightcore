package com.flightcore.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

/** 加工机方块：右键优先取出产物，其次放入原料（普通右键=整叠，Shift+右键=1 个） */
public abstract class ProcessorBlock extends BaseEntityBlock {

    protected ProcessorBlock(Properties properties) {
        super(properties);
    }

    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AbstractProcessorBlockEntity machine) {
            machine.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof AbstractProcessorBlockEntity machine) {
            ItemStackHandler inv = machine.getItems();

            // 1) 先把产物取走（不管手里拿着什么）
            for (int i = machine.inputSlots(); i < inv.getSlots(); i++) {
                ItemStack out = inv.extractItem(i, 64, false);
                if (!out.isEmpty()) {
                    give(player, out);
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }

            // 2) 放原料进输入槽
            boolean inserted = false;
            if (!stack.isEmpty()) {
                int amount = player.isShiftKeyDown() ? 1 : stack.getCount();
                for (int i = 0; i < machine.inputSlots(); i++) {
                    if (inv.isItemValid(i, stack)) {
                        ItemStack rest = inv.insertItem(i, stack.copyWithCount(amount), false);
                        int moved = amount - rest.getCount();
                        if (moved > 0) {
                            stack.shrink(moved);
                            machine.setChanged();
                            inserted = true;
                            break;
                        }
                    }
                }
            }

            if (inserted) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            // 3) 输入槽里的原料也能取回来
            for (int i = 0; i < machine.inputSlots(); i++) {
                ItemStack in = inv.extractItem(i, 64, false);
                if (!in.isEmpty()) {
                    give(player, in);
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }

            player.displayClientMessage(machine.statusComponent(), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof AbstractProcessorBlockEntity machine) {
            ItemStackHandler inv = machine.getItems();
            boolean took = false;
            // 先取产物
            for (int i = machine.inputSlots(); i < inv.getSlots(); i++) {
                ItemStack out = inv.extractItem(i, 64, false);
                if (!out.isEmpty()) {
                    give(player, out);
                    took = true;
                }
            }
            // 再取回原料
            for (int i = 0; i < machine.inputSlots(); i++) {
                ItemStack in = inv.extractItem(i, 64, false);
                if (!in.isEmpty()) {
                    give(player, in);
                    took = true;
                }
            }
            if (!took) {
                player.displayClientMessage(machine.statusComponent(), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    private void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
