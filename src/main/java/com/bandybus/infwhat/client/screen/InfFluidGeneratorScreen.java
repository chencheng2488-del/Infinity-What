package com.bandybus.infwhat.client.screen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.network.ModNetworking;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.material.FluidState;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;

/** Fluid generator main page and its active-output I/O settings page. */
public class InfFluidGeneratorScreen extends AbstractContainerScreen<InfFluidGeneratorMenu> {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(InfinityWhat.MOD_ID,
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
    private EditBox outputRateField;

    public InfFluidGeneratorScreen(InfFluidGeneratorMenu menu, Inventory inventory, Component title) {
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
        this.outputRateField = new EditBox(this.font,
                this.leftPos + IO_RATE_FIELD_X, this.topPos + IO_RATE_FIELD_Y,
                IO_RATE_FIELD_WIDTH, IO_RATE_FIELD_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.input"));
        this.outputRateField.setMaxLength(32);
        this.outputRateField.setValue(formatOutputRate(this.menu.getOutputAmountPerTick()));
        this.outputRateField.setTextColor(BUTTON_TEXT);
        this.outputRateField.setBordered(false);
        this.outputRateField.setVisible(this.ioPage);
        this.addWidget(this.outputRateField);
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        if (this.ioPage) {
            RenderSystem.setShaderTexture(0, IO_TEXTURE);
            GuiComponent.blit(pose, this.leftPos, this.topPos, 0, 0.0F, 0.0F,
                    GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            RenderSystem.setShaderTexture(0, TEXTURE);
            GuiComponent.blit(pose, this.leftPos, this.topPos, 0, 0.0F, 0.0F,
                    this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        if (!this.ioPage) {
            renderFluid(pose);
        }
    }

    private void renderFluid(PoseStack pose) {
        FluidVariant fluid = this.menu.getFluidVariant();
        if (fluid.isBlank()) {
            return;
        }

        FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid.getFluid());
        if (handler == null) {
            return;
        }
        FluidState state = fluid.getFluid().defaultFluidState();
        TextureAtlasSprite[] sprites = handler.getFluidSprites(this.minecraft.level, null, state);
        if (sprites == null || sprites.length == 0 || sprites[0] == null) {
            return;
        }
        TextureAtlasSprite sprite = sprites[0];

        int fill = Math.round(InfFluidGeneratorMenu.TANK_HEIGHT * this.menu.getFillRatio());
        if (fill <= 0) {
            return;
        }

        int tint = handler.getFluidColor(this.minecraft.level, null, state);
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
        int tile = Math.max(1, sprite.contents().height());

        // Render the fluid at native texel scale and clip it to the framed tank interior.
        // GuiComponent.blit(sprite, ...) 不会自动绑定图集，沿用上一张 GUI 纹理会采到暗色像素。
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        GuiComponent.enableScissor(x, yTop, x + InfFluidGeneratorMenu.TANK_WIDTH, yBottom);
        for (int drawn = 0; drawn < fill; drawn += tile) {
            int tileY = yBottom - drawn - tile;
            GuiComponent.blit(pose, x, tileY, 0, InfFluidGeneratorMenu.TANK_WIDTH, tile,
                    sprite, red, green, blue, alpha);
        }
        GuiComponent.disableScissor();
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        if (!this.ioPage) {
            this.font.draw(pose, this.title, this.titleLabelX, this.titleLabelY, COLOR_TITLE);
        }
        this.font.draw(pose, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, COLOR_TITLE);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        // Keep JEI, FTB and other third-party controls in their original screen coordinates.
        super.render(pose, mouseX, mouseY, partialTick);

        if (this.ioPage) {
            renderIoPage(pose, mouseX, mouseY);
            this.outputRateField.render(pose, mouseX, mouseY, partialTick);
        } else {
            renderFluidInfo(pose);
            renderMainButtons(pose, mouseX, mouseY);
        }

        this.renderTooltip(pose, mouseX, mouseY);
        renderControlTooltip(pose, mouseX, mouseY);
    }

    private void renderFluidInfo(PoseStack pose) {
        FluidVariant fluid = this.menu.getFluidVariant();
        if (fluid.isBlank()) {
            this.font.draw(pose,
                    Component.translatable("gui." + InfinityWhat.MOD_ID + ".empty"),
                    this.leftPos + TEXT_X, this.topPos + 27, COLOR_TEXT);
            return;
        }

        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        String id = key == null ? "unknown" : key.toString();
        String amount;
        if (this.menu.isInfinite()) {
            amount = this.menu.getAmount() + " B";
        } else {
            amount = formatBuckets(this.menu.getAmount()) + " / "
                    + formatBuckets(InfinityWhatConfig.finiteCapacity()) + " B";
        }

        drawTruncated(pose,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid_name",
                        FluidVariantAttributes.getName(fluid)),
                21, COLOR_TITLE);
        drawTruncated(pose,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid_id", id),
                34, COLOR_TEXT);
        drawTruncated(pose,
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

    private void drawTruncated(PoseStack pose, Component text, int y, int color) {
        String value = text.getString();
        if (this.font.width(value) > TEXT_MAX_WIDTH) {
            value = this.font.plainSubstrByWidth(value, TEXT_MAX_WIDTH - this.font.width("...")) + "...";
        }
        this.font.draw(pose, value, this.leftPos + TEXT_X, this.topPos + y, color);
    }

    private void renderMainButtons(PoseStack pose, int mouseX, int mouseY) {
        drawButton(pose, MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.clear"), false,
                isHovering(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
        drawButton(pose, MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
    }

    private void renderIoPage(PoseStack pose, int mouseX, int mouseY) {
        this.font.draw(pose,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.title"),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);

        drawButton(pose, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.back"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(pose, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.output."
                        + (this.menu.isOutputEnabled() ? "on" : "off")),
                this.menu.isOutputEnabled(),
                isHovering(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));

        this.font.draw(pose,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.label"),
                this.leftPos + IO_RATE_LABEL_X, this.topPos + IO_RATE_LABEL_Y, BUTTON_TEXT);
        drawButton(pose, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, Component.literal("✓"), false,
                isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH,
                        IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
            for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                Direction direction = IO_SIDE_LAYOUT[row][column];
                int x = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                int y = IO_SIDE_ROW_YS[row];
                boolean enabled = this.menu.isOutputSideEnabled(direction);
                drawButton(pose, x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                        Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.side."
                                + direction.getName() + (enabled ? ".on" : ".off")),
                        enabled, isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY));
            }
        }
    }

    private void drawButton(PoseStack pose, int x, int y, int width, int height,
                            Component label, boolean enabled, boolean hovered) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        GuiComponent.fill(pose, absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF);
        GuiComponent.fill(pose, absoluteX, absoluteY, absoluteX + width, absoluteY + height, fill);
        GuiComponent.drawCenteredString(pose, this.font, label,
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
                // Unitless values are interpreted as millibuckets.
                amount = new BigDecimal(value).longValueExact();
            }
            if (amount < 1 || amount > InfFluidGeneratorBlockEntity.MAX_OUTPUT_RATE_MB) {
                throw new ArithmeticException("rate outside allowed range");
            }
            ModNetworking.sendOutputRate(this.menu.containerId, true, amount);
        } catch (NumberFormatException | ArithmeticException exception) {
            this.outputRateField.setValue("");
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_fluid")
                    .withStyle(ChatFormatting.RED), true);
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
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.saved")
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            this.outputRateField.setValue("");
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.rejected")
                    .withStyle(ChatFormatting.RED), true);
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

    private void renderControlTooltip(PoseStack pose, int mouseX, int mouseY) {
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
            this.renderTooltip(pose, tooltip, mouseX, mouseY);
        }
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return isInside(mouseX - this.leftPos, mouseY - this.topPos, x, y, width, height);
    }

    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
