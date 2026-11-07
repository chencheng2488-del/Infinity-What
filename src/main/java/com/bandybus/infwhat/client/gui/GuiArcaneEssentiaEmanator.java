package com.bandybus.infwhat.client.gui;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.container.ArcaneEssentiaEmanatorContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Thaumcraft-styled screen for the Arcane Essentia Emanator (1.12.2). */
@SideOnly(Side.CLIENT)
public class GuiArcaneEssentiaEmanator extends AbstractArcaneMachineGui {

    private static final ResourceLocation TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/arcane_essentia_emanator.png");

    public GuiArcaneEssentiaEmanator(ArcaneEssentiaEmanatorContainer container,
                                     InventoryPlayer playerInventory) {
        super(container, playerInventory);
    }

    @Override
    protected ResourceLocation getMachineTexture() {
        return TEXTURE;
    }

    @Override
    protected String getTitleKey() {
        return "container.infwhat.arcane_essentia_emanator";
    }

    @Override
    protected String getHintKey() {
        return "gui.infwhat.arcane.need_essentia_sample";
    }

    @Override
    protected String getRateLabelKey() {
        return "gui.infwhat.arcane.io.rate.essentia";
    }

    @Override
    protected String getRateTooltipKey() {
        return "gui.infwhat.arcane.io.rate.essentia.tooltip";
    }

    @Override
    protected boolean hasRangeRow() {
        return true;
    }
}
