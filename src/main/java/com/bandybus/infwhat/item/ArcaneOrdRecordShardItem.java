package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.api.IArcaneConfigurable;
import com.bandybus.infwhat.api.IConfigurableMachine;
import com.bandybus.infwhat.creative.ModCreativeTab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Sneak-use copies the I/O profile of a Thaumcraft-linked arcane machine; regular use pastes it
 * onto another machine of the same kind. Sample, locked aspect, infinite mode and stored items are
 * intentionally left out of the profile.
 */
public class ArcaneOrdRecordShardItem extends AbstractConfigurationCardItem {

    public ArcaneOrdRecordShardItem() {
        super("ArcaneConfiguration", "message.infwhat.arcane_shard",
                "item.infwhat.arcane_ord_record_shard");
        this.setRegistryName(InfinityWhat.MODID, "arcane_ord_record_shard");
        this.setTranslationKey(InfinityWhat.MODID + ".arcane_ord_record_shard");
        this.setMaxStackSize(1);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    /**
     * Allows the machine block to activate while the player is sneaking. Vanilla skips block
     * activation for sneaking players holding an item, which would otherwise make the copy click
     * unreachable.
     */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos,
                                      EntityPlayer player) {
        return world.getTileEntity(pos) instanceof IArcaneConfigurable;
    }

    @Override
    protected IConfigurableMachine resolveTarget(TileEntity tile) {
        return tile instanceof IArcaneConfigurable ? (IConfigurableMachine) tile : null;
    }

    /**
     * Consumes clicks on any other tile entity machine so the shard does not open that machine's
     * GUI. The client returns SUCCESS without acting; the server shows the warning and skips the
     * block activation.
     */
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos,
                                           EnumFacing facing, float hitX, float hitY, float hitZ,
                                           EnumHand hand) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null || resolveTarget(tile) != null) {
            return EnumActionResult.PASS;
        }
        if (!world.isRemote) {
            show(player, "not_linked", TextFormatting.RED);
        }
        return EnumActionResult.SUCCESS;
    }
}
