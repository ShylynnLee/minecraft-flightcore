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

public class GeneratorBlock extends BaseEntityBlock {

    public static final MapCodec<GeneratorBlock> CODEC = simpleCodec(GeneratorBlock::new);

    public GeneratorBlock(Properties properties) {
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
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.GENERATOR.get(),
                (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }

    /** 拆掉方块时把没烧完的燃料掉出来 */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            generator.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 手持可燃烧物品右键 = 投料 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            // 普通右键 = 把手里整叠燃料塞进去；Shift+右键 = 只放 1 个
            if (GeneratorBlockEntity.burnTicks(stack) > 0
                    && generator.insertFuel(stack, !player.isShiftKeyDown())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            if (player.isShiftKeyDown()) {
                generator.printStatus(player);
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 空手右键 = 取回燃料 / 查看状态 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            ItemStack taken = generator.extractFuel();
            if (!taken.isEmpty()) {
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
            } else {
                generator.printStatus(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
