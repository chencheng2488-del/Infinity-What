package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.OrdoUsurpatoryMatrixTileEntity;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftAltarControlManager;
import com.bandybus.infwhat.item.AbstractConfigurationCardItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Ordo Usurpatory Matrix: a range/structure controller for Thaumcraft 4 infusion altars.
 *
 * <p>The block is only registered when Thaumcraft is loaded. It references no Thaumcraft class
 * directly; the integration is reflection-based in {@link ThaumcraftAltarControlManager}.
 */
public class OrdoUsurpatoryMatrixBlock extends BlockContainer {

    public OrdoUsurpatoryMatrixBlock(String name) {
        super(Material.rock);
        this.setUnlocalizedName(InfinityWhat.MODID + "." + name);
        this.setTextureName(InfinityWhat.MODID + ":" + name);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
        this.setHardness(3.0F);
        this.setResistance(10.0F);
        this.setStepSound(Block.soundTypeStone);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new OrdoUsurpatoryMatrixTileEntity();
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
        if (world.isRemote) {
            InfinityWhat.proxy.openOrdoUsurpatoryMatrixGui(x, y, z);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        // Only stops the override. The altar is never reset to defaults here: Thaumcraft
        // recalculates its own speed/stability once the infusion state allows it.
        ThaumcraftAltarControlManager.release(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
