package com.bandybus.infwhat.client.screen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.client.network.ClientRateRequestSender;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;

/** Fluid generator main page and its active-output I/O settings page. */
public class InfFluidGeneratorScreen extends AbstractContainerScreen<InfFluidGeneratorMenu> {

    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID,
                    "textures/gui/container/inf_fluid_generator.png");
    private static final Identifier IO_TEXTURE = Identifier.fromNamespaceAndPath(
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
    private EditBox outputRateField;

    public InfFluidGeneratorScreen(InfFluidGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.titleLabelX = 12;
        this.titleLabelY = 8;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = 86;
    }

    @Override
    protected void init() {
        super.init();
        this.outputRateField = new EditBox(this.font,
                this.leftPos + IO_RATE_FIELD_X, this.topPos + IO_RATE_FIELD_Y,
                IO_RATE_FIELD_WIDTH, IO_RATE_FIELD_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.input"));
        this.outputRateField.setMaxLength(32);
        this.outputRateField.setValue(formatOutputRate(this.menu.getOutputAmountPerTick()));
        this.outputRateField.setTextColor(BUTTON_TEXT);
        this.outputRateField.setBordered(false);
        this.outputRateField.setVisible(this.ioPage);
        this.addRenderableWidget(this.outputRateField);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (this.ioPage) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, IO_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                    GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                    this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        if (!this.ioPage) {
            renderFluid(graphics);
        }
    }

    private void renderFluid(GuiGraphicsExtractor graphics) {
        FluidStack stack = this.menu.getFluidStack();
        if (stack.isEmpty()) {
            return;
        }

        FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                .get(stack.getFluid().defaultFluidState());
        TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();

        int fill = Math.round(InfFluidGeneratorMenu.TANK_HEIGHT * this.menu.getFillRatio());
        if (fill <= 0) {
            return;
        }

        int tint = fluidModel.fluidTintSource() == null
                ? 0xFFFFFFFF : fluidModel.fluidTintSource().colorAsStack(stack);
        int x = this.leftPos + InfFluidGeneratorMenu.TANK_X;
        int yBottom = this.topPos + InfFluidGeneratorMenu.TANK_Y + InfFluidGeneratorMenu.TANK_HEIGHT;
        int yTop = yBottom - fill;
        int tile = Math.max(1, sprite.contents().height());

        // Render the fluid at native texel scale and clip it to the framed tank interior.
        graphics.enableScissor(x, yTop, x + InfFluidGeneratorMenu.TANK_WIDTH, yBottom);
        for (int drawn = 0; drawn < fill; drawn += tile) {
            int tileY = yBottom - drawn - tile;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, tileY,
                    InfFluidGeneratorMenu.TANK_WIDTH, tile, tint);
        }
        graphics.disableScissor();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.ioPage) {
            graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY,
                    COLOR_TITLE, false);
        }
        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, COLOR_TITLE, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep JEI, FTB and other third-party controls in their original screen coordinates.
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (this.ioPage) {
            renderIoPage(graphics, mouseX, mouseY);
        } else {
            renderFluidInfo(graphics);
            renderMainButtons(graphics, mouseX, mouseY);
        }

        renderControlTooltip(graphics, mouseX, mouseY);
    }

    private void renderFluidInfo(GuiGraphicsExtractor graphics) {
        FluidStack stack = this.menu.getFluidStack();
        if (stack.isEmpty()) {
            graphics.text(this.font,
                    Component.translatable("gui." + InfinityWhat.MOD_ID + ".empty"),
                    this.leftPos + TEXT_X, this.topPos + 27, COLOR_TEXT, false);
            return;
        }

        Identifier key = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        String id = key == null ? "unknown" : key.toString();
        String amount;
        if (this.menu.isInfinite()) {
            amount = this.menu.getAmount() + " B";
        } else {
            amount = formatBuckets(this.menu.getAmount()) + " / "
                    + formatBuckets(InfinityWhatConfig.finiteCapacity()) + " B";
        }

        drawTruncated(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid_name",
                        stack.getFluid().getFluidType().getDescription(stack)),
                21, COLOR_TITLE);
        drawTruncated(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid_id", id),
                34, COLOR_TEXT);
        drawTruncated(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".amount", amount),
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

    private void drawTruncated(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        String value = text.getString();
        if (this.font.width(value) > TEXT_MAX_WIDTH) {
            value = this.font.plainSubstrByWidth(value, TEXT_MAX_WIDTH - this.font.width("...")) + "...";
        }
        graphics.text(this.font, value, this.leftPos + TEXT_X, this.topPos + y, color, false);
    }

    private void renderMainButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawButton(graphics, MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.clear"), false,
                isHovering(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
        drawButton(graphics, MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
    }

    private void renderIoPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.title"),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT, false);

        drawButton(graphics, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.back"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(graphics, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.output."
                        + (this.menu.isOutputEnabled() ? "on" : "off")),
                this.menu.isOutputEnabled(),
                isHovering(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));

        graphics.text(this.font,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.label"),
                this.leftPos + IO_RATE_LABEL_X, this.topPos + IO_RATE_LABEL_Y, BUTTON_TEXT, false);
        drawButton(graphics, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, Component.literal("✓"), false,
                isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH,
                        IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
            for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                Direction direction = IO_SIDE_LAYOUT[row][column];
                int x = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                int y = IO_SIDE_ROW_YS[row];
                boolean enabled = this.menu.isOutputSideEnabled(direction);
                drawButton(graphics, x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                        Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.side."
                                + direction.getName() + (enabled ? ".on" : ".off")),
                        enabled, isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY));
            }
        }
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                            Component label, boolean enabled, boolean hovered) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        graphics.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF);
        graphics.fill(absoluteX, absoluteY, absoluteX + width, absoluteY + height, fill);
        graphics.text(this.font, label,
                absoluteX + (width - this.font.width(label)) / 2,
                absoluteY + (height - 8) / 2, BUTTON_TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (!this.ioPage && (button == 0 || button == 1) && isOverTank(mouseX, mouseY)) {
            this.minecraft.gameMode.handleContainerInput(this.menu.containerId,
                    InfFluidGeneratorMenu.FLUID_SLOT_INDEX, button, ContainerInput.PICKUP, this.minecraft.player);
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
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (this.ioPage && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            returnToMainPage();
            return true;
        }
        if (this.ioPage && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            playButtonSound();
            submitOutputRate();
            return true;
        }
        return super.keyPressed(event);
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
            ClientRateRequestSender.send(this.menu.containerId, true, amount);
        } catch (NumberFormatException | ArithmeticException exception) {
            this.outputRateField.setValue("");
            this.minecraft.gui.setOverlayMessage(Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_fluid")
                    .withStyle(ChatFormatting.RED), false);
        }
    }

    public int getContainerId() {
        return this.menu.containerId;
    }

    private void playButtonSound() {
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public void onOutputRateResult(boolean success, long amount) {
        if (success) {
            this.outputRateField.setValue(formatOutputRate(amount));
            this.minecraft.gui.setOverlayMessage(Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.saved")
                    .withStyle(ChatFormatting.GREEN), false);
        } else {
            this.outputRateField.setValue("");
            this.minecraft.gui.setOverlayMessage(Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.rejected")
                    .withStyle(ChatFormatting.RED), false);
        }
    }

    private void returnToMainPage() {
        this.ioPage = false;
        this.outputRateField.setVisible(false);
        this.outputRateField.setFocused(false);
    }

    private boolean isOverTank(double mouseX, double mouseY) {
        int x = this.leftPos + InfFluidGeneratorMenu.TANK_X;
        int y = this.topPos + InfFluidGeneratorMenu.TANK_Y;
        return mouseX >= x && mouseX < x + InfFluidGeneratorMenu.TANK_WIDTH
                && mouseY >= y && mouseY < y + InfFluidGeneratorMenu.TANK_HEIGHT;
    }

    private void renderControlTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        Component tooltip = null;
        if (!this.ioPage && isInside(x, y, MAIN_CLEAR_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.clear.tooltip");
        } else if (!this.ioPage && isInside(x, y, MAIN_IO_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.output.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_APPLY_X + IO_RATE_APPLY_WIDTH - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.fluid.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y,
                IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.back.tooltip");
        }
        if (tooltip != null) {
            graphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return isInside(mouseX - this.leftPos, mouseY - this.topPos, x, y, width, height);
    }

    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
