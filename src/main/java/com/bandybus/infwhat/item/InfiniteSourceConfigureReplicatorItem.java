package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.api.IConfigurableMachine;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.creative.ModCreativeTab;
import net.minecraft.tileentity.TileEntity;

/** Sneak-use copies a generator configuration; regular use pastes it onto a matching generator. */
public class InfiniteSourceConfigureReplicatorItem extends AbstractConfigurationCardItem {

    public InfiniteSourceConfigureReplicatorItem() {
        super("Configuration", "message.infwhat.replicator",
                "item.infwhat.infinite_source_config_replicator");
        this.setRegistryName(InfinityWhat.MODID, "infinite_source_config_replicator");
        this.setTranslationKey(InfinityWhat.MODID + ".infinite_source_config_replicator");
        this.setMaxStackSize(1);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    protected IConfigurableMachine resolveTarget(TileEntity tile) {
        if (tile instanceof InfItemGeneratorTileEntity
                || tile instanceof InfFluidGeneratorTileEntity) {
            return (IConfigurableMachine) tile;
        }
        return null;
    }
}
