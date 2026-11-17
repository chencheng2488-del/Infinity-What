package com.bandybus.infwhat.client.gui;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import com.bandybus.infwhat.network.ModNetworking;
import com.bandybus.infwhat.network.RateRejectReason;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ClickType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Keyboard;

import java.math.BigDecimal;
import java.util.Collections;

/** Fluid generator main page and its five-page active-output settings. */
public class GuiInfFluidGenerator extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_fluid_generator.png");
    private static final ResourceLocation IO_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_io.png");
    private static final ResourceLocation FACE_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_face.png");
    private static final ResourceLocation REDSTONE_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_redstone.png");
    private static final ResourceLocation BATCH_TEXTURE = new ResourceLocation(InfinityWhat.MODID,
            "textures/gui/container/inf_source_batch.png");

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
    private static final EnumFacing[] IO_SIDE_ORDER = EnumFacing.values();

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

    private final InfFluidGeneratorContainer container;
    private GeneratorPage page = GeneratorPage.MAIN;
    private boolean advancedMode;
    private int faceDetail = -1;
    private GuiTextField outputRateField;
    private final GuiTextField[] sideRateFields = new GuiTextField[IO_SIDE_ORDER.length];
    private GuiTextField pulseOnField;
    private GuiTextField pulseOffField;
    private GuiTextField batchSizeField;
    private GuiTextField editingField;
    private int refreshCooldown;
    private RateUnit rateUnit = RateUnit.PER_TICK;

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
        this.outputRateField = new GuiTextField(0, this.fontRendererObj,
                this.guiLeft + IO_RATE_FIELD_X, this.guiTop + IO_RATE_FIELD_Y,
                IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT);
        this.outputRateField.setMaxStringLength(32);
        this.outputRateField.setTextColor(BUTTON_TEXT);
        this.outputRateField.setEnableBackgroundDrawing(false);
        this.outputRateField.setVisible(false);

        for (int i = 0; i < this.sideRateFields.length; i++) {
            GuiTextField field = new GuiTextField(i + 1, this.fontRendererObj,
                    this.guiLeft + DETAIL_RATE_X, this.guiTop + DETAIL_RATE_Y,
                    IO_RATE_FIELD_RIGHT - DETAIL_RATE_X, DETAIL_RATE_HEIGHT);
            field.setMaxStringLength(32);
            field.setTextColor(BUTTON_TEXT);
            field.setEnableBackgroundDrawing(false);
            field.setVisible(false);
            this.sideRateFields[i] = field;
        }
        this.pulseOnField = createPulseField(
                10, RS_PULSE_ON_FIELD_X, RS_PULSE_ON_FIELD_RIGHT, RS_PULSE_ON_LABEL_Y);
        this.pulseOffField = createPulseField(
                11, RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_FIELD_RIGHT, RS_PULSE_OFF_LABEL_Y);
        this.batchSizeField = new GuiTextField(12, this.fontRendererObj,
                this.guiLeft + BATCH_FIELD_X, this.guiTop + BATCH_LABEL_Y,
                BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT);
        this.batchSizeField.setMaxStringLength(2);
        this.batchSizeField.setTextColor(BUTTON_TEXT);
        this.batchSizeField.setEnableBackgroundDrawing(false);
        this.batchSizeField.setVisible(false);
        setPage(GeneratorPage.MAIN, -1);
    }

    private GuiTextField createPulseField(int id, int x, int right, int y) {
        GuiTextField field = new GuiTextField(id, this.fontRendererObj,
                this.guiLeft + x, this.guiTop + y,
                right - x, RS_PULSE_FIELD_HEIGHT);
        field.setMaxStringLength(5);
        field.setTextColor(BUTTON_TEXT);
        field.setEnableBackgroundDrawing(false);
        field.setVisible(false);
        return field;
    }

    private void refreshOutputRateFields() {
        boolean mixed = this.container.hasMixedSideRates();
        if (!this.outputRateField.isFocused() && this.outputRateField != this.editingField) {
            this.outputRateField.setText(mixed ? ""
                    : formatUnitRate(this.container.getUniformSideRateMb()));
        }
        for (int i = 0; i < this.sideRateFields.length; i++) {
            GuiTextField field = this.sideRateFields[i];
            if (!field.isFocused() && field != this.editingField) {
                field.setText(formatUnitRate(this.container.getSideRateMb(IO_SIDE_ORDER[i])));
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

    private String formatUnitRate(long perTickMb) {
        return formatOutputRate(this.rateUnit.fromPerTick(perTickMb));
    }

    private static String formatOutputRate(long millibuckets) {
        if (millibuckets % 1000L == 0) {
            return formatBuckets(millibuckets) + " B";
        }
        return millibuckets + " mB";
    }

    private static String formatBuckets(long millibuckets) {
        return BigDecimal.valueOf(millibuckets)
                .divide(BigDecimal.valueOf(1000))
                .stripTrailingZeros()
                .toPlainString();
    }

    private boolean isInsideRateLabel(int x, int y, boolean detailPage) {
        if (detailPage) {
            return isInside(x, y, DETAIL_RATE_LABEL_X, DETAIL_RATE_Y - 1,
                    this.fontRendererObj.getStringWidth(rateLabelText()), 10);
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
        this.outputRateField.setVisible(ioList);
        if (!ioList) {
            this.outputRateField.setFocused(false);
        }
        this.pulseOnField.setVisible(redstone);
        this.pulseOffField.setVisible(redstone);
        if (!redstone) {
            this.pulseOnField.setFocused(false);
            this.pulseOffField.setFocused(false);
        }
        this.batchSizeField.setVisible(batch);
        if (!batch) {
            this.batchSizeField.setFocused(false);
        }
        for (int i = 0; i < this.sideRateFields.length; i++) {
            boolean show = isFaceDetailPage() && this.faceDetail == i;
            this.sideRateFields[i].setVisible(show);
            if (!show) {
                this.sideRateFields[i].setFocused(false);
            }
        }
    }

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
                    this.xSize, this.ySize, 368, 178);
            renderFluid();
        }
    }

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
        ResourceLocation still = stack.getFluid().getStill(stack);
        if (still == null) {
            return;
        }
        TextureAtlasSprite sprite = this.mc.getTextureMapBlocks().getAtlasSprite(still.toString());
        int tint = stack.getFluid().getColor(stack);
        float alpha = ((tint >> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.color(red, green, blue, alpha);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        int x = this.guiLeft + InfFluidGeneratorContainer.TANK_X;
        int yBottom = this.guiTop + InfFluidGeneratorContainer.TANK_Y
                + InfFluidGeneratorContainer.TANK_HEIGHT;
        int remaining = fill;
        int drawn = 0;
        while (remaining > 0) {
            int height = Math.min(16, remaining);
            drawFluidQuad(x, yBottom - drawn - height, height, sprite);
            drawn += height;
            remaining -= height;
        }

        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawFluidQuad(int x, int y, int height, TextureAtlasSprite sprite) {
        double minU = sprite.getMinU();
        double maxU = sprite.getMaxU();
        double maxV = sprite.getMaxV();
        double minV = maxV - (height / 16.0D) * (sprite.getMaxV() - sprite.getMinV());

        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + height, 0.0D).tex(minU, maxV).endVertex();
        buffer.pos(x + InfFluidGeneratorContainer.TANK_WIDTH, y + height, 0.0D)
                .tex(maxU, maxV).endVertex();
        buffer.pos(x + InfFluidGeneratorContainer.TANK_WIDTH, y, 0.0D).tex(maxU, minV).endVertex();
        buffer.pos(x, y, 0.0D).tex(minU, minV).endVertex();
        tessellator.draw();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (this.page == GeneratorPage.MAIN) {
            this.fontRendererObj.drawString(I18n.format("container.infwhat.inf_fluid_generator"),
                    12, 8, COLOR_TITLE);
            this.fontRendererObj.drawString(I18n.format("container.inventory"), 12, 86, COLOR_TITLE);
            renderFluidInfo();
            renderMainButtons(mouseX, mouseY);
        } else if (this.page.isIoPage()) {
            renderIoPage(mouseX, mouseY);
        } else if (this.page.isRedstone()) {
            renderRedstonePage(mouseX, mouseY);
        } else {
            renderBatchPage(mouseX, mouseY);
        }
        renderTabs(mouseX, mouseY);
    }

    private void renderFluidInfo() {
        FluidStack stack = this.container.getFluidStack();
        if (stack == null) {
            this.fontRendererObj.drawString(I18n.format("gui.infwhat.empty"),
                    TEXT_X, 27, COLOR_TEXT);
            return;
        }

        String id = stack.getFluid().getName();
        if (id != null && !id.contains(":")) {
            id = "minecraft:" + id;
        }
        String amount;
        if (this.container.isInfinite()) {
            amount = this.container.getAmount() + " B";
        } else {
            amount = formatBuckets(this.container.getAmount()) + " / "
                    + formatBuckets(InfinityWhatConfig.finiteCapacity()) + " B";
        }

        drawTruncated(I18n.format("gui.infwhat.fluid_name", stack.getLocalizedName()),
                21, COLOR_TITLE);
        drawTruncated(I18n.format("gui.infwhat.fluid_id", id), 34, COLOR_TEXT);
        drawTruncated(I18n.format("gui.infwhat.amount", amount), 47, COLOR_TEXT);
    }

    private void drawTruncated(String value, int y, int color) {
        if (this.fontRendererObj.getStringWidth(value) > TEXT_MAX_WIDTH) {
            value = this.fontRendererObj.trimStringToWidth(value,
                    TEXT_MAX_WIDTH - this.fontRendererObj.getStringWidth("...")) + "...";
        }
        this.fontRendererObj.drawString(value, TEXT_X, y, color);
    }

    private void renderMainButtons(int mouseX, int mouseY) {
        drawButton(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                I18n.format("gui.infwhat.fluid.clear"), false,
                isHovering(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                        mouseX, mouseY));
        drawButton(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                I18n.format("gui.infwhat.fluid.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                        mouseX, mouseY));
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
        return this.fontRendererObj.getStringWidth(label) + TAB_PADDING * 2;
    }

    private void drawTab(int x, int width, String label, boolean active, int mouseX, int mouseY) {
        boolean hovered = isHovering(x, TAB_TOP_Y, width, TAB_HEIGHT, mouseX, mouseY);
        drawRect(x - 1, TAB_TOP_Y - 1, x + width + 1, TAB_TOP_Y + TAB_HEIGHT, BUTTON_BORDER);
        drawRect(x, TAB_TOP_Y, x + width, TAB_TOP_Y + TAB_HEIGHT,
                hovered ? BUTTON_HOVER : (active ? BUTTON_ON : BUTTON_OFF));
        this.drawCenteredString(this.fontRendererObj, label, x + width / 2,
                TAB_TOP_Y + (TAB_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void renderIoPage(int mouseX, int mouseY) {
        if (isFaceDetailPage()) {
            renderFaceDetail(mouseX, mouseY);
        } else {
            renderIoList(mouseX, mouseY);
        }
    }

    private void renderIoList(int mouseX, int mouseY) {
        String title = I18n.format("gui.infwhat.io.title_total",
                formatUnitRate(this.container.getTotalEffectiveRateMb()) + this.rateUnit.suffix());
        this.fontRendererObj.drawString(truncate(title, IO_MODE_X - 16),
                12, 8, BUTTON_TEXT);
        drawButton(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT,
                I18n.format("gui.infwhat.io.mode." + (this.advancedMode ? "simple" : "advanced")),
                false, isHovering(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT,
                        mouseX, mouseY));

        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                I18n.format("gui.infwhat.fluid.io.back"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                        mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                I18n.format("gui.infwhat.fluid.io.output."
                        + (this.container.isOutputEnabled() ? "on" : "off")),
                this.container.isOutputEnabled(),
                isHovering(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                        mouseX, mouseY));

        String rateLabel = rateLabelText();
        int rateLabelX = IO_RATE_LABEL_BOX_LEFT + (IO_RATE_LABEL_BOX_RIGHT - IO_RATE_LABEL_BOX_LEFT
                - this.fontRendererObj.getStringWidth(rateLabel)) / 2;
        this.fontRendererObj.drawString(rateLabel, rateLabelX, IO_RATE_LABEL_Y,
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
                            I18n.format("gui.infwhat.fluid.io.side."
                                    + direction.getName() + (enabled ? ".on" : ".off")),
                            enabled, hovered);
                }
            }
        }
    }

    private void renderFaceDetail(int mouseX, int mouseY) {
        EnumFacing direction = IO_SIDE_ORDER[this.faceDetail];
        String title = I18n.format("gui.infwhat.io.detail.title",
                I18n.format("gui.infwhat.io.side.short." + direction.getName()));
        this.fontRendererObj.drawString(truncate(title, DETAIL_BACK_X - 16),
                12, 8, BUTTON_TEXT);
        drawButton(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                I18n.format("gui.infwhat.io.detail.back"), false,
                isHovering(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                        mouseX, mouseY));
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                I18n.format("gui.infwhat.fluid.io.back"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                        mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                I18n.format("gui.infwhat.io.detail.output."
                        + (this.container.isOutputSideEnabled(direction) ? "on" : "off")),
                this.container.isOutputSideEnabled(direction),
                isHovering(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                        mouseX, mouseY));
        this.fontRendererObj.drawString(rateLabelText(), DETAIL_RATE_LABEL_X, DETAIL_RATE_Y,
                isHoveringRateLabel(mouseX, mouseY, true) ? 0xFFFFD27F : BUTTON_TEXT);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                "\u2713", false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        BlockPos pos = this.container.getTileEntity().getPos().offset(direction);
        this.fontRendererObj.drawString(truncate(I18n.format(
                        "gui.infwhat.io.detail.facing", getNeighborName(direction)),
                        GUI_WIDTH - 24), 12, 56, BUTTON_TEXT);
        this.fontRendererObj.drawString(truncate(I18n.format(
                        "gui.infwhat.io.detail.id", getNeighborId(direction)),
                        GUI_WIDTH - 24), 12, 70, COLOR_TEXT);
        this.fontRendererObj.drawString(I18n.format("gui.infwhat.io.detail.pos",
                        pos.getX(), pos.getY(), pos.getZ()),
                12, 84, COLOR_TEXT);
        this.fontRendererObj.drawString(I18n.format("gui.infwhat.io.detail.hint"),
                12, 137, COLOR_TEXT);
    }

    private void renderRedstonePage(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(truncate(I18n.format("gui.infwhat.redstone.title"),
                        IO_MODE_X - 16), 12, 8, BUTTON_TEXT);
        RedstoneMode mode = this.container.getRedstoneMode();
        RedstoneMode[] modes = RedstoneMode.values();
        for (int i = 0; i < modes.length && i < RS_MODE_X.length; i++) {
            drawModeButton(i, I18n.format("gui.infwhat.redstone.mode." + modes[i].key()),
                    mode == modes[i], mouseX, mouseY);
        }
        drawCenteredInBox(I18n.format("gui.infwhat.redstone.pulse.on"), 12, 40, RS_PULSE_ON_LABEL_Y);
        drawCenteredInBox(I18n.format("gui.infwhat.redstone.pulse.off"), 10, 80, RS_PULSE_OFF_LABEL_Y);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                "\u2713", false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        this.fontRendererObj.drawString(truncate(redstoneStatus(), 74),
                14, RS_STATUS_Y, redstoneStatusColor());
        this.fontRendererObj.drawString(truncate(I18n.format("gui.infwhat.redstone.comparator",
                        this.container.getTileEntity().getComparatorOutput()), 74),
                98, RS_STATUS_Y, COLOR_TEXT);
        this.fontRendererObj.drawString(truncate(I18n.format(
                        "gui.infwhat.redstone.pulse.note"), 74),
                14, RS_NOTE_Y, COLOR_TEXT);
        this.fontRendererObj.drawString(truncate(I18n.format(
                        "gui.infwhat.redstone.pulse.range"), 74),
                98, RS_NOTE_Y, COLOR_TEXT);
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, "\u00d7", false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                        mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            this.fontRendererObj.drawString(truncate(I18n.format(
                            "gui.infwhat.redstone.help." + (i + 1)), 158),
                    14, RS_HELP_Y + i * RS_HELP_LINE, COLOR_TEXT);
        }
    }

    private void renderBatchPage(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(truncate(I18n.format("gui.infwhat.batch.title"),
                        IO_MODE_X - 16), 12, 8, BUTTON_TEXT);
        drawCenteredInBox(I18n.format("gui.infwhat.batch.label"),
                BATCH_LABEL_BOX_LEFT, BATCH_LABEL_BOX_WIDTH, BATCH_LABEL_Y);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y, IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                "\u2713", false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        this.fontRendererObj.drawString(truncate(I18n.format(
                        "gui.infwhat.batch.info.period", this.container.getBatchSize()), 77),
                12, BATCH_INFO_Y, BUTTON_TEXT);
        this.fontRendererObj.drawString(truncate(I18n.format(
                        "gui.infwhat.batch.info.burst", formatOutputRate(
                                (long) this.container.getBatchSize()
                                        * this.container.getTotalEffectiveRateMb())), 77),
                96, BATCH_INFO_Y, BUTTON_TEXT);
        this.fontRendererObj.drawString(truncate(I18n.format(
                        "gui.infwhat.batch.note.once"), 77),
                12, BATCH_NOTE_Y, COLOR_TEXT);
        this.fontRendererObj.drawString(truncate(I18n.format(
                        "gui.infwhat.batch.note.gated"), 77),
                96, BATCH_NOTE_Y, COLOR_TEXT);
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, "\u00d7", false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                        mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            this.fontRendererObj.drawString(truncate(I18n.format(
                            "gui.infwhat.batch.help." + (i + 1)), 158),
                    14, BATCH_HELP_Y + i * BATCH_HELP_LINE, COLOR_TEXT);
        }
    }

    private void drawModeButton(int index, String label, boolean active, int mouseX, int mouseY) {
        int x = RS_MODE_X[index];
        boolean hovered = isHovering(x, RS_MODE_Y, RS_MODE_WIDTH, RS_MODE_HEIGHT, mouseX, mouseY);
        if (active || hovered) {
            drawRect(x + 2, RS_MODE_Y + 2, x + RS_MODE_WIDTH - 1, RS_MODE_Y + RS_MODE_HEIGHT - 1,
                    hovered ? BUTTON_HOVER : BUTTON_ON);
        }
        if (index > 0) {
            drawRect(x, RS_MODE_Y, x + 1, RS_MODE_Y + RS_MODE_HEIGHT, BUTTON_BORDER);
        }
        this.drawCenteredString(this.fontRendererObj, label, x + RS_MODE_WIDTH / 2,
                RS_MODE_Y + (RS_MODE_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void drawCenteredInBox(String label, int boxX, int boxWidth, int y) {
        int x = boxX + (boxWidth - this.fontRendererObj.getStringWidth(label)) / 2;
        this.fontRendererObj.drawString(label, x, y, BUTTON_TEXT);
    }

    private int redstoneStatusColor() {
        if (!this.container.isOutputEnabled() || this.container.getTotalEffectiveRateMb() <= 0L) {
            return COLOR_TEXT;
        }
        return this.container.getTileEntity().isRedstoneAllowed() ? 0xFF7CE38B : 0xFFFFC66D;
    }

    private String redstoneStatus() {
        if (!this.container.isOutputEnabled()) {
            return I18n.format("gui.infwhat.redstone.status.output_off");
        }
        if (this.container.getTotalEffectiveRateMb() <= 0L) {
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
                default:
                    return I18n.format("gui.infwhat.redstone.status.running");
            }
        }
        return I18n.format("gui.infwhat.redstone.status.running");
    }

    private void drawButton(int x, int y, int width, int height, String label,
                            boolean enabled, boolean hovered) {
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, BUTTON_BORDER);
        drawRect(x, y, x + width, y + height,
                hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF));
        this.drawCenteredString(this.fontRendererObj, label, x + width / 2,
                y + (height - 8) / 2, BUTTON_TEXT);
    }

    private void drawFaceButton(int x, int y, int width, int height, EnumFacing direction,
                                boolean enabled, boolean hovered) {
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, BUTTON_BORDER);
        int fill = enabled ? (hovered ? FACE_ON_HOVER : BUTTON_ON)
                : (hovered ? FACE_OFF_HOVER : BUTTON_OFF);
        drawRect(x, y, x + width, y + height, fill);
        String label = I18n.format("gui.infwhat.fluid.io.side." + direction.getName());
        this.drawCenteredString(this.fontRendererObj, label, x + width / 2,
                y + (height - 8) / 2, BUTTON_TEXT);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (this.page == GeneratorPage.MAIN && (mouseButton == 0 || mouseButton == 1)
                && isOverTank(mouseX, mouseY)) {
            this.mc.playerController.windowClick(this.inventorySlots.windowId,
                    InfFluidGeneratorContainer.FLUID_SLOT_INDEX, mouseButton, ClickType.PICKUP,
                    this.mc.thePlayer);
            return;
        }

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
                    setPage(GeneratorPage.IO_LIST, -1);
                    return;
                }
            } else if (this.page.isRedstone()) {
                RedstoneMode[] modes = RedstoneMode.values();
                for (int i = 0; i < modes.length && i < RS_MODE_X.length; i++) {
                    if (isInside(x, y, RS_MODE_X[i], RS_MODE_Y, RS_MODE_WIDTH, RS_MODE_HEIGHT)) {
                        playButtonSound();
                        this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                                InfFluidGeneratorContainer.BUTTON_REDSTONE_MODE_BASE + i);
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
                if (isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
                    playButtonSound();
                    this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId,
                            InfFluidGeneratorContainer.BUTTON_TOGGLE_SIDE_BASE
                                    + direction.ordinal());
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
                            EnumFacing direction = IO_SIDE_LAYOUT[row][column];
                            if (this.advancedMode) {
                                setPage(GeneratorPage.FACE_DETAIL, direction.ordinal());
                            } else {
                                this.mc.playerController.sendEnchantPacket(
                                        this.inventorySlots.windowId,
                                        InfFluidGeneratorContainer.BUTTON_TOGGLE_SIDE_BASE
                                                + direction.ordinal());
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
            // While a field is focused, swallow every other key so E and hotbar keys never
            // close the GUI or swap items mid-typing.
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
            long value = parseFluidAmount(field);
            if (value < 0L) {
                throw new NumberFormatException();
            }
            if (!this.rateUnit.isExact(value)) {
                field.setText("");
                this.mc.thePlayer.addChatComponentMessage(new TextComponentTranslation(
                        "gui.infwhat.io.rate.inexact", Long.toString(this.rateUnit.ticksPerUnit()))
                        .setStyle(new Style().setColor(TextFormatting.RED)));
                return;
            }
            long perTick = this.rateUnit.toPerTick(value);
            if (perTick > InfFluidGeneratorTileEntity.MAX_OUTPUT_RATE_MB) {
                throw new NumberFormatException();
            }
            ModNetworking.sendOutputRate(this.inventorySlots.windowId, true, side, perTick);
        } catch (NumberFormatException exception) {
            field.setText("");
            showRateError("gui.infwhat.io.rate.invalid_fluid");
        } catch (ArithmeticException exception) {
            field.setText("");
            showRateError("gui.infwhat.io.rate.invalid_fluid");
        }
    }

    private long parseFluidAmount(GuiTextField field) {
        String value = field.getText().trim().toLowerCase(java.util.Locale.ROOT).replace(" ", "");
        if (value.endsWith("mb")) {
            return new BigDecimal(value.substring(0, value.length() - 2)).longValueExact();
        }
        if (value.endsWith("b")) {
            return new BigDecimal(value.substring(0, value.length() - 1))
                    .multiply(BigDecimal.valueOf(1000)).longValueExact();
        }
        return new BigDecimal(value).longValueExact();
    }

    private void showRateError(String translationKey) {
        this.mc.thePlayer.addChatComponentMessage(new TextComponentTranslation(translationKey)
                .setStyle(new Style().setColor(TextFormatting.RED)));
    }

    private void submitPulse() {
        try {
            int onTicks = Integer.parseInt(this.pulseOnField.getText().trim());
            int offTicks = Integer.parseInt(this.pulseOffField.getText().trim());
            if (onTicks < InfFluidGeneratorTileEntity.MIN_PULSE_TICKS
                    || onTicks > InfFluidGeneratorTileEntity.MAX_PULSE_TICKS
                    || offTicks < InfFluidGeneratorTileEntity.MIN_PULSE_TICKS
                    || offTicks > InfFluidGeneratorTileEntity.MAX_PULSE_TICKS) {
                throw new NumberFormatException();
            }
            ModNetworking.sendRedstone(this.inventorySlots.windowId, true,
                    this.container.getRedstoneMode().ordinal(), onTicks, offTicks);
            this.pulseOnField.setText(Integer.toString(onTicks));
            this.pulseOffField.setText(Integer.toString(offTicks));
        } catch (NumberFormatException exception) {
            showRateError("gui.infwhat.redstone.pulse.invalid");
        }
    }

    private void submitBatch() {
        try {
            int size = Integer.parseInt(this.batchSizeField.getText().trim());
            if (size < InfFluidGeneratorTileEntity.MIN_BATCH_SIZE
                    || size > InfFluidGeneratorTileEntity.MAX_BATCH_SIZE) {
                throw new NumberFormatException();
            }
            ModNetworking.sendBatch(this.inventorySlots.windowId, true, size);
            this.batchSizeField.setText(Integer.toString(size));
        } catch (NumberFormatException exception) {
            showRateError("gui.infwhat.batch.invalid");
        }
    }

    public void onOutputRateResult(int side, boolean success, long amount, RateRejectReason reason) {
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
            this.mc.thePlayer.addChatComponentMessage(new TextComponentTranslation(
                    "gui.infwhat.io.rate.saved")
                    .setStyle(new Style().setColor(TextFormatting.GREEN)));
        } else {
            this.mc.thePlayer.addChatComponentMessage(new TextComponentTranslation(
                    fluidRateRejectKey(reason))
                    .setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }

    private String fluidRateRejectKey(RateRejectReason reason) {
        switch (reason) {
            case LIMIT_SIDE:
                return "gui.infwhat.io.rate.limit_side";
            case LIMIT_TOTAL:
                return "gui.infwhat.io.rate.limit_total";
            case INVALID:
                return "gui.infwhat.io.rate.invalid_fluid";
            default:
                return "gui.infwhat.io.rate.rejected";
        }
    }

    private void returnToMainPage() {
        setPage(GeneratorPage.MAIN, -1);
    }

    private String getNeighborName(EnumFacing direction) {
        if (this.mc.theWorld == null) {
            return "-";
        }
        BlockPos pos = this.container.getTileEntity().getPos().offset(direction);
        if (!this.mc.theWorld.isBlockLoaded(pos)) {
            return "-";
        }
        net.minecraft.block.state.IBlockState state = this.mc.theWorld.getBlockState(pos);
        return state.getBlock().isAir(state, this.mc.theWorld, pos)
                ? "-" : state.getBlock().getLocalizedName();
    }

    private String getNeighborId(EnumFacing direction) {
        if (this.mc.theWorld == null) {
            return "-";
        }
        BlockPos pos = this.container.getTileEntity().getPos().offset(direction);
        if (!this.mc.theWorld.isBlockLoaded(pos)) {
            return "-";
        }
        net.minecraft.block.state.IBlockState state = this.mc.theWorld.getBlockState(pos);
        if (state.getBlock().isAir(state, this.mc.theWorld, pos)) {
            return "-";
        }
        ResourceLocation key = state.getBlock().getRegistryName();
        return key == null ? "-" : key.toString();
    }

    private boolean isOverTank(double mouseX, double mouseY) {
        int x = this.guiLeft + InfFluidGeneratorContainer.TANK_X;
        int y = this.guiTop + InfFluidGeneratorContainer.TANK_Y;
        return mouseX >= x && mouseX < x + InfFluidGeneratorContainer.TANK_WIDTH
                && mouseY >= y && mouseY < y + InfFluidGeneratorContainer.TANK_HEIGHT;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
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
        if (isIoPage()) {
            if (this.outputRateField.getVisible()) {
                this.outputRateField.drawTextBox();
            }
            for (GuiTextField field : this.sideRateFields) {
                if (field.getVisible()) {
                    field.drawTextBox();
                }
            }
            if (this.page == GeneratorPage.IO_LIST && this.container.hasMixedSideRates()
                    && this.outputRateField.getText().length() == 0
                    && !this.outputRateField.isFocused()) {
                this.fontRendererObj.drawString(I18n.format("gui.infwhat.io.rate.mixed"),
                        this.guiLeft + IO_RATE_FIELD_X, this.guiTop + IO_RATE_FIELD_Y + 1,
                        0xFF68757C);
            }
        } else if (this.page.isRedstone()) {
            this.pulseOnField.drawTextBox();
            this.pulseOffField.drawTextBox();
        } else if (this.page.isBatch()) {
            this.batchSizeField.drawTextBox();
        }
        renderControlTooltip(mouseX, mouseY);
    }

    private void renderControlTooltip(int mouseX, int mouseY) {
        int x = mouseX - this.guiLeft;
        int y = mouseY - this.guiTop;
        String tooltip = null;
        if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_CLEAR_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.fluid.clear.tooltip");
        } else if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_IO_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.fluid.io.tooltip");
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
            tooltip = I18n.format("gui.infwhat.fluid.io.output.tooltip");
        } else if (isIoPage() && isInsideRateLabel(x, y, isFaceDetailPage())) {
            tooltip = I18n.format("gui.infwhat.io.rate.unit.tooltip", this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInside(x, y, IO_RATE_FIELD_X,
                IO_RATE_FIELD_Y, IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.io.rate.fluid.tooltip");
        } else if (isIoPage() && isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y,
                IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
            tooltip = I18n.format("gui.infwhat.fluid.io.back.tooltip");
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

    private String truncate(String value, int maxWidth) {
        if (this.fontRendererObj.getStringWidth(value) > maxWidth) {
            return this.fontRendererObj.trimStringToWidth(value,
                    maxWidth - this.fontRendererObj.getStringWidth("...")) + "...";
        }
        return value;
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return isInside(mouseX - this.guiLeft, mouseY - this.guiTop, x, y, width, height);
    }

    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private void playButtonSound() {
        this.mc.getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public int getContainerId() {
        return this.inventorySlots.windowId;
    }
}
