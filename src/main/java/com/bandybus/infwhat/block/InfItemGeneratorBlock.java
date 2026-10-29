package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * The Infinity Item Generator block. It mirrors the Infinity Fluid Generator: right-clicking
 * opens a small UI, while every side exposes a Forge {@code IItemHandler} capability so pipe
 * mods can pull from it. The server tick handles only the explicitly configured active output;
 * redstone does not affect this block.
 */
public class InfItemGeneratorBlock extends ContainerBlock {

    public InfItemGeneratorBlock(AbstractBlock.Properties properties) {
        super(properties);
    }

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(IBlockReader world) {
        return new InfItemGeneratorBlockEntity();
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
            if (tileEntity instanceof InfItemGeneratorBlockEntity) {
                NetworkHooks.openGui((ServerPlayerEntity) player,
                        (InfItemGeneratorBlockEntity) tileEntity, pos);
            }
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
    }

    /**
     * Drops the template item that is currently stored in the generator when a player breaks the
     * block, so it is never silently deleted. The item is locked while infinite mode is enabled,
     * and this hook restores it either way.
     */
    @Override
    public void playerWillDestroy(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClientSide) {
            TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof InfItemGeneratorBlockEntity) {
                ItemStack stored = ((InfItemGeneratorBlockEntity) tileEntity).getStoredItem();
                if (!stored.isEmpty()) {
                    popResource(world, pos, stored.copy());
                }
            }
        }
        super.playerWillDestroy(world, pos, state, player);
    }
}
