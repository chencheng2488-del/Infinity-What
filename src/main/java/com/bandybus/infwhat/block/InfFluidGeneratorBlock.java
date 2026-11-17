package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.menu.ModMenuTypes;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.container.NameableContainerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;

public class InfFluidGeneratorBlock extends BlockWithEntity {

    public InfFluidGeneratorBlock(Block.Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView view) {
        return new InfFluidGeneratorBlockEntity();
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block,
                               BlockPos fromPos, boolean isMoving) {
        super.neighborUpdate(state, world, pos, block, fromPos, isMoving);
        if (!world.isClient && world.getBlockEntity(pos) instanceof InfFluidGeneratorBlockEntity) {
            ((InfFluidGeneratorBlockEntity) world.getBlockEntity(pos)).wakeOutput();
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof InfFluidGeneratorBlockEntity
                ? ((InfFluidGeneratorBlockEntity) blockEntity).getComparatorOutput() : 0;
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
            if (blockEntity instanceof InfFluidGeneratorBlockEntity) {
                // Opens the fluid GUI. 1.14 Fabric has no generic fluid storage, so pipes
                // cannot interact with this machine on this branch.
                ContainerProviderRegistry.INSTANCE.openContainer(
                        ModMenuTypes.INF_FLUID_GENERATOR, player,
                        buffer -> buffer.writeBlockPos(pos));
            }
        }
        // Always report success on the client so the arm swings; the server opens the menu.
        return true;
    }
}
