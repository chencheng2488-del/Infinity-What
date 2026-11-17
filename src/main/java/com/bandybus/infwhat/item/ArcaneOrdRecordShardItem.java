package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.api.IArcaneConfigurable;
import com.bandybus.infwhat.api.IConfigurableMachine;
import com.bandybus.infwhat.creative.ModCreativeTab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Sneak-use copies the I/O profile of a Thaumcraft-linked arcane machine; regular use pastes it
 * onto another machine of the same kind. Sample, locked aspect, infinite mode and stored items are
 * intentionally left out of the profile.
 */
public class ArcaneOrdRecordShardItem extends AbstractConfigurationCardItem {

    public ArcaneOrdRecordShardItem(int itemId) {
        super(itemId, "ArcaneConfiguration", "message.infwhat.arcane_shard",
                "item.infwhat.arcane_ord_record_shard");
        this.setUnlocalizedName(InfinityWhat.MODID + ".arcane_ord_record_shard");
        this.setTextureName(InfinityWhat.MODID + ":arcane_ord_record_shard");
        this.setMaxStackSize(1);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    protected IConfigurableMachine resolveTarget(TileEntity tile) {
        return tile instanceof IArcaneConfigurable ? (IConfigurableMachine) tile : null;
    }

    /**
     * Consumes a sneak-click on any other tile entity machine so the shard warns the player
     * instead of silently doing nothing. 1.6.4 activates the block before the item and has no
     * {@code onItemUseFirst}, so a regular click on an unrelated machine opens that machine's GUI;
     * only the sneak path can be intercepted.
     */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y,
                             int z, int side, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getBlockTileEntity(x, y, z);
        if (tile == null || resolveTarget(tile) != null) {
            return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        }
        if (world.isRemote) {
            InfinityWhat.proxy.showNotLinkedWarning(player);
        }
        return true;
    }
}
