package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class InfFluidGeneratorBlock extends BaseEntityBlock {

    public InfFluidGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockGetter level) {
        return new InfFluidGeneratorBlockEntity();
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof InfFluidGeneratorBlockEntity) {
            ((InfFluidGeneratorBlockEntity) level.getBlockEntity(pos)).wakeOutput();
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof InfFluidGeneratorBlockEntity
                ? ((InfFluidGeneratorBlockEntity) blockEntity).getComparatorOutput() : 0;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof MenuProvider ? (MenuProvider) blockEntity : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() == ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InfFluidGeneratorBlockEntity) {
                // Opens the fluid GUI. 1.16.5 Fabric has no generic fluid storage, so pipes
                // cannot interact with this machine on this branch.
                ((ServerPlayer) player).openMenu((InfFluidGeneratorBlockEntity) blockEntity);
            }
        }
        // Always report success on the client so the arm swings; the server opens the menu.
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
