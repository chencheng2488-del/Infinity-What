package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class InfFluidGeneratorBlock extends ContainerBlock {

    public InfFluidGeneratorBlock(AbstractBlock.Properties properties) {
        super(properties);
    }

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(IBlockReader world) {
        return new InfFluidGeneratorBlockEntity();
    }

    @Nullable
    @Override
    public INamedContainerProvider getMenuProvider(BlockState state, World world, BlockPos pos) {
        TileEntity tileEntity = world.getBlockEntity(pos);
        return tileEntity instanceof INamedContainerProvider ? (INamedContainerProvider) tileEntity : null;
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        if (player.getItemInHand(hand).getItem() == ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR.get()) {
            return ActionResultType.PASS;
        }
        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof InfFluidGeneratorBlockEntity) {
                // Opens the fluid GUI. Pipes and cables interact through the fluid
                // capability instead of this method, so this does not affect them.
                NetworkHooks.openGui((ServerPlayerEntity) player,
                        (InfFluidGeneratorBlockEntity) tileEntity, pos);
            }
        }
        // Always report success on the client so the arm swings; the server opens the menu.
        return ActionResultType.sidedSuccess(world.isClientSide);
    }
}
