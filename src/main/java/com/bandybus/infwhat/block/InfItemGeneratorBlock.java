package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.menu.ModMenuTypes;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Infinity Item Generator block. It mirrors the Infinity Fluid Generator: right-clicking
 * opens a small UI, while the active output pushes items into adjacent vanilla containers.
 */
public class InfItemGeneratorBlock extends BaseEntityBlock {

    public InfItemGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 1.15.2 has no block-property flag for this, so the shape check is overridden directly. */
    @Override
    public boolean isRedstoneConductor(BlockState state, BlockGetter reader, BlockPos pos) {
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockGetter level) {
        return new InfItemGeneratorBlockEntity();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof InfItemGeneratorBlockEntity) {
            ((InfItemGeneratorBlockEntity) level.getBlockEntity(pos)).wakeOutput();
        }
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof InfItemGeneratorBlockEntity
                ? ((InfItemGeneratorBlockEntity) blockEntity).getComparatorOutput() : 0;
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
            if (blockEntity instanceof InfItemGeneratorBlockEntity) {
                ContainerProviderRegistry.INSTANCE.openContainer(
                        ModMenuTypes.INF_ITEM_GENERATOR, player,
                        buffer -> buffer.writeBlockPos(pos));
            }
        }
        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /**
     * Drops the template item that is currently stored in the generator when a player breaks the
     * block, so it is never silently deleted. The item is locked while infinite mode is enabled,
     * and this hook restores it either way.
     */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InfItemGeneratorBlockEntity) {
                ItemStack stored = ((InfItemGeneratorBlockEntity) blockEntity).getStoredItem();
                if (!stored.isEmpty()) {
                    popResource(level, pos, stored.copy());
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
