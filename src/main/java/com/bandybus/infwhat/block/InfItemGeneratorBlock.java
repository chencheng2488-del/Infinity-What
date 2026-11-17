package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.menu.ModMenuTypes;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.container.NameableContainerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;

/**
 * The Infinity Item Generator block. It mirrors the Infinity Fluid Generator: right-clicking
 * opens a small UI, while the active output pushes items into adjacent vanilla containers.
 */
public class InfItemGeneratorBlock extends BlockWithEntity {

    public InfItemGeneratorBlock(Block.Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    /** 1.14 has no block-property flag for this, so the shape check is overridden directly. */
    @Override
    public boolean isSimpleFullBlock(BlockState state, BlockView view, BlockPos pos) {
        return false;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView view) {
        return new InfItemGeneratorBlockEntity();
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block,
                               BlockPos fromPos, boolean isMoving) {
        super.neighborUpdate(state, world, pos, block, fromPos, isMoving);
        if (!world.isClient && world.getBlockEntity(pos) instanceof InfItemGeneratorBlockEntity) {
            ((InfItemGeneratorBlockEntity) world.getBlockEntity(pos)).wakeOutput();
        }
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof InfItemGeneratorBlockEntity
                ? ((InfItemGeneratorBlockEntity) blockEntity).getComparatorOutput() : 0;
    }

    @Override
    public NameableContainerProvider createContainerProvider(BlockState state, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof NameableContainerProvider ? (NameableContainerProvider) blockEntity : null;
    }

    @Override
    public boolean activate(BlockState state, World world, BlockPos pos, PlayerEntity player,
                            Hand hand, BlockHitResult hit) {
        if (player.getStackInHand(hand).getItem() == ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR) {
            return false;
        }
        if (!world.isClient && player instanceof ServerPlayerEntity) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof InfItemGeneratorBlockEntity) {
                ContainerProviderRegistry.INSTANCE.openContainer(
                        ModMenuTypes.INF_ITEM_GENERATOR, player,
                        buffer -> buffer.writeBlockPos(pos));
            }
        }
        return true;
    }

    /**
     * Drops the template item that is currently stored in the generator when a player breaks the
     * block, so it is never silently deleted. The item is locked while infinite mode is enabled,
     * and this hook restores it either way.
     */
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof InfItemGeneratorBlockEntity) {
                ItemStack stored = ((InfItemGeneratorBlockEntity) blockEntity).getStoredItem();
                if (!stored.isEmpty()) {
                    dropStack(world, pos, stored.copy());
                }
            }
        }
        super.onBreak(world, pos, state, player);
    }
}
