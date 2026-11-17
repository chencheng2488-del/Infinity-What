package com.bandybus.infwhat.client.gui;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.InfItemGeneratorContainer;
import com.bandybus.infwhat.network.ModNetworking;
import com.bandybus.infwhat.network.RateRejectReason;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.input.Keyboard;

import java.util.Collections;

/** Item generator main page and the shared active-output pages. */
public class GuiInfItemGenerator extends GuiContainer {

    public static final ResourceLocation TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_item_generator.png");
    public static final ResourceLocation IO_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_io.png");
    public static final ResourceLocation FACE_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_face.png");
    public static final ResourceLocation REDSTONE_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_redstone.png");
    public static final ResourceLocation BATCH_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_batch.png");

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

    private static final int IO_SIDE_LEFT_X = 10;
    private static final int IO_SIDE_RIGHT_X = 94;
    private static final int IO_SIDE_WIDTH = 80;
    private static final int IO_SIDE_HEIGHT = 9;
    private static final int[] IO_SIDE_ROW_YS = {52, 63, 74};
    private static final EnumFacing[][] IO_SIDE_LAYOUT = {
            {EnumFacing.DOWN, EnumFacing.UP},
            {EnumFacing.NORTH, EnumFacing.SOUTH},
            {EnumFacing.WEST, EnumFacing.EAST}
    };
    private static final EnumFacing[] IO_SIDE_ORDER = EnumFacing.VALUES;

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
    private static final int COLOR_TITLE = 0xFFE9F1F4;
    private static final int COLOR_TEXT = 0xFF9FB8C2;

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

