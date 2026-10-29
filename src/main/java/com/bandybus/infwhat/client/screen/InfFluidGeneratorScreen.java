package com.bandybus.infwhat.client.screen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.network.ModNetworking;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;

/** Fluid generator main page and its active-output I/O settings page. */
public class InfFluidGeneratorScreen extends ContainerScreen<InfFluidGeneratorMenu> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(InfinityWhat.MOD_ID,
            "textures/gui/container/inf_fluid_generator.png");
    private static final ResourceLocation IO_TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_io.png");

    private static final int GUI_WIDTH = 184;
    private static final int GUI_HEIGHT = 178;
    private static final int TEXTURE_WIDTH = GUI_WIDTH * 2;
    private static final int TEXTURE_HEIGHT = GUI_HEIGHT;

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
    private static final Direction[][] IO_SIDE_LAYOUT = {
            {Direction.DOWN, Direction.UP},
            {Direction.NORTH, Direction.SOUTH},
            {Direction.WEST, Direction.EAST}
    };

    private static final int BUTTON_OFF = 0xFF303B42;
    private static final int BUTTON_ON = 0xFF256B43;
    private static final int BUTTON_HOVER = 0xFF456C83;
    private static final int BUTTON_BORDER = 0xFF070B0E;
    private static final int BUTTON_TEXT = 0xFFF0F5F7;
    private static final int COLOR_TITLE = 0xFFE9F1F4;
    private static final int COLOR_TEXT = 0xFF9FB8C2;

    private boolean ioPage;
    private TextFieldWidget outputRateField;

    public InfFluidGeneratorScreen(InfFluidGeneratorMenu menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.titleLabelX = 12;
        this.titleLabelY = 8;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = 86;
    }

    @Override
    protected void init() {
        super.init();
        this.outputRateField = new TextFieldWidget(this.font,
                this.leftPos + IO_RATE_FIELD_X, this.topPos + IO_RATE_FIELD_Y,
                IO_RATE_FIELD_WIDTH, IO_RATE_FIELD_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.input"));
        this.outputRateField.setMaxLength(32);
        this.outputRateField.setValue(formatOutputRate(this.menu.getOutputAmountPerTick()));
        this.outputRateField.setTextColor(BUTTON_TEXT);
        this.outputRateField.setBordered(false);
        this.outputRateField.setVisible(this.ioPage);
        this.addWidget(this.outputRateField);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.ioPage) {
            this.minecraft.getTextureManager().bind(IO_TEXTURE);
            AbstractGui.blit(matrixStack, this.leftPos, this.topPos, 0.0F, 0.0F,
                    GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            this.minecraft.getTextureManager().bind(TEXTURE);
            AbstractGui.blit(matrixStack, this.leftPos, this.topPos, 0.0F, 0.0F,
                    this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        if (!this.ioPage) {
            renderFluid(matrixStack);
        }
    }

    private void renderFluid(MatrixStack matrixStack) {
        FluidStack stack = this.menu.getFluidStack();
        if (stack.isEmpty()) {
            return;
        }

        // 1.16.5 exposes fluid appearance through FluidAttributes.
        FluidAttributes attributes = stack.getFluid().getAttributes();
        ResourceLocation stillTexture = attributes.getStillTexture(stack);
        if (stillTexture == null) {
            return;
        }

        int fill = Math.round(InfFluidGeneratorMenu.TANK_HEIGHT * this.menu.getFillRatio());
        if (fill <= 0) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(AtlasTexture.LOCATION_BLOCKS)
                .apply(stillTexture);
        int tint = attributes.getColor(stack);
        float alpha = ((tint >> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        int x = this.leftPos + InfFluidGeneratorMenu.TANK_X;
        int yBottom = this.topPos + InfFluidGeneratorMenu.TANK_Y + InfFluidGeneratorMenu.TANK_HEIGHT;
        int yTop = yBottom - fill;
        int tile = Math.max(1, sprite.getHeight());

        // Render the fluid at native texel scale and clip it to the framed tank interior.
        // 1.16.5's sprite blit has no per-call tint overload, so each tile is drawn as a
        // POSITION_TEX_COLOR quad with the fluid tint baked into the vertex colours.
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(AtlasTexture.LOCATION_BLOCKS);
        Matrix4f matrix = matrixStack.last().pose();
        enableScissor(x, yTop, x + InfFluidGeneratorMenu.TANK_WIDTH, yBottom);
        for (int drawn = 0; drawn < fill; drawn += tile) {
            int tileY = yBottom - drawn - tile;
            blitSpriteTinted(matrix, sprite, x, tileY,
                    InfFluidGeneratorMenu.TANK_WIDTH, tile, red, green, blue, alpha);
        }
        RenderSystem.disableScissor();
    }

    /** 1.16.5 has no tinted sprite blit; draw the sprite as a POSITION_TEX_COLOR quad. */
    private void blitSpriteTinted(Matrix4f matrix, TextureAtlasSprite sprite,
                                  int x, int y, int width, int height,
                                  float red, float green, float blue, float alpha) {
        BufferBuilder buffer = Tessellator.getInstance().getBuilder();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float z = this.getBlitOffset();
        // POSITION_TEX_COLOR 的元素顺序是 Position → UV0 → Color，调用顺序必须一致；
        // 写成 color 再 uv 会少填一个元素，endVertex 抛 "Not filled all elements of the vertex"。
        buffer.vertex(matrix, x, y + height, z)
                .uv(u0, v1).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + height, z)
                .uv(u1, v1).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y, z)
                .uv(u1, v0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y, z)
                .uv(u0, v0).color(red, green, blue, alpha).endVertex();
        Tessellator.getInstance().end();
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        if (!this.ioPage) {
            AbstractGui.drawString(matrixStack, this.font, this.title,
                    this.titleLabelX, this.titleLabelY, COLOR_TITLE);
        }
        AbstractGui.drawString(matrixStack, this.font, this.inventory.getDisplayName(),
                this.inventoryLabelX, this.inventoryLabelY, COLOR_TITLE);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(matrixStack);
        // Keep JEI, FTB and other third-party controls in their original screen coordinates.
        super.render(matrixStack, mouseX, mouseY, partialTick);

        if (this.ioPage) {
            renderIoPage(matrixStack, mouseX, mouseY);
            this.outputRateField.render(matrixStack, mouseX, mouseY, partialTick);
        } else {
            renderFluidInfo(matrixStack);
            renderMainButtons(matrixStack, mouseX, mouseY);
        }

        this.renderTooltip(matrixStack, mouseX, mouseY);
        renderControlTooltip(matrixStack, mouseX, mouseY);
    }

    private void renderFluidInfo(MatrixStack matrixStack) {
        FluidStack stack = this.menu.getFluidStack();
        if (stack.isEmpty()) {
            AbstractGui.drawString(matrixStack, this.font,
                    new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".empty"),
                    this.leftPos + TEXT_X, this.topPos + 27, COLOR_TEXT);
            return;
        }

        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        String id = key == null ? "unknown" : key.toString();
        String amount;
        if (this.menu.isInfinite()) {
            amount = this.menu.getAmount() + " B";
        } else {
            amount = formatBuckets(this.menu.getAmount()) + " / "
                    + formatBuckets(InfinityWhatConfig.finiteCapacity()) + " B";
        }

        drawTruncated(matrixStack,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid_name", stack.getDisplayName()),
                21, COLOR_TITLE);
        drawTruncated(matrixStack,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid_id", id),
                34, COLOR_TEXT);
        drawTruncated(matrixStack,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".amount", amount),
                47, COLOR_TEXT);
    }

    private String formatBuckets(long millibuckets) {
        return BigDecimal.valueOf(millibuckets)
                .divide(BigDecimal.valueOf(1_000))
                .stripTrailingZeros()
                .toPlainString();
    }

    private String formatOutputRate(long millibuckets) {
        if (millibuckets % 1_000L == 0) {
            return formatBuckets(millibuckets) + " B";
        }
        return millibuckets + " mB";
    }

    private void drawTruncated(MatrixStack matrixStack, ITextComponent text, int y, int color) {
        String value = text.getString();
        if (this.font.width(value) > TEXT_MAX_WIDTH) {
            value = this.font.plainSubstrByWidth(value, TEXT_MAX_WIDTH - this.font.width("...")) + "...";
        }
        AbstractGui.drawString(matrixStack, this.font, value, this.leftPos + TEXT_X, this.topPos + y, color);
    }

    private void renderMainButtons(MatrixStack matrixStack, int mouseX, int mouseY) {
        drawButton(matrixStack, MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.clear"), false,
                isHovering(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
        drawButton(matrixStack, MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
    }

    private void renderIoPage(MatrixStack matrixStack, int mouseX, int mouseY) {
        AbstractGui.drawString(matrixStack, this.font,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.title"),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);

        drawButton(matrixStack, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.back"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(matrixStack, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.output."
                        + (this.menu.isOutputEnabled() ? "on" : "off")),
                this.menu.isOutputEnabled(),
                isHovering(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));

        AbstractGui.drawString(matrixStack, this.font,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.label"),
                this.leftPos + IO_RATE_LABEL_X, this.topPos + IO_RATE_LABEL_Y, BUTTON_TEXT);
        drawButton(matrixStack, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, new StringTextComponent("\u2713"), false,
                isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH,
                        IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
            for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                Direction direction = IO_SIDE_LAYOUT[row][column];
                int x = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                int y = IO_SIDE_ROW_YS[row];
                boolean enabled = this.menu.isOutputSideEnabled(direction);
                drawButton(matrixStack, x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                        new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.side."
                                + direction.getName() + (enabled ? ".on" : ".off")),
                        enabled, isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY));
            }
        }
    }

    private void drawButton(MatrixStack matrixStack, int x, int y, int width, int height,
                            ITextComponent label, boolean enabled, boolean hovered) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        AbstractGui.fill(matrixStack, absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF);
        AbstractGui.fill(matrixStack, absoluteX, absoluteY, absoluteX + width, absoluteY + height, fill);
        AbstractGui.drawCenteredString(matrixStack, this.font, label,
                absoluteX + width / 2, absoluteY + (height - 8) / 2, BUTTON_TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.ioPage && (button == 0 || button == 1) && isOverTank(mouseX, mouseY)) {
            this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId,
                    InfFluidGeneratorMenu.FLUID_SLOT_INDEX, button, ClickType.PICKUP, this.minecraft.player);
            return true;
        }

        int x = (int) mouseX - this.leftPos;
        int y = (int) mouseY - this.topPos;
        if (button == 0) {
            if (!this.ioPage) {
                if (isInside(x, y, MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
                    playButtonSound();
                    this.minecraft.gameMode.handleInventoryButtonClick(
                            this.menu.containerId, InfFluidGeneratorMenu.BUTTON_CLEAR_FLUID);
                    return true;
                }
                if (isInside(x, y, MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
                    playButtonSound();
                    this.ioPage = true;
                    this.outputRateField.setValue(formatOutputRate(this.menu.getOutputAmountPerTick()));
                    this.outputRateField.setVisible(true);
                    return true;
                }
            } else {
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    returnToMainPage();
                    return true;
                }
                if (isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
                    playButtonSound();
                    this.minecraft.gameMode.handleInventoryButtonClick(
                            this.menu.containerId, InfFluidGeneratorMenu.BUTTON_TOGGLE_OUTPUT);
                    return true;
                }
                if (isInside(x, y, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitOutputRate();
                    return true;
                }
                for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
                    for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                        int sideX = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                        int sideY = IO_SIDE_ROW_YS[row];
                        if (isInside(x, y, sideX, sideY, IO_SIDE_WIDTH, IO_SIDE_HEIGHT)) {
                            playButtonSound();
                            int buttonId = InfFluidGeneratorMenu.BUTTON_TOGGLE_SIDE_BASE
                                    + IO_SIDE_LAYOUT[row][column].get3DDataValue();
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.ioPage && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            returnToMainPage();
            return true;
        }
        if (this.ioPage && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            playButtonSound();
            submitOutputRate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submitOutputRate() {
        try {
            String value = this.outputRateField.getValue().trim().toLowerCase(java.util.Locale.ROOT)
                    .replace(" ", "");
            long amount;
            if (value.endsWith("mb")) {
                amount = new BigDecimal(value.substring(0, value.length() - 2)).longValueExact();
            } else if (value.endsWith("b")) {
                amount = new BigDecimal(value.substring(0, value.length() - 1))
                        .multiply(BigDecimal.valueOf(1_000)).longValueExact();
            } else {
                // Unitless values are interpreted as Forge millibuckets.
                amount = new BigDecimal(value).longValueExact();
            }
            if (amount < 1 || amount > com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity.MAX_OUTPUT_RATE_MB) {
                throw new ArithmeticException("rate outside allowed range");
            }
            ModNetworking.sendOutputRate(this.menu.containerId, true, amount);
        } catch (NumberFormatException | ArithmeticException exception) {
            this.outputRateField.setValue("");
            this.minecraft.player.displayClientMessage(new TranslationTextComponent(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_fluid")
                    .withStyle(TextFormatting.RED), true);
        }
    }

    public int getContainerId() {
        return this.menu.containerId;
    }

    private void playButtonSound() {
        this.minecraft.getSoundManager().play(
                SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public void onOutputRateResult(boolean success, long amount) {
        if (success) {
            this.outputRateField.setValue(formatOutputRate(amount));
            this.minecraft.player.displayClientMessage(new TranslationTextComponent(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.saved")
                    .withStyle(TextFormatting.GREEN), true);
        } else {
            this.outputRateField.setValue("");
            this.minecraft.player.displayClientMessage(new TranslationTextComponent(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.rejected")
                    .withStyle(TextFormatting.RED), true);
        }
    }

    private void returnToMainPage() {
        this.ioPage = false;
        this.outputRateField.setVisible(false);
        this.outputRateField.setFocus(false);
    }

    /**
     * 1.16.5 has no {@code AbstractGui#enableScissor}; reproduce it here by converting the GUI
     * coordinates into the window-space rectangle {@link RenderSystem#enableScissor} expects.
     */
    private static void enableScissor(int x1, int y1, int x2, int y2) {
        MainWindow window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int sx = (int) (x1 * scale);
        int sy = (int) (window.getHeight() - y2 * scale);
        int sw = Math.max(0, (int) ((x2 - x1) * scale));
        int sh = Math.max(0, (int) ((y2 - y1) * scale));
        RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    private boolean isOverTank(double mouseX, double mouseY) {
        int x = this.leftPos + InfFluidGeneratorMenu.TANK_X;
        int y = this.topPos + InfFluidGeneratorMenu.TANK_Y;
        return mouseX >= x && mouseX < x + InfFluidGeneratorMenu.TANK_WIDTH
                && mouseY >= y && mouseY < y + InfFluidGeneratorMenu.TANK_HEIGHT;
    }

    private void renderControlTooltip(MatrixStack matrixStack, int mouseX, int mouseY) {
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        ITextComponent tooltip = null;
        if (!this.ioPage && isInside(x, y, MAIN_CLEAR_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.clear.tooltip");
        } else if (!this.ioPage && isInside(x, y, MAIN_IO_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.output.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_APPLY_X + IO_RATE_APPLY_WIDTH - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.fluid.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y,
                IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.back.tooltip");
        }
        if (tooltip != null) {
            this.renderTooltip(matrixStack, tooltip, mouseX, mouseY);
        }
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return isInside(mouseX - this.leftPos, mouseY - this.topPos, x, y, width, height);
    }

    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
