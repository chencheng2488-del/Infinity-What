package com.bandybus.infwhat.client.screen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import com.bandybus.infwhat.network.ModNetworking;
import com.bandybus.infwhat.network.RateRejectReason;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.ChatFormatting;
import org.lwjgl.glfw.GLFW;

/** Item generator main page and the shared active-output I/O page. */
public class InfItemGeneratorScreen extends AbstractContainerScreen<InfItemGeneratorMenu> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_item_generator.png");
    public static final ResourceLocation IO_TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_io.png");
    public static final ResourceLocation FACE_TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_face.png");
    public static final ResourceLocation REDSTONE_TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_redstone.png");
    public static final ResourceLocation BATCH_TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_batch.png");

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
    private static final int IO_RATE_LABEL_BOX_LEFT = 12;
    private static final int IO_RATE_LABEL_BOX_RIGHT = 52;
    private static final int IO_RATE_LABEL_Y = 39;
    private static final int IO_RATE_FIELD_X = 58;
    private static final int IO_RATE_FIELD_RIGHT = 150;
    private static final int IO_RATE_FIELD_Y = 39;
    private static final int IO_RATE_FIELD_HEIGHT = 9;
    private static final int IO_RATE_APPLY_X = 155;
    private static final int IO_RATE_APPLY_Y = 37;
    private static final int IO_RATE_APPLY_WIDTH = 19;
    private static final int IO_RATE_APPLY_HEIGHT = 13;
    private static final int IO_MODE_X = 104;
    private static final int IO_MODE_Y = 7;
    private static final int IO_MODE_WIDTH = 50;
    private static final int IO_MODE_HEIGHT = 12;

    // Shared 3x2 face grid: simple mode toggles the face, advanced mode colors it and opens its
    // detail page on click.
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
    private static final Direction[] IO_SIDE_ORDER = Direction.values();

    private static final int DETAIL_BACK_X = 104;
    private static final int DETAIL_BACK_Y = 7;
    private static final int DETAIL_BACK_WIDTH = 50;
    private static final int DETAIL_BACK_HEIGHT = 12;
    private static final int DETAIL_RATE_LABEL_X = 14;
    private static final int DETAIL_RATE_X = 56;
    private static final int DETAIL_RATE_Y = 39;
    private static final int DETAIL_RATE_HEIGHT = 9;

    private static final int BUTTON_OFF = 0xFF303B42;
    private static final int BUTTON_ON = 0xFF256B43;
    private static final int BUTTON_HOVER = 0xFF456C83;
    private static final int FACE_ON_HOVER = 0xFF2E8050;
    private static final int FACE_OFF_HOVER = 0xFF3C4A52;
    private static final int BUTTON_BORDER = 0xFF070B0E;
    private static final int BUTTON_TEXT = 0xFFF0F5F7;

    // Persistent page tabs, drawn just above the GUI's top border so the accepted layout below
    // stays untouched.
    private static final int TAB_START_X = 8;
    private static final int TAB_TOP_Y = -13;
    private static final int TAB_HEIGHT = 13;
    private static final int TAB_GAP = 2;
    private static final int TAB_PADDING = 6;

    private static final int RS_MODE_Y = 21;
    private static final int RS_MODE_WIDTH = 41;
    private static final int RS_MODE_HEIGHT = 13;
    private static final int[] RS_MODE_X = {10, 51, 92, 133};
    private static final int RS_PULSE_ON_LABEL_Y = 39;
    private static final int RS_PULSE_OFF_LABEL_Y = 52;
    private static final int RS_PULSE_ON_FIELD_X = 58;
    private static final int RS_PULSE_ON_FIELD_RIGHT = 150;
    private static final int RS_PULSE_OFF_FIELD_X = 98;
    private static final int RS_PULSE_OFF_FIELD_RIGHT = 170;
    private static final int RS_PULSE_FIELD_HEIGHT = 9;
    private static final int RS_STATUS_Y = 65;
    private static final int RS_NOTE_Y = 78;
    private static final int RS_HELP_Y = 97;
    private static final int RS_HELP_LINE = 11;

    private static final int BATCH_LABEL_BOX_LEFT = 10;
    private static final int BATCH_LABEL_BOX_WIDTH = 42;
    private static final int BATCH_LABEL_Y = 39;
    private static final int BATCH_FIELD_X = 58;
    private static final int BATCH_FIELD_RIGHT = 150;
    private static final int BATCH_INFO_Y = 54;
    private static final int BATCH_NOTE_Y = 67;
    private static final int BATCH_HELP_Y = 97;
    private static final int BATCH_HELP_LINE = 11;

    private GeneratorPage page = GeneratorPage.MAIN;
    private boolean advancedMode;
    private int faceDetail = -1;
    private EditBox outputRateField;
    private final EditBox[] sideRateFields = new EditBox[IO_SIDE_ORDER.length];
    private EditBox pulseOnField;
    private EditBox pulseOffField;
    private EditBox batchSizeField;
    /** Field the player is currently typing in; protected from any server-driven refresh. */
    private EditBox editingField;
    private int refreshCooldown;
    private RateUnit rateUnit = RateUnit.PER_TICK;

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
                IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.input"));
        this.outputRateField.setMaxLength(16);
        this.outputRateField.setTextColor(BUTTON_TEXT);
        this.outputRateField.setBordered(false);
        this.outputRateField.setVisible(isIoPage());
        this.addWidget(this.outputRateField);

        for (int i = 0; i < this.sideRateFields.length; i++) {
            EditBox field = new EditBox(this.font,
                    this.leftPos + DETAIL_RATE_X, this.topPos + DETAIL_RATE_Y,
                    IO_RATE_FIELD_RIGHT - DETAIL_RATE_X, DETAIL_RATE_HEIGHT,
                    Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.side.rate.input"));
            field.setMaxLength(16);
            field.setTextColor(BUTTON_TEXT);
            field.setBordered(false);
            field.setVisible(false);
            this.sideRateFields[i] = field;
            this.addWidget(field);
        }
        this.pulseOnField = createPulseField(
                RS_PULSE_ON_FIELD_X, RS_PULSE_ON_FIELD_RIGHT, RS_PULSE_ON_LABEL_Y);
        this.pulseOffField = createPulseField(
                RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_FIELD_RIGHT, RS_PULSE_OFF_LABEL_Y);
        this.batchSizeField = new EditBox(this.font,
                this.leftPos + BATCH_FIELD_X, this.topPos + BATCH_LABEL_Y,
                BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".batch.input"));
        this.batchSizeField.setMaxLength(2);
        this.batchSizeField.setTextColor(BUTTON_TEXT);
        this.batchSizeField.setBordered(false);
        this.batchSizeField.setVisible(false);
        this.addWidget(this.batchSizeField);
        setPage(GeneratorPage.MAIN, -1);
    }

    private EditBox createPulseField(int x, int right, int y) {
        EditBox field = new EditBox(this.font,
                this.leftPos + x, this.topPos + y,
                right - x, RS_PULSE_FIELD_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.input"));
        field.setMaxLength(5);
        field.setTextColor(BUTTON_TEXT);
        field.setBordered(false);
        field.setVisible(false);
        this.addWidget(field);
        return field;
    }

    private void refreshOutputRateFields() {
        boolean mixed = this.menu.hasMixedSideRates();
        if (!this.outputRateField.isFocused() && this.outputRateField != this.editingField) {
            this.outputRateField.setValue(mixed ? "" : formatUnitRate(this.menu.getUniformSideRate()));
            this.outputRateField.setHint(mixed
                    ? Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.mixed")
                    : Component.empty());
        }
        for (int i = 0; i < this.sideRateFields.length; i++) {
            EditBox field = this.sideRateFields[i];
            if (!field.isFocused() && field != this.editingField) {
                field.setValue(formatUnitRate(this.menu.getSideRate(IO_SIDE_ORDER[i])));
            }
        }
        if (!this.pulseOnField.isFocused() && this.pulseOnField != this.editingField) {
            this.pulseOnField.setValue(Integer.toString(this.menu.getPulseOnTicks()));
        }
        if (!this.pulseOffField.isFocused() && this.pulseOffField != this.editingField) {
            this.pulseOffField.setValue(Integer.toString(this.menu.getPulseOffTicks()));
        }
        if (!this.batchSizeField.isFocused() && this.batchSizeField != this.editingField) {
            this.batchSizeField.setValue(Integer.toString(this.menu.getBatchSize()));
        }
    }

    private String rateLabelText() {
        return Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.label").getString()
                + this.rateUnit.suffix();
    }

    private String formatUnitRate(long perTick) {
        return Long.toString(this.rateUnit.fromPerTick(perTick));
    }

    private boolean isInsideRateLabel(int x, int y, boolean detailPage) {
        if (detailPage) {
            return isInside(x, y, DETAIL_RATE_LABEL_X, DETAIL_RATE_Y - 1,
                    this.font.width(rateLabelText()), 10);
        }
        return isInside(x, y, IO_RATE_LABEL_BOX_LEFT, IO_RATE_LABEL_Y - 1,
                IO_RATE_LABEL_BOX_RIGHT - IO_RATE_LABEL_BOX_LEFT, 10);
    }

    private boolean isHoveringRateLabel(int mouseX, int mouseY, boolean detailPage) {
        return isInsideRateLabel(mouseX - this.leftPos, mouseY - this.topPos, detailPage);
    }

    private void cycleRateUnit() {
        this.outputRateField.setFocus(false);
        for (EditBox field : this.sideRateFields) {
            field.setFocus(false);
        }
        this.editingField = null;
        this.rateUnit = this.rateUnit.next();
        refreshOutputRateFields();
    }

    private void setPage(GeneratorPage page, int faceDetail) {
        clearEditingField();
        this.page = page;
        this.faceDetail = page == GeneratorPage.FACE_DETAIL ? faceDetail : -1;
        this.menu.setPlayerSlotsVisible(page.showsInventory());
        this.menu.setGeneratorSlotVisible(page == GeneratorPage.MAIN);
        if (page == GeneratorPage.MAIN) {
            this.outputRateField.setFocus(false);
        }
        refreshOutputRateFields();
        updateFieldVisibility();
    }

    private boolean isIoPage() {
        return this.page.isIoPage();
    }

    private boolean isFaceDetailPage() {
        return this.page.isFaceDetail();
    }

    private void updateFieldVisibility() {
        boolean ioList = this.page == GeneratorPage.IO_LIST;
        boolean redstone = this.page.isRedstone();
        boolean batch = this.page.isBatch();
        this.outputRateField.setVisible(ioList);
        if (!ioList) {
            this.outputRateField.setFocus(false);
        }
        this.pulseOnField.setVisible(redstone);
        this.pulseOffField.setVisible(redstone);
        if (!redstone) {
            this.pulseOnField.setFocus(false);
            this.pulseOffField.setFocus(false);
        }
        this.batchSizeField.setVisible(batch);
        if (!batch) {
            this.batchSizeField.setFocus(false);
        }
        updateSideFieldVisibility();
    }

    private void updateSideFieldVisibility() {
        for (int i = 0; i < this.sideRateFields.length; i++) {
            EditBox field = this.sideRateFields[i];
            boolean show = isFaceDetailPage() && this.faceDetail == i;
            if (show) {
                field.setX(this.leftPos + DETAIL_RATE_X);
                field.setY(this.topPos + DETAIL_RATE_Y);
                field.setWidth(IO_RATE_FIELD_RIGHT - DETAIL_RATE_X);
            }
            field.setVisible(show);
            if (!show) {
                field.setFocus(false);
            }
        }
    }

    /** The visible input field under the given GUI-relative point, or null. */
    private EditBox inputFieldAt(int x, int y) {
        if (this.page == GeneratorPage.IO_LIST
                && isInside(x, y, IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                        IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            return this.outputRateField;
        }
        if (this.page.isFaceDetail()
                && isInside(x, y, DETAIL_RATE_X, DETAIL_RATE_Y,
                        IO_RATE_FIELD_RIGHT - DETAIL_RATE_X, DETAIL_RATE_HEIGHT)) {
            return this.sideRateFields[this.faceDetail];
        }
        if (this.page.isRedstone()) {
            if (isInside(x, y, RS_PULSE_ON_FIELD_X, RS_PULSE_ON_LABEL_Y,
                    RS_PULSE_ON_FIELD_RIGHT - RS_PULSE_ON_FIELD_X, RS_PULSE_FIELD_HEIGHT)) {
                return this.pulseOnField;
            }
            if (isInside(x, y, RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_LABEL_Y,
                    RS_PULSE_OFF_FIELD_RIGHT - RS_PULSE_OFF_FIELD_X, RS_PULSE_FIELD_HEIGHT)) {
                return this.pulseOffField;
            }
        }
        if (this.page.isBatch()
                && isInside(x, y, BATCH_FIELD_X, BATCH_LABEL_Y,
                        BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT)) {
            return this.batchSizeField;
        }
        return null;
    }

    private void clearEditingField() {
        if (this.editingField != null) {
            this.editingField.setFocus(false);
            this.editingField = null;
            this.refreshCooldown = 10;
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.page.isSecondary()) {
            ResourceLocation texture = isFaceDetailPage() ? FACE_TEXTURE
                    : this.page.isRedstone() ? REDSTONE_TEXTURE
                    : this.page.isBatch() ? BATCH_TEXTURE : IO_TEXTURE;
            RenderSystem.setShaderTexture(0, texture);
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
        if (this.page == GeneratorPage.MAIN) {
            GuiComponent.drawString(poseStack, this.font, this.title, this.titleLabelX, this.titleLabelY,
                    0xFFE9F1F4);
            GuiComponent.drawString(poseStack, this.font, this.playerInventoryTitle,
                    this.inventoryLabelX, this.inventoryLabelY, 0xFFE9F1F4);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (this.editingField != null && this.editingField.isVisible()
                && !this.editingField.isFocused()) {
            this.editingField.setFocus(true);
        }
        this.renderBackground(poseStack);
        // Keep JEI, FTB and other third-party controls in their original screen coordinates.
        super.render(poseStack, mouseX, mouseY, partialTick);
        if (this.page.isSecondary()) {
            if (this.refreshCooldown > 0) {
                this.refreshCooldown--;
            } else {
                refreshOutputRateFields();
            }
        }
        if (isIoPage()) {
            renderIoPage(poseStack, mouseX, mouseY);
            this.outputRateField.render(poseStack, mouseX, mouseY, partialTick);
            for (EditBox field : this.sideRateFields) {
                field.render(poseStack, mouseX, mouseY, partialTick);
            }
        } else if (this.page.isRedstone()) {
            renderRedstonePage(poseStack, mouseX, mouseY);
            this.pulseOnField.render(poseStack, mouseX, mouseY, partialTick);
            this.pulseOffField.render(poseStack, mouseX, mouseY, partialTick);
        } else if (this.page.isBatch()) {
            renderBatchPage(poseStack, mouseX, mouseY);
            this.batchSizeField.render(poseStack, mouseX, mouseY, partialTick);
        } else {
            renderStoredInfo(poseStack);
            renderMainButtons(poseStack, mouseX, mouseY);
        }
        renderTabs(poseStack, mouseX, mouseY);
        this.renderTooltip(poseStack, mouseX, mouseY);
        renderControlTooltip(poseStack, mouseX, mouseY);
    }

    private void renderTabs(PoseStack poseStack, int mouseX, int mouseY) {
        drawTab(poseStack, TAB_START_X, mainTabWidth(), mainTabLabel(),
                this.page == GeneratorPage.MAIN, mouseX, mouseY);
        drawTab(poseStack, ioTabX(), tabWidth(ioTabLabel()), ioTabLabel(),
                isIoPage(), mouseX, mouseY);
        drawTab(poseStack, redstoneTabX(), tabWidth(redstoneTabLabel()), redstoneTabLabel(),
                this.page.isRedstone(), mouseX, mouseY);
        drawTab(poseStack, batchTabX(), tabWidth(batchTabLabel()), batchTabLabel(),
                this.page.isBatch(), mouseX, mouseY);
    }

    private Component mainTabLabel() {
        return Component.translatable("gui." + InfinityWhat.MOD_ID + ".page.main");
    }

    private Component ioTabLabel() {
        return Component.translatable("gui." + InfinityWhat.MOD_ID + ".page.io");
    }

    private Component redstoneTabLabel() {
        return Component.translatable("gui." + InfinityWhat.MOD_ID + ".page.redstone");
    }

    private Component batchTabLabel() {
        return Component.translatable("gui." + InfinityWhat.MOD_ID + ".page.batch");
    }

    private int mainTabWidth() {
        return tabWidth(mainTabLabel());
    }

    private int ioTabX() {
        return TAB_START_X + mainTabWidth() + TAB_GAP;
    }

    private int redstoneTabX() {
        return ioTabX() + tabWidth(ioTabLabel()) + TAB_GAP;
    }

    private int batchTabX() {
        return redstoneTabX() + tabWidth(redstoneTabLabel()) + TAB_GAP;
    }

    private int tabWidth(Component label) {
        return this.font.width(label) + TAB_PADDING * 2;
    }

    private void drawTab(PoseStack poseStack, int x, int width, Component label,
                         boolean active, int mouseX, int mouseY) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + TAB_TOP_Y;
        boolean hovered = isHovering(x, TAB_TOP_Y, width, TAB_HEIGHT, mouseX, mouseY);
        GuiComponent.fill(poseStack, absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + TAB_HEIGHT, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (active ? BUTTON_ON : BUTTON_OFF);
        GuiComponent.fill(poseStack, absoluteX, absoluteY, absoluteX + width, absoluteY + TAB_HEIGHT, fill);
        GuiComponent.drawCenteredString(poseStack, this.font, label,
                absoluteX + width / 2, absoluteY + (TAB_HEIGHT - 8) / 2, BUTTON_TEXT);
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
        if (isFaceDetailPage()) {
            renderFaceDetail(poseStack, mouseX, mouseY);
        } else {
            renderIoList(poseStack, mouseX, mouseY);
        }
    }

    private void renderIoList(PoseStack poseStack, int mouseX, int mouseY) {
        String title = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.title_total",
                formatUnitRate(this.menu.getTotalEffectiveRate()) + this.rateUnit.suffix()).getString();
        GuiComponent.drawString(poseStack, this.font, truncate(title, IO_MODE_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);
        drawButton(poseStack, IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.mode."
                        + (this.advancedMode ? "simple" : "advanced")), false,
                isHovering(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT, mouseX, mouseY));
        drawButton(poseStack, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.literal("×"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(poseStack, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.output."
                        + (this.menu.isOutputEnabled() ? "on" : "off")),
                this.menu.isOutputEnabled(), isHovering(IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));
        String rateLabel = rateLabelText();
        int rateLabelX = IO_RATE_LABEL_BOX_LEFT + (IO_RATE_LABEL_BOX_RIGHT - IO_RATE_LABEL_BOX_LEFT
                - this.font.width(rateLabel)) / 2;
        GuiComponent.drawString(poseStack, this.font, rateLabel,
                this.leftPos + rateLabelX, this.topPos + IO_RATE_LABEL_Y,
                isHoveringRateLabel(mouseX, mouseY, false) ? 0xFFFFD27F : BUTTON_TEXT);
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
                boolean hovered = isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY);
                if (this.advancedMode) {
                    drawFaceButton(poseStack, x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                            direction, enabled, hovered);
                } else {
                    drawButton(poseStack, x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                            Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.side."
                                    + direction.getName() + (enabled ? ".on" : ".off")),
                            enabled, hovered);
                }
            }
        }
    }

    private void renderFaceDetail(PoseStack poseStack, int mouseX, int mouseY) {
        Direction direction = IO_SIDE_ORDER[this.faceDetail];
        String title = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.title",
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.side.short."
                        + direction.getName())).getString();
        GuiComponent.drawString(poseStack, this.font, truncate(title, DETAIL_BACK_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);
        drawButton(poseStack, DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.back"), false,
                isHovering(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                        mouseX, mouseY));
        drawButton(poseStack, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.literal("×"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(poseStack, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.output."
                        + (this.menu.isOutputSideEnabled(direction) ? "on" : "off")),
                this.menu.isOutputSideEnabled(direction), isHovering(IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));
        GuiComponent.drawString(poseStack, this.font, rateLabelText(),
                this.leftPos + DETAIL_RATE_LABEL_X, this.topPos + DETAIL_RATE_Y,
                isHoveringRateLabel(mouseX, mouseY, true) ? 0xFFFFD27F : BUTTON_TEXT);
        drawButton(poseStack, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                Component.literal("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        BlockPos pos = this.menu.getBlockEntity().getBlockPos().relative(direction);
        GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable(
                        "gui." + InfinityWhat.MOD_ID + ".io.detail.facing", getNeighborName(direction)).getString(),
                        GUI_WIDTH - 24), this.leftPos + 12, this.topPos + 56, BUTTON_TEXT);
        GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable(
                        "gui." + InfinityWhat.MOD_ID + ".io.detail.id", getNeighborId(direction)).getString(),
                        GUI_WIDTH - 24), this.leftPos + 12, this.topPos + 70, 0xFF9FB8C2);
        GuiComponent.drawString(poseStack, this.font, Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.pos",
                        pos.getX(), pos.getY(), pos.getZ()).getString(),
                this.leftPos + 12, this.topPos + 84, 0xFF9FB8C2);
        GuiComponent.drawString(poseStack, this.font,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.hint"),
                this.leftPos + 12, this.topPos + 137, 0xFF9FB8C2);
    }

    private void renderRedstonePage(PoseStack poseStack, int mouseX, int mouseY) {
        GuiComponent.drawString(poseStack, this.font,
                truncate(Component.translatable("gui." + InfinityWhat.MOD_ID + ".redstone.title").getString(),
                        IO_MODE_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);
        RedstoneMode mode = this.menu.getRedstoneMode();
        RedstoneMode[] modes = RedstoneMode.values();
        for (int i = 0; i < modes.length && i < RS_MODE_X.length; i++) {
            drawModeButton(poseStack, i, Component.translatable("gui." + InfinityWhat.MOD_ID
                    + ".redstone.mode." + modes[i].key()), mode == modes[i], mouseX, mouseY);
        }
        drawCenteredInBox(poseStack,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.on"),
                12, 40, RS_PULSE_ON_LABEL_Y);
        drawCenteredInBox(poseStack,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.off"),
                10, 80, RS_PULSE_OFF_LABEL_Y);
        drawButton(poseStack, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                Component.literal("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        GuiComponent.drawString(poseStack, this.font, truncate(redstoneStatus().getString(), 74),
                this.leftPos + 14, this.topPos + RS_STATUS_Y, redstoneStatusColor());
        GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".redstone.comparator",
                        this.menu.getBlockEntity().getComparatorOutput()).getString(), 74),
                this.leftPos + 98, this.topPos + RS_STATUS_Y, 0xFF9FB8C2);
        GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".redstone.pulse.note").getString(), 74),
                this.leftPos + 14, this.topPos + RS_NOTE_Y, 0xFF9FB8C2);
        GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".redstone.pulse.range").getString(), 74),
                this.leftPos + 98, this.topPos + RS_NOTE_Y, 0xFF9FB8C2);
        drawButton(poseStack, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.literal("×"), false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                            + ".redstone.help." + (i + 1)).getString(), 158),
                    this.leftPos + 14, this.topPos + RS_HELP_Y + i * RS_HELP_LINE,
                    0xFF9FB8C2);
        }
    }

    private void renderBatchPage(PoseStack poseStack, int mouseX, int mouseY) {
        GuiComponent.drawString(poseStack, this.font,
                truncate(Component.translatable("gui." + InfinityWhat.MOD_ID + ".batch.title").getString(),
                        IO_MODE_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);
        drawCenteredInBox(poseStack,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".batch.label"),
                BATCH_LABEL_BOX_LEFT, BATCH_LABEL_BOX_WIDTH, BATCH_LABEL_Y);
        drawButton(poseStack, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                Component.literal("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".batch.info.period", this.menu.getBatchSize()).getString(), 77),
                this.leftPos + 12, this.topPos + BATCH_INFO_Y, BUTTON_TEXT);
        GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".batch.info.burst", Long.toString(
                                (long) this.menu.getBatchSize() * this.menu.getTotalEffectiveRate())).getString(), 77),
                this.leftPos + 96, this.topPos + BATCH_INFO_Y, BUTTON_TEXT);
        GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".batch.note.once").getString(), 77),
                this.leftPos + 12, this.topPos + BATCH_NOTE_Y, 0xFF9FB8C2);
        GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".batch.note.gated").getString(), 77),
                this.leftPos + 96, this.topPos + BATCH_NOTE_Y, 0xFF9FB8C2);
        drawButton(poseStack, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.literal("×"), false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            GuiComponent.drawString(poseStack, this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                            + ".batch.help." + (i + 1)).getString(), 158),
                    this.leftPos + 14, this.topPos + BATCH_HELP_Y + i * BATCH_HELP_LINE,
                    0xFF9FB8C2);
        }
    }

    private void drawModeButton(PoseStack poseStack, int index, Component label,
                                boolean active, int mouseX, int mouseY) {
        int x = RS_MODE_X[index];
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + RS_MODE_Y;
        boolean hovered = isHovering(x, RS_MODE_Y, RS_MODE_WIDTH, RS_MODE_HEIGHT, mouseX, mouseY);
        if (active || hovered) {
            GuiComponent.fill(poseStack, absoluteX + 2, absoluteY + 2,
                    absoluteX + RS_MODE_WIDTH - 1, absoluteY + RS_MODE_HEIGHT - 1,
                    hovered ? BUTTON_HOVER : BUTTON_ON);
        }
        if (index > 0) {
            GuiComponent.fill(poseStack, absoluteX, absoluteY, absoluteX + 1,
                    absoluteY + RS_MODE_HEIGHT, BUTTON_BORDER);
        }
        GuiComponent.drawCenteredString(poseStack, this.font, label,
                absoluteX + RS_MODE_WIDTH / 2, absoluteY + (RS_MODE_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void drawCenteredInBox(PoseStack poseStack, Component label,
                                   int boxX, int boxWidth, int y) {
        int x = boxX + (boxWidth - this.font.width(label)) / 2;
        GuiComponent.drawString(poseStack, this.font, label.getString(), this.leftPos + x, this.topPos + y,
                BUTTON_TEXT);
    }

    private int redstoneStatusColor() {
        if (!this.menu.isOutputEnabled() || this.menu.getTotalEffectiveRate() <= 0L) {
            return 0xFF9FB8C2;
        }
        return this.menu.getBlockEntity().isRedstoneAllowed() ? 0xFF7CE38B : 0xFFFFC66D;
    }

    private Component redstoneStatus() {
        if (!this.menu.isOutputEnabled()) {
            return Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".redstone.status.output_off");
        }
        if (this.menu.getTotalEffectiveRate() <= 0L) {
            return Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".redstone.status.no_sides");
        }
        if (!this.menu.getBlockEntity().isRedstoneAllowed()) {
            return switch (this.menu.getRedstoneMode()) {
                case HIGH -> Component.translatable(
                        "gui." + InfinityWhat.MOD_ID + ".redstone.status.waiting_signal");
                case LOW -> Component.translatable(
                        "gui." + InfinityWhat.MOD_ID + ".redstone.status.waiting_low");
                case PULSE -> Component.translatable(
                        "gui." + InfinityWhat.MOD_ID + ".redstone.status.pulse_off");
                default -> Component.translatable(
                        "gui." + InfinityWhat.MOD_ID + ".redstone.status.running");
            };
        }
        return Component.translatable(
                "gui." + InfinityWhat.MOD_ID + ".redstone.status.running");
    }

    private void submitPulse() {
        try {
            int onTicks = Integer.parseInt(this.pulseOnField.getValue().trim());
            int offTicks = Integer.parseInt(this.pulseOffField.getValue().trim());
            if (onTicks < InfItemGeneratorBlockEntity.MIN_PULSE_TICKS
                    || onTicks > InfItemGeneratorBlockEntity.MAX_PULSE_TICKS
                    || offTicks < InfItemGeneratorBlockEntity.MIN_PULSE_TICKS
                    || offTicks > InfItemGeneratorBlockEntity.MAX_PULSE_TICKS) {
                throw new NumberFormatException();
            }
            ModNetworking.sendRedstone(this.menu.containerId, false,
                    this.menu.getRedstoneMode().ordinal(), onTicks, offTicks);
            this.pulseOnField.setValue(Integer.toString(onTicks));
            this.pulseOffField.setValue(Integer.toString(offTicks));
        } catch (NumberFormatException exception) {
            showInvalidPulse();
        }
    }

    private void showInvalidPulse() {
        this.minecraft.player.displayClientMessage(Component.translatable(
                "gui." + InfinityWhat.MOD_ID + ".redstone.pulse.invalid")
                .withStyle(ChatFormatting.RED), true);
    }

    private void submitBatch() {
        try {
            int size = Integer.parseInt(this.batchSizeField.getValue().trim());
            if (size < InfItemGeneratorBlockEntity.MIN_BATCH_SIZE
                    || size > InfItemGeneratorBlockEntity.MAX_BATCH_SIZE) {
                throw new NumberFormatException();
            }
            ModNetworking.sendBatch(this.menu.containerId, false, size);
            this.batchSizeField.setValue(Integer.toString(size));
        } catch (NumberFormatException exception) {
            showInvalidBatch();
        }
    }

    private void showInvalidBatch() {
        this.minecraft.player.displayClientMessage(Component.translatable(
                "gui." + InfinityWhat.MOD_ID + ".batch.invalid")
                .withStyle(ChatFormatting.RED), true);
    }

    private String getNeighborName(Direction direction) {
        if (this.minecraft.level == null) {
            return "-";
        }
        BlockPos pos = this.menu.getBlockEntity().getBlockPos().relative(direction);
        if (!this.minecraft.level.isLoaded(pos)) {
            return "-";
        }
        BlockState state = this.minecraft.level.getBlockState(pos);
        return state.isAir() ? "-" : state.getBlock().getName().getString();
    }

    private String getNeighborId(Direction direction) {
        if (this.minecraft.level == null) {
            return "-";
        }
        BlockPos pos = this.menu.getBlockEntity().getBlockPos().relative(direction);
        if (!this.minecraft.level.isLoaded(pos)) {
            return "-";
        }
        BlockState state = this.minecraft.level.getBlockState(pos);
        return state.isAir() ? "-" : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
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

    /** Advanced-mode face button: color carries the on/off state, so no explicit switch text. */
    private void drawFaceButton(PoseStack poseStack, int x, int y, int width, int height,
                                Direction direction, boolean enabled, boolean hovered) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        GuiComponent.fill(poseStack, absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = enabled ? (hovered ? FACE_ON_HOVER : BUTTON_ON)
                : (hovered ? FACE_OFF_HOVER : BUTTON_OFF);
        GuiComponent.fill(poseStack, absoluteX, absoluteY, absoluteX + width, absoluteY + height, fill);
        Component label = Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.side."
                + direction.getName());
        GuiComponent.drawCenteredString(poseStack, this.font, label, absoluteX + width / 2,
                absoluteY + (height - 8) / 2, BUTTON_TEXT);
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
            EditBox field = inputFieldAt(x, y);
            if (field != null) {
                clearEditingField();
                this.editingField = field;
                field.setFocus(true);
                // Still dispatch through the vanilla screen so its focused listener is updated;
                // character input is routed through Screen.getFocused(), not the widget flag.
                return super.mouseClicked(mouseX, mouseY, button);
            }
            clearEditingField();
            if (isInside(x, y, TAB_START_X, TAB_TOP_Y, mainTabWidth(), TAB_HEIGHT)) {
                if (this.page.isSecondary()) {
                    playButtonSound();
                    returnToMainPage();
                }
                return true;
            }
            if (isInside(x, y, ioTabX(), TAB_TOP_Y, tabWidth(ioTabLabel()), TAB_HEIGHT)) {
                if (this.page != GeneratorPage.IO_LIST) {
                    playButtonSound();
                    setPage(GeneratorPage.IO_LIST, -1);
                }
                return true;
            }
            if (isInside(x, y, redstoneTabX(), TAB_TOP_Y, tabWidth(redstoneTabLabel()), TAB_HEIGHT)) {
                if (!this.page.isRedstone()) {
                    playButtonSound();
                    setPage(GeneratorPage.REDSTONE, -1);
                }
                return true;
            }
            if (isInside(x, y, batchTabX(), TAB_TOP_Y, tabWidth(batchTabLabel()), TAB_HEIGHT)) {
                if (!this.page.isBatch()) {
                    playButtonSound();
                    setPage(GeneratorPage.BATCH, -1);
                }
                return true;
            }
            if (this.page == GeneratorPage.MAIN) {
                if (isInside(x, y, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                        MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT)) {
                    playButtonSound();
                    this.minecraft.gameMode.handleInventoryButtonClick(
                            this.menu.containerId, InfItemGeneratorMenu.BUTTON_TOGGLE_INFINITE);
                    return true;
                }
                if (isInside(x, y, MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT)) {
                    playButtonSound();
                    setPage(GeneratorPage.IO_LIST, -1);
                    return true;
                }
            } else if (this.page.isRedstone()) {
                RedstoneMode[] modes = RedstoneMode.values();
                for (int i = 0; i < modes.length && i < RS_MODE_X.length; i++) {
                    if (isInside(x, y, RS_MODE_X[i], RS_MODE_Y, RS_MODE_WIDTH, RS_MODE_HEIGHT)) {
                        playButtonSound();
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                                InfItemGeneratorMenu.BUTTON_REDSTONE_MODE_BASE + i);
                        return true;
                    }
                }
                if (isInside(x, y, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitPulse();
                    return true;
                }
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    returnToMainPage();
                    return true;
                }
            } else if (this.page.isBatch()) {
                if (isInside(x, y, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitBatch();
                    return true;
                }
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    returnToMainPage();
                    return true;
                }
            } else if (isFaceDetailPage()) {
                Direction direction = IO_SIDE_ORDER[this.faceDetail];
                if (isInsideRateLabel(x, y, true)) {
                    playButtonSound();
                    cycleRateUnit();
                    return true;
                }
                if (isInside(x, y, DETAIL_BACK_X, DETAIL_BACK_Y,
                        DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT)) {
                    playButtonSound();
                    setPage(GeneratorPage.IO_LIST, -1);
                    return true;
                }
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    returnToMainPage();
                    return true;
                }
                if (isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
                    playButtonSound();
                    int buttonId = InfItemGeneratorMenu.BUTTON_TOGGLE_SIDE_BASE
                            + direction.get3DDataValue();
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
                    return true;
                }
                if (isInside(x, y, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitSideRate(this.faceDetail);
                    return true;
                }
            } else {
                if (isInsideRateLabel(x, y, false)) {
                    playButtonSound();
                    cycleRateUnit();
                    return true;
                }
                if (isInside(x, y, IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT)) {
                    playButtonSound();
                    this.advancedMode = !this.advancedMode;
                    setPage(GeneratorPage.IO_LIST, -1);
                    return true;
                }
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
                            Direction direction = IO_SIDE_LAYOUT[row][column];
                            if (this.advancedMode) {
                                setPage(GeneratorPage.FACE_DETAIL, direction.get3DDataValue());
                            } else {
                                int buttonId = InfItemGeneratorMenu.BUTTON_TOGGLE_SIDE_BASE
                                        + direction.get3DDataValue();
                                this.minecraft.gameMode.handleInventoryButtonClick(
                                        this.menu.containerId, buttonId);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void submitOutputRate() {
        submitRate(this.outputRateField, -1);
    }

    private void submitSideRate(int side) {
        submitRate(this.sideRateFields[side], side);
    }

    private void submitRate(EditBox field, int side) {
        try {
            long value = Long.parseLong(field.getValue().trim());
            if (value < 0) throw new NumberFormatException();
            if (!this.rateUnit.isExact(value)) {
                field.setValue("");
                this.minecraft.player.displayClientMessage(Component.translatable(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.inexact",
                        Long.toString(this.rateUnit.ticksPerUnit()))
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            long perTick = this.rateUnit.toPerTick(value);
            if (perTick > Integer.MAX_VALUE) throw new NumberFormatException();
            ModNetworking.sendOutputRate(this.menu.containerId, false, side, perTick);
        } catch (NumberFormatException exception) {
            field.setValue("");
            showInvalidItemRate();
        }
    }

    private void showInvalidItemRate() {
        this.minecraft.player.displayClientMessage(Component.translatable(
                "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_item")
                .withStyle(ChatFormatting.RED), true);
    }

    private void playButtonSound() {
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public int getContainerId() {
        return this.menu.containerId;
    }

    public void onOutputRateResult(int side, boolean success, long amount, RateRejectReason reason) {
        this.refreshCooldown = 10;
        if (side >= 0 && side < this.sideRateFields.length) {
            EditBox field = this.sideRateFields[side];
            if (field != this.editingField) {
                field.setValue(success ? formatUnitRate(amount) : "");
            }
        } else {
            if (this.outputRateField != this.editingField) {
                this.outputRateField.setValue(success ? formatUnitRate(amount) : "");
            }
            if (success) {
                for (EditBox field : this.sideRateFields) {
                    if (field != this.editingField) {
                        field.setValue(formatUnitRate(amount));
                    }
                }
            }
        }
        if (success) {
            this.minecraft.player.displayClientMessage(Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.saved")
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            this.minecraft.player.displayClientMessage(itemRateRejectMessage(reason)
                    .copy().withStyle(ChatFormatting.RED), true);
        }
    }

    private Component itemRateRejectMessage(RateRejectReason reason) {
        return switch (reason) {
            case LIMIT_SIDE -> Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.limit_side",
                    formatUnitRate(InfinityWhatConfig.maxItemRatePerSide()) + this.rateUnit.suffix());
            case LIMIT_TOTAL -> Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.limit_total",
                    formatUnitRate(InfinityWhatConfig.maxItemTotalRate()) + this.rateUnit.suffix());
            case INVALID -> Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_item");
            default -> Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.rejected");
        };
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.page.isSecondary() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.page.isFaceDetail()) {
                setPage(GeneratorPage.IO_LIST, -1);
            } else {
                returnToMainPage();
            }
            return true;
        }
        if (this.page == GeneratorPage.IO_LIST
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            playButtonSound();
            submitOutputRate();
            return true;
        }
        if (this.page.isFaceDetail()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            playButtonSound();
            submitSideRate(this.faceDetail);
            return true;
        }
        if (this.page.isRedstone()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            playButtonSound();
            submitPulse();
            return true;
        }
        if (this.page.isBatch()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            playButtonSound();
            submitBatch();
            return true;
        }
        EditBox focusedField = focusedInputField();
        if (focusedField != null) {
            // While an input field is focused, swallow every other key so the inventory
            // key (E) and hotbar keys never close the GUI or swap items mid-typing.
            focusedField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private EditBox focusedInputField() {
        if (this.editingField != null && this.editingField.isVisible()) {
            this.editingField.setFocus(true);
            return this.editingField;
        }
        if (this.outputRateField != null && this.outputRateField.isVisible()
                && this.outputRateField.isFocused()) {
            return this.outputRateField;
        }
        for (EditBox field : this.sideRateFields) {
            if (field.isVisible() && field.isFocused()) {
                return field;
            }
        }
        if (this.pulseOnField != null && this.pulseOnField.isVisible()
                && this.pulseOnField.isFocused()) {
            return this.pulseOnField;
        }
        if (this.pulseOffField != null && this.pulseOffField.isVisible()
                && this.pulseOffField.isFocused()) {
            return this.pulseOffField;
        }
        if (this.batchSizeField != null && this.batchSizeField.isVisible()
                && this.batchSizeField.isFocused()) {
            return this.batchSizeField;
        }
        return null;
    }

    private void returnToMainPage() {
        setPage(GeneratorPage.MAIN, -1);
    }

    private void renderControlTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        Component tooltip = null;
        if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.infinite_mode.tooltip");
        } else if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_IO_X, MAIN_IO_Y,
                MAIN_IO_WIDTH, MAIN_IO_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.tooltip");
        } else if (this.page.isRedstone() && isInside(x, y, RS_MODE_X[0], RS_MODE_Y,
                RS_MODE_X[RS_MODE_X.length - 1] + RS_MODE_WIDTH - RS_MODE_X[0], RS_MODE_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".redstone.mode.tooltip");
        } else if (this.page.isRedstone()
                && (isInside(x, y, RS_PULSE_ON_FIELD_X, RS_PULSE_ON_LABEL_Y,
                        RS_PULSE_ON_FIELD_RIGHT - RS_PULSE_ON_FIELD_X, RS_PULSE_FIELD_HEIGHT)
                    || isInside(x, y, RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_LABEL_Y,
                        RS_PULSE_OFF_FIELD_RIGHT - RS_PULSE_OFF_FIELD_X, RS_PULSE_FIELD_HEIGHT))) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.tooltip");
        } else if (this.page.isBatch() && isInside(x, y, BATCH_FIELD_X, BATCH_LABEL_Y,
                BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".batch.tooltip");
        } else if (isIoPage() && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".items.io.output.tooltip");
        } else if (isIoPage() && isFaceDetailPage() && isInsideRateLabel(x, y, true)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.unit.tooltip",
                    this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInsideRateLabel(x, y, false)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.unit.tooltip",
                    this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInside(x, y, IO_RATE_FIELD_X,
                IO_RATE_FIELD_Y, IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.item.tooltip");
        } else if (isIoPage() && !isFaceDetailPage()
                && isInside(x, y, IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.mode.tooltip");
        } else if (isIoPage() && !isFaceDetailPage() && this.advancedMode) {
            Direction direction = getHoveredFace(x, y);
            if (direction != null) {
                tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.neighbor.tooltip",
                        getNeighborName(direction), getNeighborId(direction));
            }
        }
        if (tooltip != null) this.renderTooltip(poseStack, tooltip, mouseX, mouseY);
    }

    private Direction getHoveredFace(int x, int y) {
        for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
            for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                int sideX = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                if (isInside(x, y, sideX, IO_SIDE_ROW_YS[row], IO_SIDE_WIDTH, IO_SIDE_HEIGHT)) {
                    return IO_SIDE_LAYOUT[row][column];
                }
            }
        }
        return null;
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return isInside(mouseX - this.leftPos, mouseY - this.topPos, x, y, width, height);
    }

    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
