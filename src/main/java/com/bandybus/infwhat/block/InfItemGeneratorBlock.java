package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.container.ModGuiHandler;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.item.InfiniteSourceConfigureReplicatorItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Infinity Item Generator block. Right-clicking opens the item GUI while the tile entity exposes
 * its single-slot {@code IInventory} so hoppers and pipes can pull from it.
 */
public class InfItemGeneratorBlock extends BlockContainer {

    public InfItemGeneratorBlock(String name) {
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
        return new InfItemGeneratorTileEntity();
    }

    @Override
    public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
        // Prevent a redstone line touching the machine from powering dust on its far side.
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn,
                                    int side, float hitX, float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem();
        if (held != null && held.getItem() instanceof InfiniteSourceConfigureReplicatorItem) {
            return false;
        }
        if (!worldIn.isRemote) {
            playerIn.openGui(InfinityWhat.instance, ModGuiHandler.ITEM_GENERATOR,
                    worldIn, x, y, z);
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, int x, int y, int z, Block block, int meta) {
        TileEntity tileEntity = worldIn.getTileEntity(x, y, z);
        if (tileEntity instanceof InfItemGeneratorTileEntity) {
            ItemStack stored = ((InfItemGeneratorTileEntity) tileEntity).getStoredItem();
            if (stored != null && stored.stackSize > 0) {
                this.dropBlockAsItem(worldIn, x, y, z, stored.copy());
            }
        }
        super.breakBlock(worldIn, x, y, z, block, meta);
    }
}
