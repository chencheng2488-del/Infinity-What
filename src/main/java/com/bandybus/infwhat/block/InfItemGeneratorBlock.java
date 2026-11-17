package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.block.Block;
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
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * The Infinity Item Generator block. Right-clicking opens the configuration UI, while every side
 * exposes a Forge {@code IItemHandler} capability so pipe mods can pull from it. The server tick
 * handles only the explicitly configured active output, gated by the per-generator redstone mode.
 */
public class InfItemGeneratorBlock extends ContainerBlock {

    public InfItemGeneratorBlock(Block.Properties properties) {
        super(properties);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public boolean isNormalCube(BlockState state, IBlockReader reader, BlockPos pos) {
        return false;
    }

    /** Harvest metadata lives on the block (Forge 26/27 have no block-property setters for it). */
    @Override
    public ToolType getHarvestTool(BlockState state) {
        return ToolType.PICKAXE;
    }

    @Override
    public int getHarvestLevel(BlockState state) {
        return 0;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader world) {
        return new InfItemGeneratorBlockEntity();
    }

    @Nullable
    @Override
    public INamedContainerProvider getContainer(BlockState state, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return tileEntity instanceof INamedContainerProvider ? (INamedContainerProvider) tileEntity : null;
    }

    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return true;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (!world.isRemote && world.getTileEntity(pos) instanceof InfItemGeneratorBlockEntity) {
            ((InfItemGeneratorBlockEntity) world.getTileEntity(pos)).wakeOutput();
        }
    }

    @Override
    public int getComparatorInputOverride(BlockState state, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return tileEntity instanceof InfItemGeneratorBlockEntity
                ? ((InfItemGeneratorBlockEntity) tileEntity).getComparatorOutput() : 0;
    }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos,
                                    PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (player.getHeldItem(hand).getItem() == ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR) {
            return false;
        }
        if (!world.isRemote && player instanceof ServerPlayerEntity) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof InfItemGeneratorBlockEntity) {
                NetworkHooks.openGui((ServerPlayerEntity) player,
                        (InfItemGeneratorBlockEntity) tileEntity, pos);
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
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState,
                           boolean isMoving) {
        if (!world.isRemote && state.getBlock() != newState.getBlock()) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof InfItemGeneratorBlockEntity) {
                ItemStack stored = ((InfItemGeneratorBlockEntity) tileEntity).getStoredItem();
                if (!stored.isEmpty()) {
                    spawnAsEntity(world, pos, stored.copy());
                }
            }
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }
}
