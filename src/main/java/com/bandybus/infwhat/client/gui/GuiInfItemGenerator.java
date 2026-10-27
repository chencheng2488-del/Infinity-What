package com.bandybus.infwhat.client.gui;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.container.InfItemGeneratorContainer;
import com.bandybus.infwhat.network.ModNetworking;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.Locale;

/** Item generator main page and the shared active-output I/O page. */
@SideOnly(Side.CLIENT)
public class GuiInfItemGenerator extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_item_generator.png");
    private static final ResourceLocation IO_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_io.png");

    private static final int GUI_WIDTH = 184;
    private static final int GUI_HEIGHT = 178;

    private static final int MAIN_INFINITE_X = 9;
    private static final int MAIN_INFINITE_Y = 67;
    private static final int MAIN_INFINITE_WIDTH = 166;
    private static final int MAIN_INFINITE_HEIGHT = 17;
    private static final int MAIN_IO_X = 120;
    private static final int MAIN_IO_Y = 47;
    private static final int MAIN_IO_WIDTH = 56;
    private static final int MAIN_IO_HEIGHT = 16;

    private static final int IO_CLOSE_X = 160;
    private static final int IO_CLOSE_Y = 7;
    private static final int IO_CLOSE_WIDTH = 14;
    private static final int IO_CLOSE_HEIGHT = 12;
    private static final int IO_OUTPUT_X = 10;
    private static final int IO_OUTPUT_Y = 21;
    private static final int IO_OUTPUT_WIDTH = 164;
    private static final int IO_OUTPUT_HEIGHT = 13;
    private static final int IO_RATE_LABEL_X = 11;
    private static final int IO_RATE_LABEL_Y = 39;
    private static final int IO_RATE_FIELD_X = 55;
    private static final int IO_RATE_FIELD_Y = 39;
    private static final int IO_RATE_FIELD_WIDTH = 95;
    private static final int IO_RATE_FIELD_HEIGHT = 9;
    private static final int IO_RATE_APPLY_X = 155;
    private static final int IO_RATE_APPLY_Y = 37;
    private static final int IO_RATE_APPLY_WIDTH = 19;
    private static final int IO_RATE_APPLY_HEIGHT = 13;
    private static final int IO_SIDE_LEFT_X = 10;
    private static final int IO_SIDE_RIGHT_X = 94;
    private static final int IO_SIDE_WIDTH = 80;
    private static final int IO_SIDE_HEIGHT = 9;
    private static final int[] IO_SIDE_ROW_YS = {52, 63, 74};
    private static final ForgeDirection[][] IO_SIDE_LAYOUT = {
            {ForgeDirection.DOWN, ForgeDirection.UP},
            {ForgeDirection.NORTH, ForgeDirection.SOUTH},
            {ForgeDirection.WEST, ForgeDirection.EAST}
    };

    private static final int BUTTON_OFF = 0xFF303B42;
    private static final int BUTTON_ON = 0xFF256B43;
    private static final int BUTTON_HOVER = 0xFF456C83;
    private static final int BUTTON_BORDER = 0xFF070B0E;
    private static final int BUTTON_TEXT = 0xFFF0F5F7;
    private static final int COLOR_TITLE = 0xFFE9F1F4;
    private static final int COLOR_TEXT = 0xFF9FB8C2;

    private final InfItemGeneratorContainer container;
    private boolean ioPage;
    private GuiTextField outputRateField;

    public GuiInfItemGenerator(InfItemGeneratorContainer container,
                               InventoryPlayer playerInventory) {
        super(container);
        this.container = container;
        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.outputRateField = new GuiTextField(this.fontRendererObj,
                this.guiLeft + IO_RATE_FIELD_X, this.guiTop + IO_RATE_FIELD_Y,
                IO_RATE_FIELD_WIDTH, IO_RATE_FIELD_HEIGHT);
        this.outputRateField.setMaxStringLength(16);
        this.outputRateField.setText(Integer.toString(
                this.container.getTileEntity().getOutputItemsPerTick()));
        this.outputRateField.setTextColor(BUTTON_TEXT);
        this.outputRateField.setEnableBackgroundDrawing(false);
        this.outputRateField.setVisible(this.ioPage);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.ioPage) {
            this.mc.getTextureManager().bindTexture(IO_TEXTURE);
            drawModalRectWithCustomSizedTexture(this.guiLeft, this.guiTop, 0.0F, 0.0F,
                    this.xSize, this.ySize, 184.0F, 178.0F);
        } else {
            this.mc.getTextureManager().bindTexture(TEXTURE);
            drawModalRectWithCustomSizedTexture(this.guiLeft, this.guiTop, 0.0F, 0.0F,
                    this.xSize, this.ySize, 368.0F, 178.0F);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (!this.ioPage) {
            this.fontRendererObj.drawString(StatCollector.translateToLocal(
                    "container.infwhat.inf_item_generator"), 12, 8, COLOR_TITLE);
            this.fontRendererObj.drawString(StatCollector.translateToLocal(
                    "container.inventory"), 12, 86, COLOR_TITLE);
            renderStoredInfo();
            renderMainButtons(mouseX, mouseY);
        } else {
            this.fontRendererObj.drawString(StatCollector.translateToLocal(
                    "gui.infwhat.items.io.title"), 12, 8, BUTTON_TEXT);
            renderIoPage(mouseX, mouseY);
        }
    }

    /** Draws the stored item name and registry id over the slot. */
    private void renderStoredInfo() {
        ItemStack stored = this.container.getStoredItem();
        if (stored == null || stored.stackSize <= 0) {
            return;
        }
        int maxWidth = GUI_WIDTH - 24;
        String name = truncate(stored.getDisplayName(), maxWidth);
        String id = Item.itemRegistry.getNameForObject(stored.getItem());
        id = truncate(id == null ? "unknown" : id, maxWidth);
        this.drawCenteredString(this.fontRendererObj, name, GUI_WIDTH / 2, 24, COLOR_TITLE);
        this.drawCenteredString(this.fontRendererObj, id, GUI_WIDTH / 2, 35, COLOR_TEXT);
    }

    /** Shortens a string with an ellipsis when it would exceed the given pixel width. */
    private String truncate(String value, int maxWidth) {
        if (this.fontRendererObj.getStringWidth(value) > maxWidth) {
            return this.fontRendererObj.trimStringToWidth(value,
                    maxWidth - this.fontRendererObj.getStringWidth("...")) + "...";
        }
        return value;
    }

    private void renderMainButtons(int mouseX, int mouseY) {
        drawButton(MAIN_INFINITE_X, MAIN_INFINITE_Y, MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT,
                StatCollector.translateToLocal("gui.infwhat.items.infinite_mode."
                        + (this.container.isInfiniteModeEnabled() ? "on" : "off")),
                this.container.isInfiniteModeEnabled(),
                isHovering(MAIN_INFINITE_X, MAIN_INFINITE_Y, MAIN_INFINITE_WIDTH,
                        MAIN_INFINITE_HEIGHT, mouseX, mouseY));
        drawButton(MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT,
                StatCollector.translateToLocal("gui.infwhat.items.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT,
                        mouseX, mouseY));
    }

    private void renderIoPage(int mouseX, int mouseY) {
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, "\u00d7", false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                        mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                StatCollector.translateToLocal("gui.infwhat.items.io.output."
                        + (this.container.isOutputEnabled() ? "on" : "off")),
                this.container.isOutputEnabled(),
                isHovering(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                        mouseX, mouseY));

        this.fontRendererObj.drawString(StatCollector.translateToLocal(
                "gui.infwhat.io.rate.label"), IO_RATE_LABEL_X, IO_RATE_LABEL_Y, BUTTON_TEXT);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                "\u2713", false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
            for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                ForgeDirection direction = IO_SIDE_LAYOUT[row][column];
                int x = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                int y = IO_SIDE_ROW_YS[row];
                boolean enabled = this.container.isOutputSideEnabled(direction);
                drawButton(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                        StatCollector.translateToLocal("gui.infwhat.items.io.side."
                                + direction.name().toLowerCase(Locale.ROOT)
                                + (enabled ? ".on" : ".off")),
                        enabled, isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY));
            }
        }
    }

    /** Draws one flat coloured button with a one pixel border and a centred label. */
    private void drawButton(int x, int y, int width, int height, String label,
                            boolean enabled, boolean hovered) {
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, BUTTON_BORDER);
        drawRect(x, y, x + width, y + height,
                hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF));
        this.drawCenteredString(this.fontRendererObj, label, x + width / 2,
                y + (height - 8) / 2, BUTTON_TEXT);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int x = mouseX - this.guiLeft;
        int y = mouseY - this.guiTop;
        if (mouseButton == 0) {
            if (!this.ioPage) {
                if (isInside(x, y, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                        MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT)) {
                    playButtonSound();
                    this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                            InfItemGeneratorContainer.BUTTON_TOGGLE_INFINITE);
                    return;
                }
                if (isInside(x, y, MAIN_IO_X, MAIN_IO_Y,
                        MAIN_IO_WIDTH, MAIN_IO_HEIGHT)) {
                    playButtonSound();
                    this.ioPage = true;
                    this.outputRateField.setText(Integer.toString(
                            this.container.getTileEntity().getOutputItemsPerTick()));
                    this.outputRateField.setVisible(true);
                    this.container.setGeneratorSlotVisible(false);
                    return;
                }
            } else {
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    returnToMainPage();
                    return;
                }
                if (isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
                    playButtonSound();
                    this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                            InfItemGeneratorContainer.BUTTON_TOGGLE_OUTPUT);
                    return;
                }
                if (isInside(x, y, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitOutputRate();
                    return;
                }
                for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
                    for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                        int sideX = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                        int sideY = IO_SIDE_ROW_YS[row];
                        if (isInside(x, y, sideX, sideY, IO_SIDE_WIDTH, IO_SIDE_HEIGHT)) {
                            playButtonSound();
                            int buttonId = InfItemGeneratorContainer.BUTTON_TOGGLE_SIDE_BASE
                                    + IO_SIDE_LAYOUT[row][column].ordinal();
                            this.mc.playerController.sendEnchantPacket(
                                    this.inventorySlots.windowId, buttonId);
                            return;
                        }
                    }
                }
            }
        }

        if (this.ioPage && isInside(x, y, IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_FIELD_WIDTH, IO_RATE_FIELD_HEIGHT)) {
            this.outputRateField.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (this.ioPage && keyCode == Keyboard.KEY_ESCAPE) {
            returnToMainPage();
            return;
        }
        if (this.ioPage && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)) {
            playButtonSound();
            submitOutputRate();
            return;
        }
        if (this.ioPage && this.outputRateField.isFocused()
                && this.outputRateField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    /** Parses the integer rate field and sends it to the server. */
    private void submitOutputRate() {
        try {
            long amount = Long.parseLong(this.outputRateField.getText().trim());
            if (amount < 1 || amount > Integer.MAX_VALUE) {
                throw new NumberFormatException();
            }
            ModNetworking.sendOutputRate(this.inventorySlots.windowId, false, amount);
        } catch (NumberFormatException exception) {
            this.outputRateField.setText("");
            this.mc.thePlayer.addChatComponentMessage(new ChatComponentTranslation(
                    "gui.infwhat.io.rate.invalid_item")
                    .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
        }
    }

    /** Applies the server acknowledgement to the rate field. */
    public void onOutputRateResult(int windowId, boolean success, long amount) {
        if (windowId != this.inventorySlots.windowId) {
            return;
        }
        if (success) {
            this.outputRateField.setText(Long.toString(amount));
            this.mc.thePlayer.addChatComponentMessage(new ChatComponentTranslation(
                    "gui.infwhat.io.rate.saved")
                    .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
        } else {
            this.outputRateField.setText("");
            this.mc.thePlayer.addChatComponentMessage(new ChatComponentTranslation(
                    "gui.infwhat.io.rate.rejected")
                    .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
        }
    }

    private void returnToMainPage() {
        this.ioPage = false;
        this.outputRateField.setVisible(false);
        this.outputRateField.setFocused(false);
        this.container.setGeneratorSlotVisible(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.ioPage) {
            this.outputRateField.drawTextBox();
        }
        renderControlTooltip(mouseX, mouseY);
    }

    /** Shows the tooltip of whichever control sits under the cursor. */
    private void renderControlTooltip(int mouseX, int mouseY) {
        int x = mouseX - this.guiLeft;
        int y = mouseY - this.guiTop;
        String tooltip = null;
        if (!this.ioPage && isInside(x, y, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT)) {
            tooltip = StatCollector.translateToLocal("gui.infwhat.items.infinite_mode.tooltip");
        } else if (!this.ioPage && isInside(x, y, MAIN_IO_X, MAIN_IO_Y,
                MAIN_IO_WIDTH, MAIN_IO_HEIGHT)) {
            tooltip = StatCollector.translateToLocal("gui.infwhat.items.io.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
            tooltip = StatCollector.translateToLocal("gui.infwhat.items.io.output.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_APPLY_X + IO_RATE_APPLY_WIDTH - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = StatCollector.translateToLocal("gui.infwhat.io.rate.item.tooltip");
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
        this.mc.getSoundHandler().playSound(
                PositionedSoundRecord.createPositionedSoundRecord(
                        new ResourceLocation("gui.button.press"), 1.0F));
    }

    /** Machine tile entity shown by this screen. */
    public InfItemGeneratorTileEntity getTileEntity() {
        return this.container.getTileEntity();
    }
}
