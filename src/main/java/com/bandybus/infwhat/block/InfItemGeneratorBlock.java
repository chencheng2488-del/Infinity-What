package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.container.ModGuiHandler;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.item.InfiniteSourceConfigureReplicatorItem;
import cpw.mods.fml.common.network.FMLNetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Infinity Item Generator block. Right-clicking opens the item GUI while the tile entity exposes
 * its single-slot {@code IInventory} so hoppers and pipes can pull from it.
 *
 * <p>The 1.7.10 port overrides {@code isNormalCube(IBlockAccess, ...)} to stop a redstone line from
 * powering dust on the far side. 1.6.4 has no such hook: redstone connectivity goes through
 * {@code World.isBlockNormalCube}, which is driven by the block's opacity, so the machine keeps the
 * vanilla opaque-cube behaviour here.
 */
public class InfItemGeneratorBlock extends BlockContainer {

    public InfItemGeneratorBlock(int blockId, String name) {
        super(blockId, Material.iron);
        this.setUnlocalizedName(InfinityWhat.MODID + "." + name);
        this.setTextureName(InfinityWhat.MODID + ":" + name);
        this.setHardness(3.5F);
        this.setResistance(10.0F);
        this.setStepSound(Block.soundMetalFootstep);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    public TileEntity createNewTileEntity(World world) {
        return new InfItemGeneratorTileEntity();
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn,
                                    int side, float hitX, float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem();
        if (held != null && held.getItem() instanceof InfiniteSourceConfigureReplicatorItem) {
            return false;
        }
        if (!worldIn.isRemote) {
            FMLNetworkHandler.openGui(playerIn, InfinityWhat.instance,
                    ModGuiHandler.ITEM_GENERATOR, worldIn, x, y, z);
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, int x, int y, int z, int blockId, int meta) {
        TileEntity tileEntity = worldIn.getBlockTileEntity(x, y, z);
        if (tileEntity instanceof InfItemGeneratorTileEntity) {
            ItemStack stored = ((InfItemGeneratorTileEntity) tileEntity).getStoredItem();
            if (stored != null && stored.stackSize > 0) {
                this.dropBlockAsItem_do(worldIn, x, y, z, stored.copy());
            }
        }
        super.breakBlock(worldIn, x, y, z, blockId, meta);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World worldIn, int x, int y, int z, int side) {
        TileEntity tileEntity = worldIn.getBlockTileEntity(x, y, z);
        return tileEntity instanceof InfItemGeneratorTileEntity
                ? ((InfItemGeneratorTileEntity) tileEntity).getComparatorOutput() : 0;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void onNeighborBlockChange(World worldIn, int x, int y, int z, int neighborBlockId) {
        super.onNeighborBlockChange(worldIn, x, y, z, neighborBlockId);
        if (!worldIn.isRemote && worldIn.getBlockTileEntity(x, y, z)
                instanceof InfItemGeneratorTileEntity) {
            ((InfItemGeneratorTileEntity) worldIn.getBlockTileEntity(x, y, z)).wakeOutput();
        }
    }
}