    private final InfItemGeneratorContainer container;
    private GeneratorPage page = GeneratorPage.MAIN;
    private boolean advancedMode;
    private int faceDetail = -1;
    private GuiTextField outputRateField;
    private final GuiTextField[] sideRateFields = new GuiTextField[IO_SIDE_ORDER.length];
    private GuiTextField pulseOnField;
    private GuiTextField pulseOffField;
    private GuiTextField batchSizeField;
    /** Field the player is currently typing in; protected from any server-driven refresh. */
    private GuiTextField editingField;
    private int refreshCooldown;
    private RateUnit rateUnit = RateUnit.PER_TICK;

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
        this.outputRateField = createField(IO_RATE_FIELD_X, IO_RATE_FIELD_Y,
                IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT, 16);
        for (int i = 0; i < this.sideRateFields.length; i++) {
            this.sideRateFields[i] = createField(DETAIL_RATE_X, DETAIL_RATE_Y,
                    IO_RATE_FIELD_RIGHT - DETAIL_RATE_X, DETAIL_RATE_HEIGHT, 16);
        }
        this.pulseOnField = createField(RS_PULSE_ON_FIELD_X, RS_PULSE_ON_LABEL_Y,
                RS_PULSE_ON_FIELD_RIGHT - RS_PULSE_ON_FIELD_X, RS_PULSE_FIELD_HEIGHT, 5);
        this.pulseOffField = createField(RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_LABEL_Y,
                RS_PULSE_OFF_FIELD_RIGHT - RS_PULSE_OFF_FIELD_X, RS_PULSE_FIELD_HEIGHT, 5);
        this.batchSizeField = createField(BATCH_FIELD_X, BATCH_LABEL_Y,
                BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT, 2);
        setPage(GeneratorPage.MAIN, -1);
    }

    private GuiTextField createField(int x, int y, int width, int height, int maxLength) {
        GuiTextField field = new GuiTextField(0, this.fontRenderer, this.guiLeft + x,
                this.guiTop + y, width, height);
        field.setMaxStringLength(maxLength);
        field.setTextColor(BUTTON_TEXT);
        field.setEnableBackgroundDrawing(false);
        field.setVisible(false);
        return field;
    }

    private void refreshOutputRateFields() {
        boolean mixed = this.container.hasMixedSideRates();
        if (!this.outputRateField.isFocused() && this.outputRateField != this.editingField) {
            this.outputRateField.setText(mixed ? ""
                    : formatUnitRate(this.container.getUniformSideRate()));
        }
        for (int i = 0; i < this.sideRateFields.length; i++) {
            GuiTextField field = this.sideRateFields[i];
            if (!field.isFocused() && field != this.editingField) {
                field.setText(formatUnitRate(this.container.getSideRate(IO_SIDE_ORDER[i])));
            }
        }
        if (!this.pulseOnField.isFocused() && this.pulseOnField != this.editingField) {
            this.pulseOnField.setText(Integer.toString(this.container.getPulseOnTicks()));
        }
        if (!this.pulseOffField.isFocused() && this.pulseOffField != this.editingField) {
            this.pulseOffField.setText(Integer.toString(this.container.getPulseOffTicks()));
        }
        if (!this.batchSizeField.isFocused() && this.batchSizeField != this.editingField) {
            this.batchSizeField.setText(Integer.toString(this.container.getBatchSize()));
        }
    }

    private String rateLabelText() {
        return I18n.format("gui.infwhat.io.rate.label") + this.rateUnit.suffix();
    }

    private String formatUnitRate(long perTick) {
        return Long.toString(this.rateUnit.fromPerTick(perTick));
    }

    private boolean isInsideRateLabel(int x, int y, boolean detailPage) {
        if (detailPage) {
            return isInside(x, y, DETAIL_RATE_LABEL_X, DETAIL_RATE_Y - 1,
                    this.fontRenderer.getStringWidth(rateLabelText()), 10);
        }
        return isInside(x, y, IO_RATE_LABEL_BOX_LEFT, IO_RATE_LABEL_Y - 1,
                IO_RATE_LABEL_BOX_RIGHT - IO_RATE_LABEL_BOX_LEFT, 10);
    }

    private boolean isHoveringRateLabel(int mouseX, int mouseY, boolean detailPage) {
        return isInsideRateLabel(mouseX - this.guiLeft, mouseY - this.guiTop, detailPage);
    }

    private void cycleRateUnit() {
        this.outputRateField.setFocused(false);
        for (GuiTextField field : this.sideRateFields) {
            field.setFocused(false);
        }
        this.editingField = null;
        this.rateUnit = this.rateUnit.next();
        refreshOutputRateFields();
    }

    private void setPage(GeneratorPage page, int faceDetail) {
        clearEditingField();
        this.page = page;
        this.faceDetail = page == GeneratorPage.FACE_DETAIL ? faceDetail : -1;
        this.container.setPlayerSlotsVisible(page.showsInventory());
        this.container.setGeneratorSlotVisible(page == GeneratorPage.MAIN);
        if (page == GeneratorPage.MAIN) {
            this.outputRateField.setFocused(false);
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
        showField(this.outputRateField, ioList);
        showField(this.pulseOnField, redstone);
        showField(this.pulseOffField, redstone);
        showField(this.batchSizeField, batch);
        updateSideFieldVisibility();
    }

    private void updateSideFieldVisibility() {
        for (int i = 0; i < this.sideRateFields.length; i++) {
            GuiTextField field = this.sideRateFields[i];
            boolean show = isFaceDetailPage() && this.faceDetail == i;
            if (show) {
                field.x = this.guiLeft + DETAIL_RATE_X;
                field.y = this.guiTop + DETAIL_RATE_Y;
                field.width = IO_RATE_FIELD_RIGHT - DETAIL_RATE_X;
            }
            showField(field, show);
        }
    }

    private static void showField(GuiTextField field, boolean show) {
        field.setVisible(show);
        if (!show) {
            field.setFocused(false);
        }
    }

    /** The visible input field under the given GUI-relative point, or null. */
    private GuiTextField inputFieldAt(int x, int y) {
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
            this.editingField.setFocused(false);
            this.editingField = null;
            this.refreshCooldown = 10;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.page.isSecondary()) {
            ResourceLocation texture = isFaceDetailPage() ? FACE_TEXTURE
                    : this.page.isRedstone() ? REDSTONE_TEXTURE
                    : this.page.isBatch() ? BATCH_TEXTURE : IO_TEXTURE;
            this.mc.getTextureManager().bindTexture(texture);
            drawModalRectWithCustomSizedTexture(this.guiLeft, this.guiTop, 0, 0,
                    GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            this.mc.getTextureManager().bindTexture(TEXTURE);
            drawModalRectWithCustomSizedTexture(this.guiLeft, this.guiTop, 0, 0,
                    GUI_WIDTH, GUI_HEIGHT, MAIN_TEXTURE_WIDTH, GUI_HEIGHT);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (this.page == GeneratorPage.MAIN) {
            this.fontRenderer.drawString(I18n.format(
                    "container.infwhat.inf_item_generator"), 12, 8, COLOR_TITLE);
            this.fontRenderer.drawString(I18n.format("container.inventory"), 12, 86, COLOR_TITLE);
            renderStoredInfo();
            renderMainButtons(mouseX, mouseY);
        } else if (this.page.isFaceDetail()) {
            renderFaceDetail(mouseX, mouseY);
        } else if (this.page.isRedstone()) {
            renderRedstonePage(mouseX, mouseY);
        } else if (this.page.isBatch()) {
            renderBatchPage(mouseX, mouseY);
        } else {
            renderIoList(mouseX, mouseY);
        }
        renderTabs(mouseX, mouseY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.editingField != null && this.editingField.getVisible()
                && !this.editingField.isFocused()) {
            this.editingField.setFocused(true);
        }
        if (this.page.isSecondary()) {
            if (this.refreshCooldown > 0) {
                this.refreshCooldown--;
            } else {
                refreshOutputRateFields();
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawVisibleFields();
        renderControlTooltip(mouseX, mouseY);
    }

    private void drawVisibleFields() {
        if (this.outputRateField.getVisible()) {
            if (!this.outputRateField.isFocused() && this.outputRateField.getText().isEmpty()) {
                this.fontRenderer.drawString(I18n.format("gui.infwhat.io.rate.mixed"),
                        this.outputRateField.x, this.outputRateField.y, COLOR_TEXT);
            }
            this.outputRateField.drawTextBox();
        }
        for (GuiTextField field : this.sideRateFields) {
            if (field.getVisible()) {
                field.drawTextBox();
            }
        }
        if (this.pulseOnField.getVisible()) {
            this.pulseOnField.drawTextBox();
        }
        if (this.pulseOffField.getVisible()) {
            this.pulseOffField.drawTextBox();
        }
        if (this.batchSizeField.getVisible()) {
            this.batchSizeField.drawTextBox();
        }
    }

    private void renderTabs(int mouseX, int mouseY) {
        drawTab(TAB_START_X, mainTabWidth(), mainTabLabel(),
                this.page == GeneratorPage.MAIN, mouseX, mouseY);
        drawTab(ioTabX(), tabWidth(ioTabLabel()), ioTabLabel(),
                isIoPage(), mouseX, mouseY);
        drawTab(redstoneTabX(), tabWidth(redstoneTabLabel()), redstoneTabLabel(),
                this.page.isRedstone(), mouseX, mouseY);
        drawTab(batchTabX(), tabWidth(batchTabLabel()), batchTabLabel(),
                this.page.isBatch(), mouseX, mouseY);
    }

    private String mainTabLabel() {
        return I18n.format("gui.infwhat.page.main");
    }

    private String ioTabLabel() {
        return I18n.format("gui.infwhat.page.io");
    }

    private String redstoneTabLabel() {
        return I18n.format("gui.infwhat.page.redstone");
    }

    private String batchTabLabel() {
        return I18n.format("gui.infwhat.page.batch");
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

    private int tabWidth(String label) {
        return this.fontRenderer.getStringWidth(label) + TAB_PADDING * 2;
    }

    private void drawTab(int x, int width, String label, boolean active, int mouseX, int mouseY) {
        boolean hovered = isHovering(x, TAB_TOP_Y, width, TAB_HEIGHT, mouseX, mouseY);
        drawRect(x - 1, TAB_TOP_Y - 1, x + width + 1, TAB_TOP_Y + TAB_HEIGHT, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (active ? BUTTON_ON : BUTTON_OFF);
        drawRect(x, TAB_TOP_Y, x + width, TAB_TOP_Y + TAB_HEIGHT, fill);
        this.drawCenteredString(this.fontRenderer, label, x + width / 2,
                TAB_TOP_Y + (TAB_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void renderMainButtons(int mouseX, int mouseY) {
        drawButton(MAIN_INFINITE_X, MAIN_INFINITE_Y, MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT,
                I18n.format("gui.infwhat.items.infinite_mode."
                        + (this.container.isInfiniteModeEnabled() ? "on" : "off")),
                this.container.isInfiniteModeEnabled(),
                isHovering(MAIN_INFINITE_X, MAIN_INFINITE_Y, MAIN_INFINITE_WIDTH,
                        MAIN_INFINITE_HEIGHT, mouseX, mouseY));
        drawButton(MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT,
                I18n.format("gui.infwhat.items.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT,
                        mouseX, mouseY));
    }

    private void renderIoList(int mouseX, int mouseY) {
        String title = I18n.format("gui.infwhat.io.title_total",
                formatUnitRate(this.container.getTotalEffectiveRate()) + this.rateUnit.suffix());
        this.fontRenderer.drawString(truncate(title, IO_MODE_X - 16),
                12, 8, BUTTON_TEXT);
        drawButton(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT,
                I18n.format("gui.infwhat.io.mode."
                        + (this.advancedMode ? "simple" : "advanced")), false,
                isHovering(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT, mouseX, mouseY));
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                "\u00d7", false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                I18n.format("gui.infwhat.items.io.output."
                        + (this.container.isOutputEnabled() ? "on" : "off")),
                this.container.isOutputEnabled(), isHovering(IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));
        String rateLabel = rateLabelText();
        int rateLabelX = IO_RATE_LABEL_BOX_LEFT + (IO_RATE_LABEL_BOX_RIGHT - IO_RATE_LABEL_BOX_LEFT
                - this.fontRenderer.getStringWidth(rateLabel)) / 2;
        this.fontRenderer.drawString(rateLabel, rateLabelX, IO_RATE_LABEL_Y,
                isHoveringRateLabel(mouseX, mouseY, false) ? 0xFFFFD27F : BUTTON_TEXT);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                "\u2713", false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
            for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                EnumFacing direction = IO_SIDE_LAYOUT[row][column];
                int x = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                int y = IO_SIDE_ROW_YS[row];
                boolean enabled = this.container.isOutputSideEnabled(direction);
                boolean hovered = isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY);
                if (this.advancedMode) {
                    drawFaceButton(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, direction, enabled, hovered);
                } else {
                    drawButton(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                            I18n.format("gui.infwhat.items.io.side." + direction.getName()
                                    + (enabled ? ".on" : ".off")), enabled, hovered);
                }
            }
        }
    }

    private void renderFaceDetail(int mouseX, int mouseY) {
        EnumFacing direction = IO_SIDE_ORDER[this.faceDetail];
        String title = I18n.format("gui.infwhat.io.detail.title",
                I18n.format("gui.infwhat.io.side.short." + direction.getName()));
        this.fontRenderer.drawString(truncate(title, DETAIL_BACK_X - 16),
                12, 8, BUTTON_TEXT);
        drawButton(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                I18n.format("gui.infwhat.io.detail.back"), false,
                isHovering(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                        mouseX, mouseY));
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                "\u00d7", false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                I18n.format("gui.infwhat.io.detail.output."
                        + (this.container.isOutputSideEnabled(direction) ? "on" : "off")),
                this.container.isOutputSideEnabled(direction), isHovering(IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));
        this.fontRenderer.drawString(rateLabelText(), DETAIL_RATE_LABEL_X, DETAIL_RATE_Y,
                isHoveringRateLabel(mouseX, mouseY, true) ? 0xFFFFD27F : BUTTON_TEXT);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                "\u2713", false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        BlockPos pos = this.container.getTileEntity().getPos().offset(direction);
        this.fontRenderer.drawString(truncate(I18n.format(
                        "gui.infwhat.io.detail.facing", getNeighborName(direction)),
                GUI_WIDTH - 24), 12, 56, BUTTON_TEXT);
        this.fontRenderer.drawString(truncate(I18n.format(
                        "gui.infwhat.io.detail.id", getNeighborId(direction)),
                GUI_WIDTH - 24), 12, 70, COLOR_TEXT);
        this.fontRenderer.drawString(I18n.format("gui.infwhat.io.detail.pos",
                        pos.getX(), pos.getY(), pos.getZ()),
                12, 84, COLOR_TEXT);
        this.fontRenderer.drawString(I18n.format("gui.infwhat.io.detail.hint"),
                12, 137, COLOR_TEXT);
    }

    private void renderRedstonePage(int mouseX, int mouseY) {
        this.fontRenderer.drawString(truncate(I18n.format("gui.infwhat.redstone.title"),
                IO_MODE_X - 16), 12, 8, BUTTON_TEXT);
        RedstoneMode mode = this.container.getRedstoneMode();
        RedstoneMode[] modes = RedstoneMode.values();
        for (int i = 0; i < modes.length && i < RS_MODE_X.length; i++) {
            drawModeButton(i, I18n.format("gui.infwhat.redstone.mode." + modes[i].key()),
                    mode == modes[i], mouseX, mouseY);
        }
        drawCenteredInBox(I18n.format("gui.infwhat.redstone.pulse.on"),
                12, 40, RS_PULSE_ON_LABEL_Y);
        drawCenteredInBox(I18n.format("gui.infwhat.redstone.pulse.off"),
                10, 80, RS_PULSE_OFF_LABEL_Y);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                "\u2713", false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        this.fontRenderer.drawString(truncate(redstoneStatus(), 74),
                14, RS_STATUS_Y, redstoneStatusColor());
        this.fontRenderer.drawString(truncate(I18n.format("gui.infwhat.redstone.comparator",
                this.container.getTileEntity().getComparatorOutput()), 74),
                98, RS_STATUS_Y, COLOR_TEXT);
        this.fontRenderer.drawString(truncate(I18n.format(
                "gui.infwhat.redstone.pulse.note"), 74), 14, RS_NOTE_Y, COLOR_TEXT);
        this.fontRenderer.drawString(truncate(I18n.format(
                "gui.infwhat.redstone.pulse.range"), 74), 98, RS_NOTE_Y, COLOR_TEXT);
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                "\u00d7", false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            this.fontRenderer.drawString(truncate(I18n.format(
                    "gui.infwhat.redstone.help." + (i + 1)), 158),
                    14, RS_HELP_Y + i * RS_HELP_LINE, COLOR_TEXT);
        }
    }

    private void renderBatchPage(int mouseX, int mouseY) {
        this.fontRenderer.drawString(truncate(I18n.format("gui.infwhat.batch.title"),
                IO_MODE_X - 16), 12, 8, BUTTON_TEXT);
        drawCenteredInBox(I18n.format("gui.infwhat.batch.label"),
                BATCH_LABEL_BOX_LEFT, BATCH_LABEL_BOX_WIDTH, BATCH_LABEL_Y);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                "\u2713", false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        this.fontRenderer.drawString(truncate(I18n.format("gui.infwhat.batch.info.period",
                this.container.getBatchSize()), 77), 12, BATCH_INFO_Y, BUTTON_TEXT);
        this.fontRenderer.drawString(truncate(I18n.format("gui.infwhat.batch.info.burst",
                Long.toString((long) this.container.getBatchSize()
                        * this.container.getTotalEffectiveRate())), 77),
                96, BATCH_INFO_Y, BUTTON_TEXT);
        this.fontRenderer.drawString(truncate(I18n.format(
                "gui.infwhat.batch.note.once"), 77), 12, BATCH_NOTE_Y, COLOR_TEXT);
        this.fontRenderer.drawString(truncate(I18n.format(
                "gui.infwhat.batch.note.gated"), 77), 96, BATCH_NOTE_Y, COLOR_TEXT);
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                "\u00d7", false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            this.fontRenderer.drawString(truncate(I18n.format(
                    "gui.infwhat.batch.help." + (i + 1)), 158),
                    14, BATCH_HELP_Y + i * BATCH_HELP_LINE, COLOR_TEXT);
        }
    }

    private void drawModeButton(int index, String label, boolean active, int mouseX, int mouseY) {
        int x = RS_MODE_X[index];
        boolean hovered = isHovering(x, RS_MODE_Y, RS_MODE_WIDTH, RS_MODE_HEIGHT, mouseX, mouseY);
        if (active || hovered) {
            drawRect(x + 2, RS_MODE_Y + 2,
                    x + RS_MODE_WIDTH - 1, RS_MODE_Y + RS_MODE_HEIGHT - 1,
                    hovered ? BUTTON_HOVER : BUTTON_ON);
        }
        if (index > 0) {
            drawRect(x, RS_MODE_Y, x + 1, RS_MODE_Y + RS_MODE_HEIGHT, BUTTON_BORDER);
        }
        this.drawCenteredString(this.fontRenderer, label,
                x + RS_MODE_WIDTH / 2, RS_MODE_Y + (RS_MODE_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void drawCenteredInBox(String label, int boxX, int boxWidth, int y) {
        int x = boxX + (boxWidth - this.fontRenderer.getStringWidth(label)) / 2;
        this.fontRenderer.drawString(label, x, y, BUTTON_TEXT);
    }

    private int redstoneStatusColor() {
        if (!this.container.isOutputEnabled() || this.container.getTotalEffectiveRate() <= 0L) {
            return COLOR_TEXT;
        }
        return this.container.getTileEntity().isRedstoneAllowed() ? 0xFF7CE38B : 0xFFFFC66D;
    }

    private String redstoneStatus() {
        if (!this.container.isOutputEnabled()) {
            return I18n.format("gui.infwhat.redstone.status.output_off");
        }
        if (this.container.getTotalEffectiveRate() <= 0L) {
            return I18n.format("gui.infwhat.redstone.status.no_sides");
        }
        if (!this.container.getTileEntity().isRedstoneAllowed()) {
            switch (this.container.getRedstoneMode()) {
                case HIGH:
                    return I18n.format("gui.infwhat.redstone.status.waiting_signal");
                case LOW:
                    return I18n.format("gui.infwhat.redstone.status.waiting_low");
                case PULSE:
                    return I18n.format("gui.infwhat.redstone.status.pulse_off");
                case ALWAYS:
                default:
                    return I18n.format("gui.infwhat.redstone.status.running");
            }
        }
        return I18n.format("gui.infwhat.redstone.status.running");
    }

    private void submitPulse() {
        try {
            int onTicks = Integer.parseInt(this.pulseOnField.getText().trim());
            int offTicks = Integer.parseInt(this.pulseOffField.getText().trim());
            if (onTicks < InfItemGeneratorTileEntity.MIN_PULSE_TICKS
                    || onTicks > InfItemGeneratorTileEntity.MAX_PULSE_TICKS
                    || offTicks < InfItemGeneratorTileEntity.MIN_PULSE_TICKS
                    || offTicks > InfItemGeneratorTileEntity.MAX_PULSE_TICKS) {
                throw new NumberFormatException();
            }
            ModNetworking.sendRedstone(this.container.windowId, false,
                    this.container.getRedstoneMode().ordinal(), onTicks, offTicks);
            this.pulseOnField.setText(Integer.toString(onTicks));
            this.pulseOffField.setText(Integer.toString(offTicks));
        } catch (NumberFormatException exception) {
            showStatus("gui.infwhat.redstone.pulse.invalid", TextFormatting.RED);
        }
    }

    private void submitBatch() {
        try {
            int size = Integer.parseInt(this.batchSizeField.getText().trim());
            if (size < InfItemGeneratorTileEntity.MIN_BATCH_SIZE
                    || size > InfItemGeneratorTileEntity.MAX_BATCH_SIZE) {
                throw new NumberFormatException();
            }
            ModNetworking.sendBatch(this.container.windowId, false, size);
            this.batchSizeField.setText(Integer.toString(size));
        } catch (NumberFormatException exception) {
            showStatus("gui.infwhat.batch.invalid", TextFormatting.RED);
        }
    }

    private String getNeighborName(EnumFacing direction) {
        World world = this.mc.world;
        if (world == null) {
            return "-";
        }
        BlockPos pos = this.container.getTileEntity().getPos().offset(direction);
        if (!world.isBlockLoaded(pos)) {
            return "-";
        }
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().isAir(state, world, pos) ? "-"
                : state.getBlock().getLocalizedName();
    }

    private String getNeighborId(EnumFacing direction) {
        World world = this.mc.world;
        if (world == null) {
            return "-";
        }
        BlockPos pos = this.container.getTileEntity().getPos().offset(direction);
        if (!world.isBlockLoaded(pos)) {
            return "-";
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock().isAir(state, world, pos)) {
            return "-";
        }
        ResourceLocation id = Block.REGISTRY.getNameForObject(state.getBlock());
        return id == null ? "-" : id.toString();
    }

    private void drawButton(int x, int y, int width, int height, String label,
                            boolean enabled, boolean hovered) {
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, BUTTON_BORDER);
        drawRect(x, y, x + width, y + height,
                hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF));
        this.drawCenteredString(this.fontRenderer, label, x + width / 2,
                y + (height - 8) / 2, BUTTON_TEXT);
    }

    private void drawFaceButton(int x, int y, int width, int height, EnumFacing direction,
                                boolean enabled, boolean hovered) {
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, BUTTON_BORDER);
        int fill = enabled ? (hovered ? FACE_ON_HOVER : BUTTON_ON)
                : (hovered ? FACE_OFF_HOVER : BUTTON_OFF);
        drawRect(x, y, x + width, y + height, fill);
        this.drawCenteredString(this.fontRenderer,
                I18n.format("gui.infwhat.items.io.side." + direction.getName()),
                x + width / 2, y + (height - 8) / 2, BUTTON_TEXT);
    }

    private void renderStoredInfo() {
        ItemStack stored = this.container.getStoredItem();
        if (stored.isEmpty()) {
            return;
        }
        int maxWidth = GUI_WIDTH - 24;
        String name = truncate(stored.getDisplayName(), maxWidth);
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stored.getItem());
        String id = truncate(key == null ? "unknown" : key.toString(), maxWidth);
        this.drawCenteredString(this.fontRenderer, name, GUI_WIDTH / 2, 24, COLOR_TITLE);
        this.drawCenteredString(this.fontRenderer, id, GUI_WIDTH / 2, 35, COLOR_TEXT);
    }

    private String truncate(String value, int maxWidth) {
        if (this.fontRenderer.getStringWidth(value) > maxWidth) {
            return this.fontRenderer.trimStringToWidth(value,
                    maxWidth - this.fontRenderer.getStringWidth("...")) + "...";
        }
        return value;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        int x = mouseX - this.guiLeft;
        int y = mouseY - this.guiTop;
        if (mouseButton == 0) {
            GuiTextField field = inputFieldAt(x, y);
            if (field != null) {
                clearEditingField();
                this.editingField = field;
                field.setFocused(true);
                field.mouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
            clearEditingField();
            if (isInside(x, y, TAB_START_X, TAB_TOP_Y, mainTabWidth(), TAB_HEIGHT)) {
                if (this.page.isSecondary()) {
                    playButtonSound();
                    returnToMainPage();
                }
                return;
            }
            if (isInside(x, y, ioTabX(), TAB_TOP_Y, tabWidth(ioTabLabel()), TAB_HEIGHT)) {
                if (this.page != GeneratorPage.IO_LIST) {
                    playButtonSound();
                    setPage(GeneratorPage.IO_LIST, -1);
                }
                return;
            }
            if (isInside(x, y, redstoneTabX(), TAB_TOP_Y,
                    tabWidth(redstoneTabLabel()), TAB_HEIGHT)) {
                if (!this.page.isRedstone()) {
                    playButtonSound();
                    setPage(GeneratorPage.REDSTONE, -1);
                }
                return;
            }
            if (isInside(x, y, batchTabX(), TAB_TOP_Y, tabWidth(batchTabLabel()), TAB_HEIGHT)) {
                if (!this.page.isBatch()) {
                    playButtonSound();
                    setPage(GeneratorPage.BATCH, -1);
                }
                return;
            }
            if (this.page == GeneratorPage.MAIN) {
                if (isInside(x, y, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                        MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT)) {
                    playButtonSound();
                    this.mc.playerController.sendEnchantPacket(this.container.windowId,
                            InfItemGeneratorContainer.BUTTON_TOGGLE_INFINITE);
                    return;
                }
                if (isInside(x, y, MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT)) {
                    playButtonSound();
                    setPage(GeneratorPage.IO_LIST, -1);
                    return;
                }
            } else if (this.page.isRedstone()) {
                RedstoneMode[] modes = RedstoneMode.values();
                for (int i = 0; i < modes.length && i < RS_MODE_X.length; i++) {
                    if (isInside(x, y, RS_MODE_X[i], RS_MODE_Y, RS_MODE_WIDTH, RS_MODE_HEIGHT)) {
                        playButtonSound();
                        this.mc.playerController.sendEnchantPacket(this.container.windowId,
                                InfItemGeneratorContainer.BUTTON_REDSTONE_MODE_BASE + i);
                        return;
                    }
                }
                if (isInside(x, y, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitPulse();
                    return;
                }
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    returnToMainPage();
                    return;
                }
            } else if (this.page.isBatch()) {
                if (isInside(x, y, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitBatch();
                    return;
                }
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    returnToMainPage();
                    return;
                }
            } else if (isFaceDetailPage()) {
                EnumFacing direction = IO_SIDE_ORDER[this.faceDetail];
                if (isInsideRateLabel(x, y, true)) {
                    playButtonSound();
                    cycleRateUnit();
                    return;
                }
                if (isInside(x, y, DETAIL_BACK_X, DETAIL_BACK_Y,
                        DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT)) {
                    playButtonSound();
                    setPage(GeneratorPage.IO_LIST, -1);
                    return;
                }
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    returnToMainPage();
                    return;
                }
                if (isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
                    playButtonSound();
                    int buttonId = InfItemGeneratorContainer.BUTTON_TOGGLE_SIDE_BASE
                            + direction.getIndex();
                    this.mc.playerController.sendEnchantPacket(this.container.windowId, buttonId);
                    return;
                }
                if (isInside(x, y, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT)) {
                    playButtonSound();
                    submitSideRate(this.faceDetail);
                    return;
                }
            } else {
                if (isInsideRateLabel(x, y, false)) {
                    playButtonSound();
                    cycleRateUnit();
                    return;
                }
                if (isInside(x, y, IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT)) {
                    playButtonSound();
                    this.advancedMode = !this.advancedMode;
                    setPage(GeneratorPage.IO_LIST, -1);
                    return;
                }
                if (isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
                    playButtonSound();
                    returnToMainPage();
                    return;
                }
                if (isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
                    playButtonSound();
                    this.mc.playerController.sendEnchantPacket(this.container.windowId,
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
                            EnumFacing direction = IO_SIDE_LAYOUT[row][column];
                            if (this.advancedMode) {
                                setPage(GeneratorPage.FACE_DETAIL, direction.getIndex());
                            } else {
                                int buttonId = InfItemGeneratorContainer.BUTTON_TOGGLE_SIDE_BASE
                                        + direction.getIndex();
                                this.mc.playerController.sendEnchantPacket(
                                        this.container.windowId, buttonId);
                            }
                            return;
                        }
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (this.page.isSecondary() && keyCode == Keyboard.KEY_ESCAPE) {
            if (this.page.isFaceDetail()) {
                setPage(GeneratorPage.IO_LIST, -1);
            } else {
                returnToMainPage();
            }
            return;
        }
        if (this.page == GeneratorPage.IO_LIST
                && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)) {
            playButtonSound();
            submitOutputRate();
            return;
        }
        if (this.page.isFaceDetail()
                && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)) {
            playButtonSound();
            submitSideRate(this.faceDetail);
            return;
        }
        if (this.page.isRedstone()
                && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)) {
            playButtonSound();
            submitPulse();
            return;
        }
        if (this.page.isBatch()
                && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)) {
            playButtonSound();
            submitBatch();
            return;
        }
        GuiTextField focusedField = focusedInputField();
        if (focusedField != null) {
            // While an input field is focused, swallow every other key so the inventory
            // key (E) and hotbar keys never close the GUI or swap items mid-typing.
            focusedField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private GuiTextField focusedInputField() {
        if (this.editingField != null && this.editingField.getVisible()) {
            this.editingField.setFocused(true);
            return this.editingField;
        }
        if (this.outputRateField != null && this.outputRateField.getVisible()
                && this.outputRateField.isFocused()) {
            return this.outputRateField;
        }
        for (GuiTextField field : this.sideRateFields) {
            if (field.getVisible() && field.isFocused()) {
                return field;
            }
        }
        if (this.pulseOnField != null && this.pulseOnField.getVisible()
                && this.pulseOnField.isFocused()) {
            return this.pulseOnField;
        }
        if (this.pulseOffField != null && this.pulseOffField.getVisible()
                && this.pulseOffField.isFocused()) {
            return this.pulseOffField;
        }
        if (this.batchSizeField != null && this.batchSizeField.getVisible()
                && this.batchSizeField.isFocused()) {
            return this.batchSizeField;
        }
        return null;
    }

    private void submitOutputRate() {
        submitRate(this.outputRateField, -1);
    }

    private void submitSideRate(int side) {
        submitRate(this.sideRateFields[side], side);
    }

    private void submitRate(GuiTextField field, int side) {
        try {
            long value = Long.parseLong(field.getText().trim());
            if (value < 0L) {
                throw new NumberFormatException();
            }
            if (!this.rateUnit.isExact(value)) {
                field.setText("");
                showStatus("gui.infwhat.io.rate.inexact", TextFormatting.RED,
                        Long.toString(this.rateUnit.ticksPerUnit()));
                return;
            }
            long perTick = this.rateUnit.toPerTick(value);
            if (perTick > Integer.MAX_VALUE) {
                throw new NumberFormatException();
            }
            ModNetworking.sendOutputRate(this.container.windowId, false, side, perTick);
        } catch (NumberFormatException exception) {
            field.setText("");
            showStatus("gui.infwhat.io.rate.invalid_item", TextFormatting.RED);
        }
    }

    private void showStatus(String translationKey, TextFormatting color, Object... arguments) {
        this.mc.player.sendStatusMessage(new TextComponentTranslation(translationKey, arguments)
                .setStyle(new Style().setColor(color)), true);
    }

    private void playButtonSound() {
        this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
                SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public void onOutputRateResult(int windowId, int side, boolean success, long amount,
                                   RateRejectReason reason) {
        if (windowId != this.container.windowId) {
            return;
        }
        this.refreshCooldown = 10;
        if (side >= 0 && side < this.sideRateFields.length) {
            GuiTextField field = this.sideRateFields[side];
            if (field != this.editingField) {
                field.setText(success ? formatUnitRate(amount) : "");
            }
        } else {
            if (this.outputRateField != this.editingField) {
                this.outputRateField.setText(success ? formatUnitRate(amount) : "");
            }
            if (success) {
                for (GuiTextField field : this.sideRateFields) {
                    if (field != this.editingField) {
                        field.setText(formatUnitRate(amount));
                    }
                }
            }
        }
        if (success) {
            showStatus("gui.infwhat.io.rate.saved", TextFormatting.GREEN);
        } else {
            showStatus(itemRateRejectMessageKey(reason), TextFormatting.RED,
                    itemRateRejectArgument(reason));
        }
    }

    private String itemRateRejectMessageKey(RateRejectReason reason) {
        switch (reason) {
            case LIMIT_SIDE:
                return "gui.infwhat.io.rate.limit_side";
            case LIMIT_TOTAL:
                return "gui.infwhat.io.rate.limit_total";
            case INVALID:
                return "gui.infwhat.io.rate.invalid_item";
            default:
                return "gui.infwhat.io.rate.rejected";
        }
    }

    private Object itemRateRejectArgument(RateRejectReason reason) {
        switch (reason) {
            case LIMIT_SIDE:
                return formatUnitRate(InfinityWhatConfig.maxItemRatePerSide()) + this.rateUnit.suffix();
            case LIMIT_TOTAL:
                return formatUnitRate(InfinityWhatConfig.maxItemTotalRate()) + this.rateUnit.suffix();
            default:
                return "";
        }
    }

    private void returnToMainPage() {
        setPage(GeneratorPage.MAIN, -1);
    }

    private void renderControlTooltip(int mouseX, int mouseY) {
        int x = mouseX - this.guiLeft;
        int y = mouseY - this.guiTop;
        String tooltip = null;
        if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.items.infinite_mode.tooltip");
        } else if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_IO_X, MAIN_IO_Y,
                MAIN_IO_WIDTH, MAIN_IO_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.items.io.tooltip");
        } else if (this.page.isRedstone() && isInside(x, y, RS_MODE_X[0], RS_MODE_Y,
                RS_MODE_X[RS_MODE_X.length - 1] + RS_MODE_WIDTH - RS_MODE_X[0], RS_MODE_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.redstone.mode.tooltip");
        } else if (this.page.isRedstone()
                && (isInside(x, y, RS_PULSE_ON_FIELD_X, RS_PULSE_ON_LABEL_Y,
                        RS_PULSE_ON_FIELD_RIGHT - RS_PULSE_ON_FIELD_X, RS_PULSE_FIELD_HEIGHT)
                    || isInside(x, y, RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_LABEL_Y,
                        RS_PULSE_OFF_FIELD_RIGHT - RS_PULSE_OFF_FIELD_X, RS_PULSE_FIELD_HEIGHT))) {
            tooltip = I18n.format("gui.infwhat.redstone.pulse.tooltip");
        } else if (this.page.isBatch() && isInside(x, y, BATCH_FIELD_X, BATCH_LABEL_Y,
                BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.batch.tooltip");
        } else if (isIoPage() && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.items.io.output.tooltip");
        } else if (isIoPage() && isFaceDetailPage() && isInsideRateLabel(x, y, true)) {
            tooltip = I18n.format("gui.infwhat.io.rate.unit.tooltip", this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInsideRateLabel(x, y, false)) {
            tooltip = I18n.format("gui.infwhat.io.rate.unit.tooltip", this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInside(x, y, IO_RATE_FIELD_X,
                IO_RATE_FIELD_Y, IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.io.rate.item.tooltip");
        } else if (isIoPage() && !isFaceDetailPage()
                && isInside(x, y, IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.io.mode.tooltip");
        } else if (isIoPage() && !isFaceDetailPage() && this.advancedMode) {
            EnumFacing direction = getHoveredFace(x, y);
            if (direction != null) {
                tooltip = I18n.format("gui.infwhat.io.neighbor.tooltip",
                        getNeighborName(direction), getNeighborId(direction));
            }
        }
        if (tooltip != null) {
            this.drawHoveringText(Collections.singletonList(tooltip), mouseX, mouseY);
        }
    }

    private EnumFacing getHoveredFace(int x, int y) {
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
        return isInside(mouseX - this.guiLeft, mouseY - this.guiTop, x, y, width, height);
    }

    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
