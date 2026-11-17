package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * The Infinity Item Generator block. It mirrors the Infinity Fluid Generator: right-clicking
 * opens a small UI, while every side exposes a Forge {@code IItemHandler} capability so pipe
 * mods can pull from it. The server tick handles only the explicitly configured active output;
 * redstone does not affect this block.
 */
public class InfItemGeneratorBlock extends BlockContainer {

    public InfItemGeneratorBlock(Block.Properties properties) {
        super(properties);
    }

    @Override
    public TileEntity createNewTileEntity(IBlockReader world) {
        return new InfItemGeneratorBlockEntity();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public ToolType getHarvestTool(IBlockState state) {
        return ToolType.PICKAXE;
    }

    @Override
    public int getHarvestLevel(IBlockState state) {
        return 0;
    }

    /** 1.13.2 uses {@code isNormalCube} where 1.14 uses {@code isRedstoneConductor}. */
    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        if (!world.isRemote && world.getTileEntity(pos) instanceof InfItemGeneratorBlockEntity) {
            ((InfItemGeneratorBlockEntity) world.getTileEntity(pos)).wakeOutput();
        }
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return tileEntity instanceof InfItemGeneratorBlockEntity
                ? ((InfItemGeneratorBlockEntity) tileEntity).getComparatorOutput() : 0;
    }

    @Override
    public boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.getHeldItem(hand).getItem() == ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR) {
            return false;
        }
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof InfItemGeneratorBlockEntity) {
                NetworkHooks.openGui((EntityPlayerMP) player,
                        (InfItemGeneratorBlockEntity) tileEntity, pos);
            }
        }
        return true;
    }

    /**
     * Drops the template item currently stored in the generator when a player breaks the block,
     * so it is never silently deleted. 1.13.2's equivalent of 1.14's {@code playerDestroy} is
     * {@code harvestBlock}, which also receives the tile entity.
     */
    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             TileEntity tileEntity, ItemStack tool) {
        if (!world.isRemote && tileEntity instanceof InfItemGeneratorBlockEntity) {
            InfItemGeneratorBlockEntity generator = (InfItemGeneratorBlockEntity) tileEntity;
            ItemStack stored = generator.getStoredItem();
            if (!stored.isEmpty()) {
                Block.spawnAsEntity(world, pos, stored.copy());
                generator.clearStoredItem();
            }
        }
        super.harvestBlock(world, player, pos, state, tileEntity, tool);
    }
}
