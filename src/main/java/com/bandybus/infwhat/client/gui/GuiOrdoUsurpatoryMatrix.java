package com.bandybus.infwhat.client.gui;

import com.bandybus.infwhat.block.entity.OrdoUsurpatoryMatrixTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.network.ModNetworking;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * Settings screen for the Ordo Usurpatory Matrix. It intentionally has no inventory slots; all
 * edits are sent to the server through a dedicated packet. The server clamps the values again,
 * so this screen only ever sends values the user picked, never normalized altar data.
 */
@SideOnly(Side.CLIENT)
@SuppressWarnings("unchecked") // GuiScreen.buttonList is a raw List on 1.7.10.
public class GuiOrdoUsurpatoryMatrix extends GuiScreen {

    private static final int BUTTON_SPEED_MINUS = 0;
    private static final int BUTTON_SPEED_PLUS = 1;
    private static final int BUTTON_MODE_PREVIOUS = 2;
    private static final int BUTTON_MODE_NEXT = 3;
    private static final int BUTTON_VALUE_MINUS = 4;
    private static final int BUTTON_VALUE_PLUS = 5;
    private static final int BUTTON_RESET = 6;
    private static final int BUTTON_DONE = 7;

    private static final float SPEED_STEP = 0.5F;
    private static final float STABILITY_STEP = 1.0F;

    private final OrdoUsurpatoryMatrixTileEntity tile;
    private float speedMultiplier;
    private OrdoUsurpatoryMatrixTileEntity.StabilityMode stabilityMode;
    private float stabilityValue;

