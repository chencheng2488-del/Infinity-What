package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.OrdoUsurpatoryMatrixTileEntity;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.integration.thaumcraft6.ThaumcraftAltarControlManager;
import com.bandybus.infwhat.item.AbstractConfigurationCardItem;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Ordo Usurpatory Matrix: a range/structure controller for Thaumcraft infusion altars.
 *
 * <p>The block is only registered when Thaumcraft is loaded. It references no Thaumcraft class
 * directly; the integration is reflection-based in {@link ThaumcraftAltarControlManager}.
 */
public class OrdoUsurpatoryMatrixBlock extends BlockContainer {

    public OrdoUsurpatoryMatrixBlock(String name) {
        super(Material.ROCK);
        setRegistryName(InfinityWhat.MODID, name);
        setTranslationKey(InfinityWhat.MODID + "." + name);
        setCreativeTab(ModCreativeTab.INSTANCE);
        setHardness(3.0F);
        setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new OrdoUsurpatoryMatrixTileEntity();
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
        if (world.isRemote) {
            InfinityWhat.proxy.openOrdoUsurpatoryMatrixGui(pos);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        // Only stops the override. The altar is never reset to defaults here: Thaumcraft
        // recalculates its own speed/stability once the infusion state allows it.
        ThaumcraftAltarControlManager.release(world, pos);
        super.breakBlock(world, pos, state);
    }

    /**
     * Overrides the current (NonNullList) drop hook. Forge's harvest path still calls the
     * deprecated List variant, but that variant delegates to this one, so both paths are covered
     * by a single override. The list is mutable on purpose: harvest-drop event handlers of other
     * mods are allowed to add drops.
     */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                         IBlockState state, int fortune) {
        drops.add(new ItemStack(this));
    }
}
