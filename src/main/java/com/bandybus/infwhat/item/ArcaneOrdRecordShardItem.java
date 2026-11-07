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

    public ArcaneOrdRecordShardItem() {
        super("ArcaneConfiguration", "message.infwhat.arcane_shard",
                "item.infwhat.arcane_ord_record_shard");
        this.setUnlocalizedName(InfinityWhat.MODID + ".arcane_ord_record_shard");
        this.setTextureName(InfinityWhat.MODID + ":arcane_ord_record_shard");
        this.setMaxStackSize(1);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    /**
     * Allows the machine block to activate while the player is sneaking. Vanilla skips block
     * activation for sneaking players holding an item, which would otherwise make the copy click
     * unreachable.
     */
    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return world.getTileEntity(x, y, z) instanceof IArcaneConfigurable;
    }

    @Override
    protected IConfigurableMachine resolveTarget(TileEntity tile) {
        return tile instanceof IArcaneConfigurable ? (IConfigurableMachine) tile : null;
    }

    /**
     * Consumes clicks on any other tile entity machine so the shard does not open that machine's
     * GUI, and warns the player locally. 1.7.10 has no client-side interact event and returning
     * true here suppresses both the client GUI and the interaction packet.
     */
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y,
                                  int z, int side, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null || resolveTarget(tile) != null) {
            return false;
        }
        if (world.isRemote) {
            InfinityWhat.proxy.showNotLinkedWarning(player);
        }
        return true;
    }
}