    public GuiOrdoUsurpatoryMatrix(OrdoUsurpatoryMatrixTileEntity tile) {
        this.tile = tile;
        this.speedMultiplier = tile.getSpeedMultiplier();
        this.stabilityMode = tile.getStabilityMode();
        this.stabilityValue = tile.getStabilityValue();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int center = this.width / 2;
        // 176 px tall content, centred vertically: title, three rows, bound line, reset/done.
        int top = this.height / 2 - 88;
        int left = center - 120;
        int right = center + 96;
        this.buttonList.add(new GuiButton(BUTTON_SPEED_MINUS, left, top + 28, 24, 20, "-"));
        this.buttonList.add(new GuiButton(BUTTON_SPEED_PLUS, right, top + 28, 24, 20, "+"));
        this.buttonList.add(new GuiButton(BUTTON_MODE_PREVIOUS, left, top + 68, 24, 20, "<"));
        this.buttonList.add(new GuiButton(BUTTON_MODE_NEXT, right, top + 68, 24, 20, ">"));
        this.buttonList.add(new GuiButton(BUTTON_VALUE_MINUS, left, top + 108, 24, 20, "-"));
        this.buttonList.add(new GuiButton(BUTTON_VALUE_PLUS, right, top + 108, 24, 20, "+"));
        this.buttonList.add(new GuiButton(BUTTON_RESET, center - 70, top + 156, 60, 20,
                StatCollector.translateToLocal("gui.infwhat.ordo.reset")));
        this.buttonList.add(new GuiButton(BUTTON_DONE, center + 10, top + 156, 60, 20,
                StatCollector.translateToLocal("gui.infwhat.ordo.done")));
        updateValueButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_SPEED_MINUS:
                this.speedMultiplier = Math.max(1.0F, this.speedMultiplier - SPEED_STEP);
                break;
            case BUTTON_SPEED_PLUS:
                this.speedMultiplier = Math.min((float) InfinityWhatConfig.ordoMatrixMaxSpeed(),
                        this.speedMultiplier + SPEED_STEP);
                break;
            case BUTTON_MODE_PREVIOUS:
                this.stabilityMode = previousMode(this.stabilityMode);
                break;
            case BUTTON_MODE_NEXT:
                this.stabilityMode = nextMode(this.stabilityMode);
                break;
            case BUTTON_VALUE_MINUS:
                this.stabilityValue = Math.max(0.0F,
                        this.stabilityValue - STABILITY_STEP);
                break;
            case BUTTON_VALUE_PLUS:
                this.stabilityValue = Math.min(
                        OrdoUsurpatoryMatrixTileEntity.maxStabilityValue(),
                        this.stabilityValue + STABILITY_STEP);
                break;
            case BUTTON_RESET:
                this.speedMultiplier = 1.0F;
                this.stabilityMode = OrdoUsurpatoryMatrixTileEntity.StabilityMode.VANILLA;
                this.stabilityValue = 0.0F;
                break;
            case BUTTON_DONE:
                this.mc.displayGuiScreen(null);
                return;
            default:
                return;
        }
        sendSettings();
        updateValueButtons();
    }

    /**
     * The numeric stability value is only read in FORCE mode; ALWAYS_STABLE uses the altar's own
     * minimum and ALWAYS_UNSTABLE the hard maximum. Disabling the buttons makes that visible.
     */
    private void updateValueButtons() {
        boolean editable =
                this.stabilityMode == OrdoUsurpatoryMatrixTileEntity.StabilityMode.FORCE;
        for (Object element : this.buttonList) {
            GuiButton button = (GuiButton) element;
            if (button.id == BUTTON_VALUE_MINUS || button.id == BUTTON_VALUE_PLUS) {
                button.enabled = editable;
            }
        }
    }

    private void sendSettings() {
        ModNetworking.sendOrdoSettings(this.tile.xCoord, this.tile.yCoord, this.tile.zCoord,
                this.speedMultiplier, this.stabilityMode.ordinal(), this.stabilityValue);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int center = this.width / 2;
        int top = this.height / 2 - 88;

        drawCenteredString(this.fontRendererObj, StatCollector.translateToLocal(
                "gui.infwhat.ordo.title"), center, top, 0xFFFFFF);

        drawString(this.fontRendererObj, StatCollector.translateToLocal(
                "gui.infwhat.ordo.speed"), center - 120, top + 18, 0xA0A8B0);
        drawCenteredString(this.fontRendererObj, String.format("%.1fx", this.speedMultiplier),
                center, top + 34, 0xFFFFFF);

        drawString(this.fontRendererObj, StatCollector.translateToLocal(
                "gui.infwhat.ordo.stability"), center - 120, top + 58, 0xA0A8B0);
        drawCenteredString(this.fontRendererObj, modeLabel(this.stabilityMode), center,
                top + 74, 0xFFFFFF);

        drawString(this.fontRendererObj, StatCollector.translateToLocal(
                "gui.infwhat.ordo.value"), center - 120, top + 98, 0xA0A8B0);
        drawCenteredString(this.fontRendererObj, String.format("%.1f", this.stabilityValue),
                center, top + 114, 0xFFFFFF);

        String bound = this.tile.isBound()
                ? StatCollector.translateToLocalFormatted("gui.infwhat.ordo.bound",
                        this.tile.getBoundAltarX(), this.tile.getBoundAltarY(),
                        this.tile.getBoundAltarZ())
                : StatCollector.translateToLocal("gui.infwhat.ordo.unbound");
        drawCenteredString(this.fontRendererObj, bound, center, top + 138, 0x9FC7A8);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static String modeLabel(OrdoUsurpatoryMatrixTileEntity.StabilityMode mode) {
        return StatCollector.translateToLocal("gui.infwhat.ordo.mode."
                + mode.name().toLowerCase());
    }

    private static OrdoUsurpatoryMatrixTileEntity.StabilityMode nextMode(
            OrdoUsurpatoryMatrixTileEntity.StabilityMode mode) {
        OrdoUsurpatoryMatrixTileEntity.StabilityMode[] values =
                OrdoUsurpatoryMatrixTileEntity.StabilityMode.values();
        return values[(mode.ordinal() + 1) % values.length];
    }

    private static OrdoUsurpatoryMatrixTileEntity.StabilityMode previousMode(
            OrdoUsurpatoryMatrixTileEntity.StabilityMode mode) {
        OrdoUsurpatoryMatrixTileEntity.StabilityMode[] values =
                OrdoUsurpatoryMatrixTileEntity.StabilityMode.values();
        return values[(mode.ordinal() - 1 + values.length) % values.length];
    }
}
