package com.bandybus.infwhat.client.gui;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.container.AbstractArcaneMachineContainer;
import com.bandybus.infwhat.integration.thaumcraft6.AbstractArcaneMachineTileEntity;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import thaumcraft.api.aspects.Aspect;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

/**
 * Shared Thaumcraft-styled screen for the two arcane machines (1.12.2): main page plus an
 * active-output I/O page. Subclasses only provide texture, title and hint keys.
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractArcaneMachineGui extends GuiContainer {

    protected static final ResourceLocation IO_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/arcane_source_io.png");

    protected static final int GUI_WIDTH = 184;
    protected static final int GUI_HEIGHT = 196;

    private static final int INFINITE_X = 9;
    private static final int INFINITE_WIDTH = 115;
    private static final int IO_X = 128;
    private static final int IO_WIDTH = 47;
    private static final int BUTTON_Y = 68;
    private static final int BUTTON_HEIGHT = 16;

    private static final int IO_CLOSE_X = 160;
    private static final int IO_CLOSE_Y = 7;
    private static final int IO_CLOSE_WIDTH = 14;
    private static final int IO_CLOSE_HEIGHT = 12;
    private static final int IO_OUTPUT_X = 10;
    private static final int IO_OUTPUT_Y = 28;
    private static final int IO_OUTPUT_WIDTH = 164;
    private static final int IO_OUTPUT_NARROW_WIDTH = 110;
    private static final int IO_OUTPUT_HEIGHT = 14;
    private static final int IO_PREVIEW_X = 128;
    private static final int IO_PREVIEW_Y = 28;
    private static final int IO_PREVIEW_WIDTH = 47;
    private static final int IO_PREVIEW_HEIGHT = 14;
    private static final int IO_RATE_LABEL_X = 11;
    private static final int IO_RATE_LABEL_Y = 48;
    private static final int IO_RATE_FIELD_X = 64;
    private static final int IO_RATE_FIELD_Y = 48;
    private static final int IO_RATE_FIELD_WIDTH = 80;
    private static final int IO_RATE_FIELD_HEIGHT = 9;
    private static final int IO_RATE_APPLY_X = 150;
    private static final int IO_RATE_APPLY_Y = 45;
    private static final int IO_RANGE_LABEL_X = 11;
    private static final int IO_RANGE_LABEL_Y = 62;
    private static final int IO_RANGE_FIELD_X = 64;
    private static final int IO_RANGE_FIELD_Y = 62;
    private static final int IO_RANGE_FIELD_WIDTH = 80;
    private static final int IO_RANGE_FIELD_HEIGHT = 9;
    private static final int IO_RANGE_APPLY_X = 150;
    private static final int IO_RANGE_APPLY_Y = 61;
    private static final int IO_APPLY_WIDTH = 19;
    private static final int IO_APPLY_HEIGHT = 13;
    private static final int IO_SIDE_WIDTH = 52;
    private static final int IO_SIDE_HEIGHT = 10;
    private static final int[] IO_SIDE_COLUMN_XS = {10, 66, 122};
    private static final int[] IO_SIDE_ROW_YS = {76, 89};
    private static final EnumFacing[][] IO_SIDE_LAYOUT = {
            {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH},
            {EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST}
    };

    protected static final int GOLD = 0xFFE7C86A;
    protected static final int GOLD_DARK = 0xFF7A5A25;
    protected static final int TEXT = 0xFFCFC3D9;
    protected static final int GREEN = 0xFF79D49A;
    protected static final int BUTTON_BASE = 0xFF1B2A2A;
    protected static final int BUTTON_HOVER = 0xFF2F5452;
    protected static final int BUTTON_ON = 0xFF25413F;

    protected final AbstractArcaneMachineContainer container;
    private boolean ioPage;
    private GuiTextField outputRateField;
    private GuiTextField refillRadiusField;

    protected AbstractArcaneMachineGui(AbstractArcaneMachineContainer container,
                                       InventoryPlayer playerInventory) {
        super(container);
        this.container = container;
        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;
    }

    /** Main page background texture. */
    protected abstract ResourceLocation getMachineTexture();

    /** Title translation key. */
    protected abstract String getTitleKey();

    /** Hint translation key shown while no sample is locked. */
    protected abstract String getHintKey();

    /** Rate label translation key. */
    protected abstract String getRateLabelKey();

    /** Rate field tooltip translation key. */
    protected abstract String getRateTooltipKey();

    /** Whether the I/O page shows the jar refill radius row. */
    protected boolean hasRangeRow() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.outputRateField = new GuiTextField(0, this.fontRenderer,
                this.guiLeft + IO_RATE_FIELD_X, this.guiTop + IO_RATE_FIELD_Y,
                IO_RATE_FIELD_WIDTH, IO_RATE_FIELD_HEIGHT);
        this.outputRateField.setMaxStringLength(16);
        this.outputRateField.setText(Integer.toString(this.container.getOutputItemsPerTick()));
        this.outputRateField.setTextColor(TEXT);
        this.outputRateField.setEnableBackgroundDrawing(false);
        this.outputRateField.setVisible(this.ioPage);

        if (hasRangeRow()) {
            this.refillRadiusField = new GuiTextField(0, this.fontRenderer,
                    this.guiLeft + IO_RANGE_FIELD_X, this.guiTop + IO_RANGE_FIELD_Y,
                    IO_RANGE_FIELD_WIDTH, IO_RANGE_FIELD_HEIGHT);
            this.refillRadiusField.setMaxStringLength(8);
            this.refillRadiusField.setText(Integer.toString(this.container.getRefillRadius()));
            this.refillRadiusField.setTextColor(TEXT);
            this.refillRadiusField.setEnableBackgroundDrawing(false);
            this.refillRadiusField.setVisible(this.ioPage);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.ioPage ? IO_TEXTURE : getMachineTexture());
        drawModalRectWithCustomSizedTexture(this.guiLeft, this.guiTop, 0, 0,
                this.xSize, this.ySize, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (!this.ioPage) {
            this.drawCenteredString(this.fontRenderer, I18n.format(getTitleKey()),
                    GUI_WIDTH / 2, 8, GOLD);
            this.fontRenderer.drawString(I18n.format("container.inventory"), 12, 104, GOLD);
            drawMachineInfo();
            drawMainButtons(mouseX, mouseY);
        } else {
            this.fontRenderer.drawString(I18n.format("gui.infwhat.io.title"), 20, 8, GOLD);
            drawIoPage(mouseX, mouseY);
        }
    }

    private void drawMachineInfo() {
        Aspect aspect = this.container.getTileEntity().getLockedAspect();
        if (aspect == null) {
            this.fontRenderer.drawString(I18n.format("gui.infwhat.arcane.unlocked"), 44, 44, GOLD);
            this.fontRenderer.drawString(I18n.format(getHintKey()), 44, 56, TEXT);
            if (this.container.isInfinite()) {
                this.drawCenteredString(this.fontRenderer, I18n.format(
                        "gui.infwhat.arcane.status.waiting"), GUI_WIDTH / 2, 90, GREEN);
            }
            return;
        }
        drawAspectIcon(aspect, 13, 38, 24);
        this.fontRenderer.drawString(aspect.getName(), 44, 44, GOLD);
        String tag = aspect.getTag();
        this.fontRenderer.drawString(tag == null ? "" : tag, 44, 56, TEXT);
        String status = this.container.isInfinite()
                ? I18n.format("gui.infwhat.arcane.status.infinite")
                : I18n.format("gui.infwhat.arcane.status.stopped");
        this.drawCenteredString(this.fontRenderer, status, GUI_WIDTH / 2, 90,
                this.container.isInfinite() ? GREEN : TEXT);
    }

    private void drawAspectIcon(Aspect aspect, int x, int y, int size) {
        ResourceLocation image = aspect.getImage();
        if (image == null) {
            return;
        }
        this.mc.getTextureManager().bindTexture(image);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + size, this.zLevel).tex(0.0D, 1.0D).endVertex();
        buffer.pos(x + size, y + size, this.zLevel).tex(1.0D, 1.0D).endVertex();
        buffer.pos(x + size, y, this.zLevel).tex(1.0D, 0.0D).endVertex();
        buffer.pos(x, y, this.zLevel).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
    }

    private void drawMainButtons(int mouseX, int mouseY) {
        drawButton(INFINITE_X, BUTTON_Y, INFINITE_WIDTH, BUTTON_HEIGHT,
                I18n.format("gui.infwhat.arcane.infinite."
                        + (this.container.isInfinite() ? "on" : "off")),
                this.container.isInfinite(),
                isHovering(INFINITE_X, BUTTON_Y, INFINITE_WIDTH, BUTTON_HEIGHT, mouseX, mouseY));
        drawButton(IO_X, BUTTON_Y, IO_WIDTH, BUTTON_HEIGHT,
                I18n.format("gui.infwhat.items.io.open"), false,
                isHovering(IO_X, BUTTON_Y, IO_WIDTH, BUTTON_HEIGHT, mouseX, mouseY));
    }

    private void drawIoPage(int mouseX, int mouseY) {
        drawFieldBackground(IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_FIELD_WIDTH, IO_RATE_FIELD_HEIGHT);
        if (hasRangeRow()) {
            drawFieldBackground(IO_RANGE_FIELD_X, IO_RANGE_FIELD_Y,
                    IO_RANGE_FIELD_WIDTH, IO_RANGE_FIELD_HEIGHT);
        }
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, "\u00d7", false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        int outputWidth = hasRangeRow() ? IO_OUTPUT_NARROW_WIDTH : IO_OUTPUT_WIDTH;
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, outputWidth, IO_OUTPUT_HEIGHT,
                I18n.format("gui.infwhat.items.io.output."
                        + (this.container.isOutputEnabled() ? "on" : "off")),
                this.container.isOutputEnabled(),
                isHovering(IO_OUTPUT_X, IO_OUTPUT_Y, outputWidth, IO_OUTPUT_HEIGHT,
                        mouseX, mouseY));
        if (hasRangeRow()) {
            boolean preview = this.container.isRangePreviewEnabled();
            drawButton(IO_PREVIEW_X, IO_PREVIEW_Y, IO_PREVIEW_WIDTH, IO_PREVIEW_HEIGHT,
                    I18n.format("gui.infwhat.arcane.io.preview."
                            + (preview ? "on" : "off")),
                    preview, isHovering(IO_PREVIEW_X, IO_PREVIEW_Y, IO_PREVIEW_WIDTH,
                            IO_PREVIEW_HEIGHT, mouseX, mouseY));
        }

        this.fontRenderer.drawString(I18n.format(getRateLabelKey()),
                IO_RATE_LABEL_X, IO_RATE_LABEL_Y, GOLD);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_APPLY_WIDTH, IO_APPLY_HEIGHT,
                "\u2713", false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_APPLY_WIDTH, IO_APPLY_HEIGHT, mouseX, mouseY));

        if (hasRangeRow()) {
            this.fontRenderer.drawString(I18n.format("gui.infwhat.arcane.io.range"),
                    IO_RANGE_LABEL_X, IO_RANGE_LABEL_Y, GOLD);
            drawButton(IO_RANGE_APPLY_X, IO_RANGE_APPLY_Y, IO_APPLY_WIDTH, IO_APPLY_HEIGHT,
                    "\u2713", false, isHovering(IO_RANGE_APPLY_X, IO_RANGE_APPLY_Y,
                            IO_APPLY_WIDTH, IO_APPLY_HEIGHT, mouseX, mouseY));
        }

        for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
            for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                EnumFacing direction = IO_SIDE_LAYOUT[row][column];
                int x = IO_SIDE_COLUMN_XS[column];
                int y = IO_SIDE_ROW_YS[row];
                boolean enabled = this.container.isOutputSideEnabled(direction);
                drawButton(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                        I18n.format("gui.infwhat.arcane.io.side."
                                + direction.name().toLowerCase(Locale.ROOT)),
                        enabled, isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY));
            }
        }
    }

    private void drawButton(int x, int y, int width, int height, String label,
                            boolean enabled, boolean hovered) {
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, GOLD_DARK);
        drawRect(x, y, x + width, y + height,
                hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_BASE));
        drawRect(x + 1, y + 1, x + width - 1, y + 2, GOLD_DARK);
        this.drawCenteredString(this.fontRenderer, label, x + width / 2,
                y + (height - 8) / 2, GOLD);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int x = mouseX - this.guiLeft;
        int y = mouseY - this.guiTop;
        if (mouseButton == 0) {
            if (!this.ioPage) {
                if (isInside(x, y, INFINITE_X, BUTTON_Y, INFINITE_WIDTH, BUTTON_HEIGHT)) {
                    playButtonSound();
                    this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                            AbstractArcaneMachineContainer.BUTTON_TOGGLE_INFINITE);
                    return;
                }
                if (isInside(x, y, IO_X, BUTTON_Y, IO_WIDTH, BUTTON_HEIGHT)) {
                    playButtonSound();
                    openIoPage();
                    return;
                }
            } else {
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    closeIoPage();
                    return;
                }
                int outputWidth = hasRangeRow() ? IO_OUTPUT_NARROW_WIDTH : IO_OUTPUT_WIDTH;
                if (isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                        outputWidth, IO_OUTPUT_HEIGHT)) {
                    playButtonSound();
                    this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                            AbstractArcaneMachineContainer.BUTTON_TOGGLE_OUTPUT);
                    return;
                }
                if (hasRangeRow() && isInside(x, y, IO_PREVIEW_X, IO_PREVIEW_Y,
                        IO_PREVIEW_WIDTH, IO_PREVIEW_HEIGHT)) {
                    playButtonSound();
                    this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                            AbstractArcaneMachineContainer.BUTTON_TOGGLE_PREVIEW);
                    return;
                }
                if (isInside(x, y, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_APPLY_WIDTH, IO_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitOutputRate();
                    return;
                }
                if (hasRangeRow() && isInside(x, y, IO_RANGE_APPLY_X, IO_RANGE_APPLY_Y,
                        IO_APPLY_WIDTH, IO_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitRefillRadius();
                    return;
                }
                for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
                    for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                        int sideX = IO_SIDE_COLUMN_XS[column];
                        int sideY = IO_SIDE_ROW_YS[row];
                        if (isInside(x, y, sideX, sideY, IO_SIDE_WIDTH, IO_SIDE_HEIGHT)) {
                            playButtonSound();
                            this.mc.playerController.sendEnchantPacket(
                                    this.inventorySlots.windowId,
                                    AbstractArcaneMachineContainer.BUTTON_TOGGLE_SIDE_BASE
                                            + IO_SIDE_LAYOUT[row][column].ordinal());
                            return;
                        }
                    }
                }
            }
        }

        if (this.ioPage && isInside(x, y, IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_FIELD_WIDTH, IO_RATE_FIELD_HEIGHT)) {
            this.outputRateField.mouseClicked(mouseX, mouseY, mouseButton);
            if (hasRangeRow()) {
                this.refillRadiusField.setFocused(false);
            }
            return;
        }
        if (this.ioPage && hasRangeRow() && isInside(x, y, IO_RANGE_FIELD_X, IO_RANGE_FIELD_Y,
                IO_RANGE_FIELD_WIDTH, IO_RANGE_FIELD_HEIGHT)) {
            this.refillRadiusField.mouseClicked(mouseX, mouseY, mouseButton);
            this.outputRateField.setFocused(false);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void openIoPage() {
        this.ioPage = true;
        this.outputRateField.setText(Integer.toString(this.container.getOutputItemsPerTick()));
        this.outputRateField.setVisible(true);
        if (hasRangeRow()) {
            this.refillRadiusField.setText(Integer.toString(this.container.getRefillRadius()));
            this.refillRadiusField.setVisible(true);
        }
        this.container.setMachineSlotsVisible(false);
    }

    private void closeIoPage() {
        this.ioPage = false;
        this.outputRateField.setVisible(false);
        this.outputRateField.setFocused(false);
        if (hasRangeRow()) {
            this.refillRadiusField.setVisible(false);
            this.refillRadiusField.setFocused(false);
        }
        this.container.setMachineSlotsVisible(true);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.ioPage && keyCode == Keyboard.KEY_ESCAPE) {
            closeIoPage();
            return;
        }
        if (this.ioPage && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)) {
            playButtonSound();
            if (hasRangeRow() && this.refillRadiusField.isFocused()) {
                submitRefillRadius();
            } else {
                submitOutputRate();
            }
            return;
        }
        if (this.ioPage && this.outputRateField.isFocused()
                && this.outputRateField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (this.ioPage && hasRangeRow() && this.refillRadiusField.isFocused()
                && this.refillRadiusField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    /** Validates the rate field and sends it to the server through the container button channel. */
    private void submitOutputRate() {
        try {
            int amount = Integer.parseInt(this.outputRateField.getText().trim());
            if (amount < 1 || amount > AbstractArcaneMachineTileEntity.MAX_OUTPUT_RATE) {
                throw new NumberFormatException();
            }
            this.outputRateField.setText(Integer.toString(amount));
            this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                    AbstractArcaneMachineContainer.BUTTON_SET_RATE_BASE + amount);
        } catch (NumberFormatException exception) {
            this.outputRateField.setText("");
            this.mc.player.sendMessage(new TextComponentTranslation(
                    "gui.infwhat.arcane.io.rate.invalid")
                    .setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }

    /** Validates the jar radius field, sends it to the server and refreshes the world preview. */
    private void submitRefillRadius() {
        try {
            int radius = Integer.parseInt(this.refillRadiusField.getText().trim());
            int maximum = this.container.getTileEntity().getMaxRefillRadius();
            if (radius < 1 || radius > maximum) {
                throw new NumberFormatException();
            }
            this.refillRadiusField.setText(Integer.toString(radius));
            this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                    AbstractArcaneMachineContainer.BUTTON_SET_RANGE_BASE + radius);
        } catch (NumberFormatException exception) {
            this.refillRadiusField.setText("");
            this.mc.player.sendMessage(new TextComponentTranslation(
                    "gui.infwhat.arcane.io.range.invalid")
                    .setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.ioPage) {
            this.outputRateField.drawTextBox();
            if (hasRangeRow()) {
                this.refillRadiusField.drawTextBox();
            }
        }
        renderControlTooltip(mouseX, mouseY);
    }

    /** Draws a Thaumcraft-styled inset frame around a text field. */
    private void drawFieldBackground(int x, int y, int width, int height) {
        drawRect(x - 3, y - 3, x + width + 3, y + height + 3, GOLD_DARK);
        drawRect(x - 2, y - 2, x + width + 2, y + height + 2, 0xFF120A18);
    }

    private void renderControlTooltip(int mouseX, int mouseY) {
        int x = mouseX - this.guiLeft;
        int y = mouseY - this.guiTop;
        String tooltip = null;
        if (!this.ioPage && isInside(x, y, INFINITE_X, BUTTON_Y, INFINITE_WIDTH, BUTTON_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.arcane.infinite.tooltip");
        } else if (!this.ioPage && isInside(x, y, IO_X, BUTTON_Y, IO_WIDTH, BUTTON_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.items.io.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                (hasRangeRow() ? IO_OUTPUT_NARROW_WIDTH : IO_OUTPUT_WIDTH), IO_OUTPUT_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.items.io.output.tooltip");
        } else if (this.ioPage && hasRangeRow()
                && isInside(x, y, IO_PREVIEW_X, IO_PREVIEW_Y,
                IO_PREVIEW_WIDTH, IO_PREVIEW_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.arcane.io.preview.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_APPLY_X + IO_APPLY_WIDTH - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = I18n.format(getRateTooltipKey());
        } else if (this.ioPage && hasRangeRow()
                && isInside(x, y, IO_RANGE_FIELD_X, IO_RANGE_FIELD_Y,
                IO_RANGE_APPLY_X + IO_APPLY_WIDTH - IO_RANGE_FIELD_X, IO_RANGE_FIELD_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.arcane.io.range.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y,
                IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.items.io.back.tooltip");
        }
        if (tooltip != null) {
            this.drawHoveringText(Collections.singletonList(tooltip), mouseX, mouseY);
        }
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return isInside(mouseX - this.guiLeft, mouseY - this.guiTop, x, y, width, height);
    }

    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private void playButtonSound() {
        this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
                net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
