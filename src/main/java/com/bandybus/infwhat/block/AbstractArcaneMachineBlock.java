package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.item.AbstractConfigurationCardItem;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Shared block behaviour for the two arcane machines (1.12.2): registry name, GUI opening and
 * dropping the tile entity contents when the block is broken.
 */
public abstract class AbstractArcaneMachineBlock extends BlockContainer {

    private final String tileClassName;
    private final int guiId;

    protected AbstractArcaneMachineBlock(String name, String tileClassName, int guiId) {
        super(Material.ROCK);
        this.tileClassName = tileClassName;
        this.guiId = guiId;
        setRegistryName(InfinityWhat.MODID, name);
        setTranslationKey(InfinityWhat.MODID + "." + name);
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHardness(3.0F);
        setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        try {
            return (TileEntity) Class.forName(this.tileClassName).getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot create arcane machine tile "
                    + this.tileClassName, exception);
        }
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() instanceof AbstractConfigurationCardItem
                && ((AbstractConfigurationCardItem) held.getItem()).useOnBlock(held, player, world,
                pos)) {
            return true;
        }
        player.openGui(InfinityWhat.instance, this.guiId, world, pos.getX(), pos.getY(),
                pos.getZ());
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IInventory) {
            InventoryHelper.dropInventoryItems(world, pos, (IInventory) tile);
        }
        super.breakBlock(world, pos, state);
    }
}
