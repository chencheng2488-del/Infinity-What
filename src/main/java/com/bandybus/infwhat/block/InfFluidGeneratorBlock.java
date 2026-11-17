package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.container.ModGuiHandler;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.item.InfiniteSourceConfigureReplicatorItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Infinity Fluid Generator block. Right-clicking opens the fluid GUI; pipes and cables interact
 * through the legacy Forge {@code IFluidHandler} implementation on the tile entity.
 */
public class InfFluidGeneratorBlock extends BlockContainer {

    public InfFluidGeneratorBlock(String name) {
        super(Material.iron);
        this.setUnlocalizedName(InfinityWhat.MODID + "." + name);
        this.setTextureName(InfinityWhat.MODID + ":" + name);
        this.setHardness(3.5F);
        this.setResistance(10.0F);
        this.setStepSound(Block.soundTypeMetal);
        this.setHarvestLevel("pickaxe", 2);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new InfFluidGeneratorTileEntity();
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn,
                                    int side, float hitX, float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem();
        if (held != null && held.getItem() instanceof InfiniteSourceConfigureReplicatorItem) {
            return false;
        }
        if (!worldIn.isRemote) {
            playerIn.openGui(InfinityWhat.instance, ModGuiHandler.FLUID_GENERATOR,
                    worldIn, x, y, z);
        }
        return true;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World worldIn, int x, int y, int z, int side) {
        TileEntity tileEntity = worldIn.getTileEntity(x, y, z);
        return tileEntity instanceof InfFluidGeneratorTileEntity
                ? ((InfFluidGeneratorTileEntity) tileEntity).getComparatorOutput() : 0;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void onNeighborBlockChange(World worldIn, int x, int y, int z, Block neighborBlock) {
        super.onNeighborBlockChange(worldIn, x, y, z, neighborBlock);
        if (!worldIn.isRemote && worldIn.getTileEntity(x, y, z)
                instanceof InfFluidGeneratorTileEntity) {
            ((InfFluidGeneratorTileEntity) worldIn.getTileEntity(x, y, z)).wakeOutput();
        }
    }
}
