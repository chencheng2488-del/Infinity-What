package com.bandybus.infwhat.client.gui;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.container.ArcaneAspectReplicatorContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

/** Thaumcraft-styled screen for the Arcane Aspect Replicator. */
@SideOnly(Side.CLIENT)
public class GuiArcaneAspectReplicator extends AbstractArcaneMachineGui {

    private static final ResourceLocation TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/arcane_aspect_replicator.png");

    public GuiArcaneAspectReplicator(ArcaneAspectReplicatorContainer container,
                                     InventoryPlayer playerInventory) {
        super(container, playerInventory);
    }

    @Override
    protected ResourceLocation getMachineTexture() {
        return TEXTURE;
    }

    @Override
    protected String getTitleKey() {
        return "container.infwhat.arcane_aspect_replicator";
    }

    @Override
    protected String getHintKey() {
        return "gui.infwhat.arcane.need_sample";
    }

    @Override
    protected String getRateLabelKey() {
        return "gui.infwhat.arcane.io.rate.item";
    }

    @Override
    protected String getRateTooltipKey() {
        return "gui.infwhat.arcane.io.rate.item.tooltip";
    }
}
