package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
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
 * Infinity Item Generator block. Right-clicking opens the item GUI while every side exposes the
 * Forge item handler capability so pipe mods and hoppers can pull from it.
 */
public class InfItemGeneratorBlock extends BlockContainer {

    public InfItemGeneratorBlock(String name) {
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
        return new InfItemGeneratorTileEntity();
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        // Prevent a redstone line touching the machine from powering dust on its far side.
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, ItemStack heldItem,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem(hand);
        if (held != null && held.getItem() instanceof InfiniteSourceConfigureReplicatorItem) {
            return false;
        }
        if (!worldIn.isRemote) {
            playerIn.openGui(InfinityWhat.instance, ModGuiHandler.ITEM_GENERATOR,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof InfItemGeneratorTileEntity) {
            ItemStack stored = ((InfItemGeneratorTileEntity) tileEntity).getStoredItem();
            if (stored != null && stored.stackSize > 0) {
                spawnAsEntity(worldIn, pos, stored.copy());
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        return tileEntity instanceof InfItemGeneratorTileEntity
                ? ((InfItemGeneratorTileEntity) tileEntity).getComparatorOutput() : 0;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block neighborBlock) {
        super.neighborChanged(state, worldIn, pos, neighborBlock);
        if (!worldIn.isRemote && worldIn.getTileEntity(pos) instanceof InfItemGeneratorTileEntity) {
            ((InfItemGeneratorTileEntity) worldIn.getTileEntity(pos)).wakeOutput();
        }
    }
}
