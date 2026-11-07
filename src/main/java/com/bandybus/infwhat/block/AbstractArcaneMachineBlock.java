package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.item.AbstractConfigurationCardItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Shared block behaviour for the two arcane machines: metal texture registration, GUI opening and
 * dropping the tile entity contents when the block is broken.
 */
public abstract class AbstractArcaneMachineBlock extends BlockContainer {

    private final String tileClassName;
    private final int guiId;

    protected AbstractArcaneMachineBlock(String name, String tileClassName, int guiId) {
        super(Material.rock);
        this.tileClassName = tileClassName;
        this.guiId = guiId;
        this.setUnlocalizedName(InfinityWhat.MODID + "." + name);
        this.setTextureName(InfinityWhat.MODID + ":" + name);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
        this.setHardness(3.0F);
        this.setResistance(10.0F);
        this.setStepSound(Block.soundTypeMetal);
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
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                    int side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() instanceof AbstractConfigurationCardItem
                && ((AbstractConfigurationCardItem) held.getItem()).useOnBlock(held, player, world,
                x, y, z)) {
            return true;
        }
        player.openGui(InfinityWhat.instance, this.guiId, world, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IInventory) {
            IInventory inventory = (IInventory) tile;
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack != null && stack.stackSize > 0) {
                    dropBlockAsItem(world, x, y, z, stack);
                }
            }
        }
        super.breakBlock(world, x, y, z, block, metadata);
    }
}
