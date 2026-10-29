package com.bandybus.infwhat.client.screen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import com.bandybus.infwhat.network.ModNetworking;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Item generator main page and the shared active-output I/O page. */
public class InfItemGeneratorScreen extends AbstractContainerScreen<InfItemGeneratorMenu> {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_item_generator.png");
    public static final ResourceLocation IO_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_io.png");

    private static final int GUI_WIDTH = 184;
    private static final int GUI_HEIGHT = 178;
    private static final int MAIN_TEXTURE_WIDTH = GUI_WIDTH * 2;
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

    private boolean ioPage;
    private EditBox outputRateField;

    public InfItemGeneratorScreen(InfItemGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = 86;
        this.titleLabelX = 12;
        this.titleLabelY = 8;
    }

    @Override
    protected void init() {
        super.init();
        this.outputRateField = new EditBox(this.font,
                this.leftPos + IO_RATE_FIELD_X, this.topPos + IO_RATE_FIELD_Y,
                IO_RATE_FIELD_WIDTH, IO_RATE_FIELD_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.input"));
        this.outputRateField.setMaxLength(16);
        this.outputRateField.setValue(Integer.toString(this.menu.getOutputItemsPerTick()));
        this.outputRateField.setTextColor(BUTTON_TEXT);
        this.outputRateField.setBordered(false);
        this.outputRateField.setVisible(this.ioPage);
        this.addWidget(this.outputRateField);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.ioPage) {
            RenderSystem.setShaderTexture(0, IO_TEXTURE);
            GuiComponent.blit(poseStack, this.leftPos, this.topPos, 0.0F, 0.0F,
                    GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            RenderSystem.setShaderTexture(0, TEXTURE);
            GuiComponent.blit(poseStack, this.leftPos, this.topPos, 0.0F, 0.0F,
                    GUI_WIDTH, GUI_HEIGHT, MAIN_TEXTURE_WIDTH, GUI_HEIGHT);
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        if (!this.ioPage) {
            GuiComponent.drawString(poseStack, this.font, this.title,
                    this.titleLabelX, this.titleLabelY, 0xFFE9F1F4);
        }
        GuiComponent.drawString(poseStack, this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0xFFE9F1F4);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        // Keep JEI, FTB and other third-party controls in their original screen coordinates.
        super.render(poseStack, mouseX, mouseY, partialTick);
        if (this.ioPage) {
            renderIoPage(poseStack, mouseX, mouseY);
            this.outputRateField.render(poseStack, mouseX, mouseY, partialTick);
        } else {
            renderStoredInfo(poseStack);
            renderMainButtons(poseStack, mouseX, mouseY);
        }
        this.renderTooltip(poseStack, mouseX, mouseY);
        renderControlTooltip(poseStack, mouseX, mouseY);
    }

    private void renderMainButtons(PoseStack poseStack, int mouseX, int mouseY) {
        drawButton(poseStack, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.infinite_mode."
                        + (this.menu.isInfiniteModeEnabled() ? "on" : "off")),
                this.menu.isInfiniteModeEnabled(), isHovering(MAIN_INFINITE_X, MAIN_INFINITE_Y,
                        MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT, mouseX, mouseY));
        drawButton(poseStack, MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT, mouseX, mouseY));
    }

    private void renderIoPage(PoseStack poseStack, int mouseX, int mouseY) {
        GuiComponent.drawString(poseStack, this.font,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.title"),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);
        drawButton(poseStack, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.literal("×"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(poseStack, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.output."
                        + (this.menu.isOutputEnabled() ? "on" : "off")),
                this.menu.isOutputEnabled(), isHovering(IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));
        GuiComponent.drawString(poseStack, this.font,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.label"),
                this.leftPos + IO_RATE_LABEL_X, this.topPos + IO_RATE_LABEL_Y, BUTTON_TEXT);
        drawButton(poseStack, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                Component.literal("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
            for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                Direction direction = IO_SIDE_LAYOUT[row][column];
                int x = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                int y = IO_SIDE_ROW_YS[row];
                boolean enabled = this.menu.isOutputSideEnabled(direction);
                drawButton(poseStack, x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                        Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.side."
                                + direction.getName() + (enabled ? ".on" : ".off")),
                        enabled, isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY));
            }
        }
    }

