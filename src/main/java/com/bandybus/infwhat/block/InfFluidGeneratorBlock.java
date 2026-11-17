package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.container.ModGuiHandler;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.item.InfiniteSourceConfigureReplicatorItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Infinity Fluid Generator block. Right-clicking opens the fluid GUI; pipes and cables interact
 * through the legacy Forge {@code IFluidHandler} implementation on the tile entity.
 */
public class InfFluidGeneratorBlock extends BlockContainer {

    public InfFluidGeneratorBlock(String name) {
        super(Material.IRON);
        this.setRegistryName(InfinityWhat.MODID, name);
        this.setUnlocalizedName(InfinityWhat.MODID + "." + name);
        this.setHardness(3.5F);
        this.setResistance(10.0F);
        this.setSoundType(SoundType.METAL);
        this.setHarvestLevel("pickaxe", 2);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new InfFluidGeneratorTileEntity();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem(hand);
        if (held != null && held.getItem() instanceof InfiniteSourceConfigureReplicatorItem) {
            return false;
        }
        if (!worldIn.isRemote) {
            playerIn.openGui(InfinityWhat.instance, ModGuiHandler.FLUID_GENERATOR,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        return tileEntity instanceof InfFluidGeneratorTileEntity
                ? ((InfFluidGeneratorTileEntity) tileEntity).getComparatorOutput() : 0;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block neighborBlock,
                                BlockPos fromPos) {
        super.neighborChanged(state, worldIn, pos, neighborBlock, fromPos);
        if (!worldIn.isRemote && worldIn.getTileEntity(pos) instanceof InfFluidGeneratorTileEntity) {
            ((InfFluidGeneratorTileEntity) worldIn.getTileEntity(pos)).wakeOutput();
        }
    }
}
