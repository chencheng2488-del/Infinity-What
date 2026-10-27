package com.bandybus.infwhat.client.gui;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import com.bandybus.infwhat.network.ModNetworking;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;

/** Fluid generator main page and its active-output I/O settings page. */
@SideOnly(Side.CLIENT)
public class GuiInfFluidGenerator extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_fluid_generator.png");
    private static final ResourceLocation IO_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_io.png");

    private static final int GUI_WIDTH = 184;
    private static final int GUI_HEIGHT = 178;

    private static final int TEXT_X = 44;
    private static final int TEXT_MAX_WIDTH = GUI_WIDTH - TEXT_X - 12;

    private static final int MAIN_CLEAR_X = 10;
    private static final int MAIN_CLEAR_Y = 68;
    private static final int MAIN_BUTTON_WIDTH = 80;
    private static final int MAIN_BUTTON_HEIGHT = 14;
    private static final int MAIN_IO_X = 94;

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

    private final InfFluidGeneratorContainer container;
    private boolean ioPage;
    private GuiTextField outputRateField;

    public GuiInfFluidGenerator(InfFluidGeneratorContainer container,
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
        this.outputRateField.setMaxStringLength(32);
        this.outputRateField.setText(formatOutputRate(
                this.container.getTileEntity().getOutputAmountPerTick()));
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
            renderFluid();
        }
    }

    /** Draws the stored fluid as a tinted, 16 pixel tall column inside the tank window. */
    private void renderFluid() {
        InfFluidGeneratorTileEntity tileEntity = this.container.getTileEntity();
        FluidStack stack = tileEntity.getStoredFluid();
        if (stack == null) {
            return;
        }
        int fill = Math.round(InfFluidGeneratorContainer.TANK_HEIGHT * tileEntity.getFillRatio());
        if (fill <= 0) {
            return;
        }
        IIcon icon = stack.getFluid().getStillIcon();
        if (icon == null) {
            icon = stack.getFluid().getIcon();
        }
        if (icon == null) {
            return;
        }
        int tint = stack.getFluid().getColor(stack);
        float alpha = ((tint >> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(red, green, blue, alpha);
        this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

        int x = this.guiLeft + InfFluidGeneratorContainer.TANK_X;
        int yBottom = this.guiTop + InfFluidGeneratorContainer.TANK_Y
                + InfFluidGeneratorContainer.TANK_HEIGHT;
        int remaining = fill;
        int drawn = 0;
        while (remaining > 0) {
            int height = Math.min(16, remaining);
            drawFluidQuad(x, yBottom - drawn - height, height, icon);
            drawn += height;
            remaining -= height;
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Draws one 16 pixel tall slice of the fluid icon, cropped from its bottom edge. */
    private void drawFluidQuad(int x, int y, int height, IIcon icon) {
        double minU = icon.getMinU();
        double maxU = icon.getMaxU();
        double maxV = icon.getMaxV();
        double minV = maxV - (height / 16.0D) * (icon.getMaxV() - icon.getMinV());

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, this.zLevel, minU, maxV);
        tessellator.addVertexWithUV(x + InfFluidGeneratorContainer.TANK_WIDTH, y + height,
                this.zLevel, maxU, maxV);
        tessellator.addVertexWithUV(x + InfFluidGeneratorContainer.TANK_WIDTH, y,
                this.zLevel, maxU, minV);
        tessellator.addVertexWithUV(x, y, this.zLevel, minU, minV);
        tessellator.draw();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (!this.ioPage) {
            this.fontRendererObj.drawString(StatCollector.translateToLocal(
                    "container.infwhat.inf_fluid_generator"), 12, 8, COLOR_TITLE);
            this.fontRendererObj.drawString(StatCollector.translateToLocal(
                    "container.inventory"), 12, 86, COLOR_TITLE);
            renderFluidInfo();
            renderMainButtons(mouseX, mouseY);
        } else {
            this.fontRendererObj.drawString(StatCollector.translateToLocal(
                    "gui.infwhat.fluid.io.title"), 12, 8, BUTTON_TEXT);
            renderIoPage(mouseX, mouseY);
        }
    }

    /** Draws the fluid name, id and amount column on the main page. */
    private void renderFluidInfo() {
        FluidStack stack = this.container.getTileEntity().getStoredFluid();
        if (stack == null) {
            this.fontRendererObj.drawString(StatCollector.translateToLocal(
                    "gui.infwhat.empty"), TEXT_X, 27, COLOR_TEXT);
            return;
        }

        String id = stack.getFluid().getName();
        if (id != null && !id.contains(":")) {
            id = "minecraft:" + id;
        }
        String amount;
        if (this.container.getTileEntity().isInfinite()) {
            amount = this.container.getTileEntity().getStoredAmount() + " B";
        } else {
            amount = formatBuckets(this.container.getTileEntity().getStoredAmount()) + " / "
                    + formatBuckets(InfinityWhatConfig.finiteCapacity()) + " B";
        }

        drawTruncated(StatCollector.translateToLocalFormatted("gui.infwhat.fluid_name",
                stack.getLocalizedName()), 21, COLOR_TITLE);
        drawTruncated(StatCollector.translateToLocalFormatted("gui.infwhat.fluid_id", id),
                34, COLOR_TEXT);
        drawTruncated(StatCollector.translateToLocalFormatted("gui.infwhat.amount", amount),
                47, COLOR_TEXT);
    }

    /** Renders millibuckets as plain buckets without trailing zeros. */
    private static String formatBuckets(long millibuckets) {
        return BigDecimal.valueOf(millibuckets)
                .divide(BigDecimal.valueOf(1000))
                .stripTrailingZeros()
                .toPlainString();
    }

    /** Renders a per-tick rate using the unit that keeps the value readable. */
    private static String formatOutputRate(long millibuckets) {
        if (millibuckets % 1000L == 0) {
            return formatBuckets(millibuckets) + " B";
        }
        return millibuckets + " mB";
    }

    /** Draws a line, cut down with an ellipsis when it would leave the text column. */
    private void drawTruncated(String value, int y, int color) {
        if (this.fontRendererObj.getStringWidth(value) > TEXT_MAX_WIDTH) {
            value = this.fontRendererObj.trimStringToWidth(value,
                    TEXT_MAX_WIDTH - this.fontRendererObj.getStringWidth("...")) + "...";
        }
        this.fontRendererObj.drawString(value, TEXT_X, y, color);
    }

    private void renderMainButtons(int mouseX, int mouseY) {
        drawButton(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                StatCollector.translateToLocal("gui.infwhat.fluid.clear"), false,
                isHovering(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                        mouseX, mouseY));
        drawButton(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                StatCollector.translateToLocal("gui.infwhat.fluid.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                        mouseX, mouseY));
    }

    private void renderIoPage(int mouseX, int mouseY) {
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                StatCollector.translateToLocal("gui.infwhat.fluid.io.back"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                        mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                StatCollector.translateToLocal("gui.infwhat.fluid.io.output."
                        + (this.container.getTileEntity().isOutputEnabled() ? "on" : "off")),
                this.container.getTileEntity().isOutputEnabled(),
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
                        StatCollector.translateToLocal("gui.infwhat.fluid.io.side."
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
        if (!this.ioPage && (mouseButton == 0 || mouseButton == 1) && isOverTank(mouseX, mouseY)) {
            this.mc.playerController.windowClick(this.inventorySlots.windowId,
                    InfFluidGeneratorContainer.FLUID_SLOT_INDEX, mouseButton, 0, this.mc.thePlayer);
            return;
        }

        int x = mouseX - this.guiLeft;
        int y = mouseY - this.guiTop;
        if (mouseButton == 0) {
            if (!this.ioPage) {
                if (isInside(x, y, MAIN_CLEAR_X, MAIN_CLEAR_Y,
                        MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
                    playButtonSound();
                    this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                            InfFluidGeneratorContainer.BUTTON_CLEAR_FLUID);
                    return;
                }
                if (isInside(x, y, MAIN_IO_X, MAIN_CLEAR_Y,
                        MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
                    playButtonSound();
                    this.ioPage = true;
                    this.outputRateField.setText(formatOutputRate(
                            this.container.getTileEntity().getOutputAmountPerTick()));
                    this.outputRateField.setVisible(true);
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
                            InfFluidGeneratorContainer.BUTTON_TOGGLE_OUTPUT);
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
                            int buttonId = InfFluidGeneratorContainer.BUTTON_TOGGLE_SIDE_BASE
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

    /** Parses the rate field and sends it to the server, reporting invalid input in chat. */
    private void submitOutputRate() {
        try {
            String value = this.outputRateField.getText().trim().toLowerCase(Locale.ROOT)
                    .replace(" ", "");
            long amount;
            if (value.endsWith("mb")) {
                amount = new BigDecimal(value.substring(0, value.length() - 2)).longValueExact();
            } else if (value.endsWith("b")) {
                amount = new BigDecimal(value.substring(0, value.length() - 1))
                        .multiply(BigDecimal.valueOf(1000)).longValueExact();
            } else {
                amount = new BigDecimal(value).longValueExact();
            }
            if (amount < 1 || amount > InfFluidGeneratorTileEntity.MAX_OUTPUT_RATE_MB) {
                throw new ArithmeticException("rate outside allowed range");
            }
            ModNetworking.sendOutputRate(this.inventorySlots.windowId, true, amount);
        } catch (NumberFormatException exception) {
            this.outputRateField.setText("");
            showRateError("gui.infwhat.io.rate.invalid_fluid");
        } catch (ArithmeticException exception) {
            this.outputRateField.setText("");
            showRateError("gui.infwhat.io.rate.invalid_fluid");
        }
    }

    /** Reports a rejected rate in the chat log. */
    private void showRateError(String translationKey) {
        this.mc.thePlayer.addChatComponentMessage(new ChatComponentTranslation(translationKey)
                .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
    }

    /** Applies the server acknowledgement to the rate field. */
    public void onOutputRateResult(int windowId, boolean success, long amount) {
        if (windowId != this.inventorySlots.windowId) {
            return;
        }
        if (success) {
            this.outputRateField.setText(formatOutputRate(amount));
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
        if (!this.ioPage && isInside(x, y, MAIN_CLEAR_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = StatCollector.translateToLocal("gui.infwhat.fluid.clear.tooltip");
        } else if (!this.ioPage && isInside(x, y, MAIN_IO_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = StatCollector.translateToLocal("gui.infwhat.fluid.io.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
            tooltip = StatCollector.translateToLocal("gui.infwhat.fluid.io.output.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_APPLY_X + IO_RATE_APPLY_WIDTH - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = StatCollector.translateToLocal("gui.infwhat.io.rate.fluid.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y,
                IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
            tooltip = StatCollector.translateToLocal("gui.infwhat.fluid.io.back.tooltip");
        }
        if (tooltip != null) {
            this.drawHoveringText(Collections.singletonList(tooltip), mouseX, mouseY);
        }
    }

    private boolean isOverTank(double mouseX, double mouseY) {
        int x = this.guiLeft + InfFluidGeneratorContainer.TANK_X;
        int y = this.guiTop + InfFluidGeneratorContainer.TANK_Y;
        return mouseX >= x && mouseX < x + InfFluidGeneratorContainer.TANK_WIDTH
                && mouseY >= y && mouseY < y + InfFluidGeneratorContainer.TANK_HEIGHT;
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
}