    private void drawButton(PoseStack poseStack, int x, int y, int width, int height,
                            Component label, boolean enabled, boolean hovered) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        GuiComponent.fill(poseStack, absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF);
        GuiComponent.fill(poseStack, absoluteX, absoluteY, absoluteX + width, absoluteY + height, fill);
        GuiComponent.drawCenteredString(poseStack, this.font, label,
                absoluteX + width / 2, absoluteY + (height - 8) / 2, BUTTON_TEXT);
    }

    private void renderStoredInfo(PoseStack poseStack) {
        ItemStack stored = this.menu.getStoredItem();
        if (stored.isEmpty()) return;
        int maxWidth = GUI_WIDTH - 24;
        String name = truncate(stored.getHoverName().getString(), maxWidth);
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stored.getItem());
        String id = truncate(key.toString(), maxWidth);
        GuiComponent.drawCenteredString(poseStack, this.font, name, this.leftPos + GUI_WIDTH / 2,
                this.topPos + 24, 0xFFE9F1F4);
        GuiComponent.drawCenteredString(poseStack, this.font, id, this.leftPos + GUI_WIDTH / 2,
                this.topPos + 35, 0xFF9FB8C2);
    }

    private String truncate(String value, int maxWidth) {
        if (this.font.width(value) > maxWidth) {
            return this.font.plainSubstrByWidth(value, maxWidth - this.font.width("...")) + "...";
        }
        return value;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - this.leftPos;
        int y = (int) mouseY - this.topPos;
        if (button == 0) {
            if (!this.ioPage) {
                if (isInside(x, y, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                        MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT)) {
                    playButtonSound();
                    this.minecraft.gameMode.handleInventoryButtonClick(
                            this.menu.containerId, InfItemGeneratorMenu.BUTTON_TOGGLE_INFINITE);
                    return true;
                }
                if (isInside(x, y, MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT)) {
                    playButtonSound();
                    this.ioPage = true;
                    this.outputRateField.setValue(Integer.toString(this.menu.getOutputItemsPerTick()));
                    this.outputRateField.setVisible(true);
                    this.menu.setGeneratorSlotVisible(false);
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
                            this.menu.containerId, InfItemGeneratorMenu.BUTTON_TOGGLE_OUTPUT);
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
                            int buttonId = InfItemGeneratorMenu.BUTTON_TOGGLE_SIDE_BASE
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

    private void submitOutputRate() {
        try {
            long amount = Long.parseLong(this.outputRateField.getValue().trim());
            if (amount < 1 || amount > Integer.MAX_VALUE) throw new NumberFormatException();
            ModNetworking.sendOutputRate(this.menu.containerId, false, amount);
        } catch (NumberFormatException exception) {
            this.outputRateField.setValue("");
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_item")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    private void playButtonSound() {
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public int getContainerId() {
        return this.menu.containerId;
    }

    public void onOutputRateResult(boolean success, long amount) {
        if (success) {
            this.outputRateField.setValue(Long.toString(amount));
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

    private void returnToMainPage() {
        this.ioPage = false;
        this.outputRateField.setVisible(false);
        this.outputRateField.setFocused(false);
        this.menu.setGeneratorSlotVisible(true);
    }

    private void renderControlTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        Component tooltip = null;
        if (!this.ioPage && isInside(x, y, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.infinite_mode.tooltip");
        } else if (!this.ioPage && isInside(x, y, MAIN_IO_X, MAIN_IO_Y,
                MAIN_IO_WIDTH, MAIN_IO_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.output.tooltip");
        } else if (this.ioPage && isInside(x, y, IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_APPLY_X + IO_RATE_APPLY_WIDTH - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.item.tooltip");
        }
        if (tooltip != null) this.renderTooltip(poseStack, tooltip, mouseX, mouseY);
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return isInside(mouseX - this.leftPos, mouseY - this.topPos, x, y, width, height);
    }

    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
