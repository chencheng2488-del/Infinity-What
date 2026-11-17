package com.bandybus.infwhat.client.screen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.network.ModNetworking;
import com.bandybus.infwhat.network.RateRejectReason;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;

/** Fluid generator main page and its active-output I/O settings page. */
public class InfFluidGeneratorScreen extends AbstractContainerScreen<InfFluidGeneratorMenu> {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(InfinityWhat.MOD_ID,
                    "textures/gui/container/inf_fluid_generator.png");
    private static final ResourceLocation IO_TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_io.png");
    private static final ResourceLocation FACE_TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_face.png");
    private static final ResourceLocation REDSTONE_TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_redstone.png");
    private static final ResourceLocation BATCH_TEXTURE = new ResourceLocation(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_batch.png");

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
    private static final int COLOR_TITLE = 0xFFE9F1F4;
    private static final int COLOR_TEXT = 0xFF9FB8C2;

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
                IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.input"));
        this.outputRateField.setMaxLength(32);
        this.outputRateField.setTextColor(BUTTON_TEXT);
        this.outputRateField.setBordered(false);
        this.outputRateField.setVisible(isIoPage());
        this.addWidget(this.outputRateField);

        for (int i = 0; i < this.sideRateFields.length; i++) {
            EditBox field = new EditBox(this.font,
                    this.leftPos + DETAIL_RATE_X, this.topPos + DETAIL_RATE_Y,
                    IO_RATE_FIELD_RIGHT - DETAIL_RATE_X, DETAIL_RATE_HEIGHT,
                    Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.side.rate.input"));
            field.setMaxLength(32);
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
            this.outputRateField.setValue(mixed ? "" : formatUnitRate(this.menu.getUniformSideRateMb()));
            this.outputRateField.setHint(mixed
                    ? Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.mixed")
                    : Component.empty());
        }
        for (int i = 0; i < this.sideRateFields.length; i++) {
            EditBox field = this.sideRateFields[i];
            if (!field.isFocused() && field != this.editingField) {
                field.setValue(formatUnitRate(this.menu.getSideRateMb(IO_SIDE_ORDER[i])));
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

    private String formatUnitRate(long perTickMb) {
        return formatOutputRate(this.rateUnit.fromPerTick(perTickMb));
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
        this.outputRateField.setFocused(false);
        for (EditBox field : this.sideRateFields) {
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
        this.menu.setPlayerSlotsVisible(page.showsInventory());
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
                field.setFocused(false);
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
            this.editingField.setFocused(false);
            this.editingField = null;
            this.refreshCooldown = 10;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (this.page.isSecondary()) {
            ResourceLocation texture = isFaceDetailPage() ? FACE_TEXTURE
                    : this.page.isRedstone() ? REDSTONE_TEXTURE
                    : this.page.isBatch() ? BATCH_TEXTURE : IO_TEXTURE;
            graphics.blit(texture, this.leftPos, this.topPos, 0, 0,
                    GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0,
                    this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        if (this.page == GeneratorPage.MAIN) {
            renderFluid(graphics);
        }
    }

    private void renderFluid(GuiGraphics graphics) {
        FluidStack stack = this.menu.getFluidStack();
        if (stack.isEmpty()) {
            return;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluid());
        ResourceLocation stillTexture = extensions.getStillTexture(stack);
        if (stillTexture == null) {
            return;
        }

        int fill = Math.round(InfFluidGeneratorMenu.TANK_HEIGHT * this.menu.getFillRatio());
        if (fill <= 0) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(stillTexture);
        int tint = extensions.getTintColor(stack);
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
        graphics.enableScissor(x, yTop, x + InfFluidGeneratorMenu.TANK_WIDTH, yBottom);
        for (int drawn = 0; drawn < fill; drawn += tile) {
            int tileY = yBottom - drawn - tile;
            graphics.blit(x, tileY, 0, InfFluidGeneratorMenu.TANK_WIDTH, tile,
                    sprite, red, green, blue, alpha);
        }
        graphics.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.page == GeneratorPage.MAIN) {
            graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY,
                    COLOR_TITLE, false);
            graphics.drawString(this.font, this.playerInventoryTitle,
                    this.inventoryLabelX, this.inventoryLabelY, COLOR_TITLE, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.editingField != null && this.editingField.isVisible()
                && !this.editingField.isFocused()) {
            this.editingField.setFocused(true);
        }
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        // Keep JEI, FTB and other third-party controls in their original screen coordinates.
        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.page.isSecondary()) {
            if (this.refreshCooldown > 0) {
                this.refreshCooldown--;
            } else {
                refreshOutputRateFields();
            }
        }
        if (isIoPage()) {
            renderIoPage(graphics, mouseX, mouseY);
            this.outputRateField.render(graphics, mouseX, mouseY, partialTick);
            for (EditBox field : this.sideRateFields) {
                field.render(graphics, mouseX, mouseY, partialTick);
            }
        } else if (this.page.isRedstone()) {
            renderRedstonePage(graphics, mouseX, mouseY);
            this.pulseOnField.render(graphics, mouseX, mouseY, partialTick);
            this.pulseOffField.render(graphics, mouseX, mouseY, partialTick);
        } else if (this.page.isBatch()) {
            renderBatchPage(graphics, mouseX, mouseY);
            this.batchSizeField.render(graphics, mouseX, mouseY, partialTick);
        } else {
            renderFluidInfo(graphics);
            renderMainButtons(graphics, mouseX, mouseY);
        }

        renderTabs(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderControlTooltip(graphics, mouseX, mouseY);
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        drawTab(graphics, TAB_START_X, mainTabWidth(), mainTabLabel(),
                this.page == GeneratorPage.MAIN, mouseX, mouseY);
        drawTab(graphics, ioTabX(), tabWidth(ioTabLabel()), ioTabLabel(),
                isIoPage(), mouseX, mouseY);
        drawTab(graphics, redstoneTabX(), tabWidth(redstoneTabLabel()), redstoneTabLabel(),
                this.page.isRedstone(), mouseX, mouseY);
        drawTab(graphics, batchTabX(), tabWidth(batchTabLabel()), batchTabLabel(),
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

    private void drawTab(GuiGraphics graphics, int x, int width, Component label,
                         boolean active, int mouseX, int mouseY) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + TAB_TOP_Y;
        boolean hovered = isHovering(x, TAB_TOP_Y, width, TAB_HEIGHT, mouseX, mouseY);
        graphics.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + TAB_HEIGHT, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (active ? BUTTON_ON : BUTTON_OFF);
        graphics.fill(absoluteX, absoluteY, absoluteX + width, absoluteY + TAB_HEIGHT, fill);
        graphics.drawCenteredString(this.font, label,
                absoluteX + width / 2, absoluteY + (TAB_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void renderFluidInfo(GuiGraphics graphics) {
        FluidStack stack = this.menu.getFluidStack();
        if (stack.isEmpty()) {
            graphics.drawString(this.font,
                    Component.translatable("gui." + InfinityWhat.MOD_ID + ".empty"),
                    this.leftPos + TEXT_X, this.topPos + 27, COLOR_TEXT, false);
            return;
        }

        ResourceLocation key = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        String id = key == null ? "unknown" : key.toString();
        String amount;
        if (this.menu.isInfinite()) {
            amount = this.menu.getAmount() + " B";
        } else {
            amount = formatBuckets(this.menu.getAmount()) + " / "
                    + formatBuckets(InfinityWhatConfig.finiteCapacity()) + " B";
        }

        drawTruncated(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid_name", stack.getDisplayName()),
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

    private String truncate(String value, int maxWidth) {
        if (this.font.width(value) > maxWidth) {
            return this.font.plainSubstrByWidth(value, maxWidth - this.font.width("...")) + "...";
        }
        return value;
    }

    private void drawTruncated(GuiGraphics graphics, Component text, int y, int color) {
        String value = text.getString();
        if (this.font.width(value) > TEXT_MAX_WIDTH) {
            value = this.font.plainSubstrByWidth(value, TEXT_MAX_WIDTH - this.font.width("...")) + "...";
        }
        graphics.drawString(this.font, value, this.leftPos + TEXT_X, this.topPos + y, color, false);
    }

    private void renderMainButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        drawButton(graphics, MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.clear"), false,
                isHovering(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
        drawButton(graphics, MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
    }

    private void renderIoPage(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isFaceDetailPage()) {
            renderFaceDetail(graphics, mouseX, mouseY);
        } else {
            renderIoList(graphics, mouseX, mouseY);
        }
    }

    private void renderIoList(GuiGraphics graphics, int mouseX, int mouseY) {
        String title = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.title_total",
                formatUnitRate(this.menu.getTotalEffectiveRateMb()) + this.rateUnit.suffix()).getString();
        graphics.drawString(this.font, truncate(title, IO_MODE_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT, false);
        drawButton(graphics, IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.mode."
                        + (this.advancedMode ? "simple" : "advanced")), false,
                isHovering(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT, mouseX, mouseY));

        drawButton(graphics, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.literal("×"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(graphics, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.output."
                        + (this.menu.isOutputEnabled() ? "on" : "off")),
                this.menu.isOutputEnabled(),
                isHovering(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));

        String rateLabel = rateLabelText();
        int rateLabelX = IO_RATE_LABEL_BOX_LEFT + (IO_RATE_LABEL_BOX_RIGHT - IO_RATE_LABEL_BOX_LEFT
                - this.font.width(rateLabel)) / 2;
        graphics.drawString(this.font, rateLabel,
                this.leftPos + rateLabelX, this.topPos + IO_RATE_LABEL_Y,
                isHoveringRateLabel(mouseX, mouseY, false) ? 0xFFFFD27F : BUTTON_TEXT, false);
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
                boolean hovered = isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY);
                if (this.advancedMode) {
                    drawFaceButton(graphics, x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                            direction, enabled, hovered);
                } else {
                    drawButton(graphics, x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                            Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.side."
                                    + direction.getName() + (enabled ? ".on" : ".off")),
                            enabled, hovered);
                }
            }
        }
    }

    private void renderFaceDetail(GuiGraphics graphics, int mouseX, int mouseY) {
        Direction direction = IO_SIDE_ORDER[this.faceDetail];
        String title = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.title",
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.side.short."
                        + direction.getName())).getString();
        graphics.drawString(this.font, truncate(title, DETAIL_BACK_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT, false);
        drawButton(graphics, DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.back"), false,
                isHovering(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                        mouseX, mouseY));
        drawButton(graphics, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.literal("×"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(graphics, IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.output."
                        + (this.menu.isOutputSideEnabled(direction) ? "on" : "off")),
                this.menu.isOutputSideEnabled(direction), isHovering(IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));
        graphics.drawString(this.font, rateLabelText(),
                this.leftPos + DETAIL_RATE_LABEL_X, this.topPos + DETAIL_RATE_Y,
                isHoveringRateLabel(mouseX, mouseY, true) ? 0xFFFFD27F : BUTTON_TEXT, false);
        drawButton(graphics, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                Component.literal("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        BlockPos pos = this.menu.getBlockEntity().getBlockPos().relative(direction);
        graphics.drawString(this.font, truncate(Component.translatable(
                        "gui." + InfinityWhat.MOD_ID + ".io.detail.facing", getNeighborName(direction)).getString(),
                        GUI_WIDTH - 24), this.leftPos + 12, this.topPos + 56, BUTTON_TEXT, false);
        graphics.drawString(this.font, truncate(Component.translatable(
                        "gui." + InfinityWhat.MOD_ID + ".io.detail.id", getNeighborId(direction)).getString(),
                        GUI_WIDTH - 24), this.leftPos + 12, this.topPos + 70, COLOR_TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.pos",
                        pos.getX(), pos.getY(), pos.getZ()).getString(),
                this.leftPos + 12, this.topPos + 84, COLOR_TEXT, false);
        graphics.drawString(this.font,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.detail.hint"),
                this.leftPos + 12, this.topPos + 137, COLOR_TEXT, false);
    }

    private void renderRedstonePage(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font,
                truncate(Component.translatable("gui." + InfinityWhat.MOD_ID + ".redstone.title").getString(),
                        IO_MODE_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT, false);
        RedstoneMode mode = this.menu.getRedstoneMode();
        RedstoneMode[] modes = RedstoneMode.values();
        for (int i = 0; i < modes.length && i < RS_MODE_X.length; i++) {
            drawModeButton(graphics, i, Component.translatable("gui." + InfinityWhat.MOD_ID
                    + ".redstone.mode." + modes[i].key()), mode == modes[i], mouseX, mouseY);
        }
        drawCenteredInBox(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.on"),
                12, 40, RS_PULSE_ON_LABEL_Y);
        drawCenteredInBox(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.off"),
                10, 80, RS_PULSE_OFF_LABEL_Y);
        drawButton(graphics, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                Component.literal("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        graphics.drawString(this.font, truncate(redstoneStatus().getString(), 74),
                this.leftPos + 14, this.topPos + RS_STATUS_Y, redstoneStatusColor(), false);
        graphics.drawString(this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".redstone.comparator",
                        this.menu.getBlockEntity().getComparatorOutput()).getString(), 74),
                this.leftPos + 98, this.topPos + RS_STATUS_Y, COLOR_TEXT, false);
        graphics.drawString(this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".redstone.pulse.note").getString(), 74),
                this.leftPos + 14, this.topPos + RS_NOTE_Y, COLOR_TEXT, false);
        graphics.drawString(this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".redstone.pulse.range").getString(), 74),
                this.leftPos + 98, this.topPos + RS_NOTE_Y, COLOR_TEXT, false);
        drawButton(graphics, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.literal("×"), false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            graphics.drawString(this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                            + ".redstone.help." + (i + 1)).getString(), 158),
                    this.leftPos + 14, this.topPos + RS_HELP_Y + i * RS_HELP_LINE,
                    COLOR_TEXT, false);
        }
    }

    private void renderBatchPage(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font,
                truncate(Component.translatable("gui." + InfinityWhat.MOD_ID + ".batch.title").getString(),
                        IO_MODE_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT, false);
        drawCenteredInBox(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".batch.label"),
                BATCH_LABEL_BOX_LEFT, BATCH_LABEL_BOX_WIDTH, BATCH_LABEL_Y);
        drawButton(graphics, IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                Component.literal("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        graphics.drawString(this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".batch.info.period", this.menu.getBatchSize()).getString(), 77),
                this.leftPos + 12, this.topPos + BATCH_INFO_Y, BUTTON_TEXT, false);
        graphics.drawString(this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".batch.info.burst", formatOutputRate(
                                (long) this.menu.getBatchSize() * this.menu.getTotalEffectiveRateMb())).getString(), 77),
                this.leftPos + 96, this.topPos + BATCH_INFO_Y, BUTTON_TEXT, false);
        graphics.drawString(this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".batch.note.once").getString(), 77),
                this.leftPos + 12, this.topPos + BATCH_NOTE_Y, COLOR_TEXT, false);
        graphics.drawString(this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                        + ".batch.note.gated").getString(), 77),
                this.leftPos + 96, this.topPos + BATCH_NOTE_Y, COLOR_TEXT, false);
        drawButton(graphics, IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                Component.literal("×"), false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            graphics.drawString(this.font, truncate(Component.translatable("gui." + InfinityWhat.MOD_ID
                            + ".batch.help." + (i + 1)).getString(), 158),
                    this.leftPos + 14, this.topPos + BATCH_HELP_Y + i * BATCH_HELP_LINE,
                    COLOR_TEXT, false);
        }
    }

    private void drawModeButton(GuiGraphics graphics, int index, Component label,
                                boolean active, int mouseX, int mouseY) {
        int x = RS_MODE_X[index];
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + RS_MODE_Y;
        boolean hovered = isHovering(x, RS_MODE_Y, RS_MODE_WIDTH, RS_MODE_HEIGHT, mouseX, mouseY);
        if (active || hovered) {
            graphics.fill(absoluteX + 2, absoluteY + 2,
                    absoluteX + RS_MODE_WIDTH - 1, absoluteY + RS_MODE_HEIGHT - 1,
                    hovered ? BUTTON_HOVER : BUTTON_ON);
        }
        if (index > 0) {
            graphics.fill(absoluteX, absoluteY, absoluteX + 1,
                    absoluteY + RS_MODE_HEIGHT, BUTTON_BORDER);
        }
        graphics.drawCenteredString(this.font, label,
                absoluteX + RS_MODE_WIDTH / 2, absoluteY + (RS_MODE_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void drawCenteredInBox(GuiGraphics graphics, Component label,
                                   int boxX, int boxWidth, int y) {
        int x = boxX + (boxWidth - this.font.width(label)) / 2;
        graphics.drawString(this.font, label.getString(), this.leftPos + x, this.topPos + y,
                BUTTON_TEXT, false);
    }

    private int redstoneStatusColor() {
        if (!this.menu.isOutputEnabled() || this.menu.getTotalEffectiveRateMb() <= 0L) {
            return COLOR_TEXT;
        }
        return this.menu.getBlockEntity().isRedstoneAllowed() ? 0xFF7CE38B : 0xFFFFC66D;
    }

    private Component redstoneStatus() {
        if (!this.menu.isOutputEnabled()) {
            return Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".redstone.status.output_off");
        }
        if (this.menu.getTotalEffectiveRateMb() <= 0L) {
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
            if (onTicks < InfFluidGeneratorBlockEntity.MIN_PULSE_TICKS
                    || onTicks > InfFluidGeneratorBlockEntity.MAX_PULSE_TICKS
                    || offTicks < InfFluidGeneratorBlockEntity.MIN_PULSE_TICKS
                    || offTicks > InfFluidGeneratorBlockEntity.MAX_PULSE_TICKS) {
                throw new NumberFormatException();
            }
            ModNetworking.sendRedstone(this.menu.containerId, true,
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
            if (size < InfFluidGeneratorBlockEntity.MIN_BATCH_SIZE
                    || size > InfFluidGeneratorBlockEntity.MAX_BATCH_SIZE) {
                throw new NumberFormatException();
            }
            ModNetworking.sendBatch(this.menu.containerId, true, size);
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

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height,
                            Component label, boolean enabled, boolean hovered) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        graphics.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF);
        graphics.fill(absoluteX, absoluteY, absoluteX + width, absoluteY + height, fill);
        graphics.drawCenteredString(this.font, label,
                absoluteX + width / 2, absoluteY + (height - 8) / 2, BUTTON_TEXT);
    }

    /** Advanced-mode face button: color carries the on/off state, so no explicit switch text. */
    private void drawFaceButton(GuiGraphics graphics, int x, int y, int width, int height,
                                Direction direction, boolean enabled, boolean hovered) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        graphics.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = enabled ? (hovered ? FACE_ON_HOVER : BUTTON_ON)
                : (hovered ? FACE_OFF_HOVER : BUTTON_OFF);
        graphics.fill(absoluteX, absoluteY, absoluteX + width, absoluteY + height, fill);
        Component label = Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.side."
                + direction.getName());
        graphics.drawCenteredString(this.font, label, absoluteX + width / 2,
                absoluteY + (height - 8) / 2, BUTTON_TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.page == GeneratorPage.MAIN && (button == 0 || button == 1) && isOverTank(mouseX, mouseY)) {
            this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId,
                    InfFluidGeneratorMenu.FLUID_SLOT_INDEX, button, ClickType.PICKUP, this.minecraft.player);
            return true;
        }

        int x = (int) mouseX - this.leftPos;
        int y = (int) mouseY - this.topPos;
        if (button == 0) {
            EditBox field = inputFieldAt(x, y);
            if (field != null) {
                clearEditingField();
                this.editingField = field;
                field.setFocused(true);
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
                if (isInside(x, y, MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
                    playButtonSound();
                    this.minecraft.gameMode.handleInventoryButtonClick(
                            this.menu.containerId, InfFluidGeneratorMenu.BUTTON_CLEAR_FLUID);
                    return true;
                }
                if (isInside(x, y, MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
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
                                InfFluidGeneratorMenu.BUTTON_REDSTONE_MODE_BASE + i);
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
                    int buttonId = InfFluidGeneratorMenu.BUTTON_TOGGLE_SIDE_BASE
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
                            Direction direction = IO_SIDE_LAYOUT[row][column];
                            if (this.advancedMode) {
                                setPage(GeneratorPage.FACE_DETAIL, direction.get3DDataValue());
                            } else {
                                int buttonId = InfFluidGeneratorMenu.BUTTON_TOGGLE_SIDE_BASE
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
            this.editingField.setFocused(true);
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

    private void submitOutputRate() {
        submitRate(this.outputRateField, -1);
    }

    private void submitSideRate(int side) {
        submitRate(this.sideRateFields[side], side);
    }

    private void submitRate(EditBox field, int side) {
        try {
            long value = parseFluidAmount(field);
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
            if (perTick > InfFluidGeneratorBlockEntity.MAX_OUTPUT_RATE_MB) {
                throw new NumberFormatException();
            }
            ModNetworking.sendOutputRate(this.menu.containerId, true, side, perTick);
        } catch (NumberFormatException | ArithmeticException exception) {
            field.setValue("");
            showInvalidFluidRate();
        }
    }

    private long parseFluidAmount(EditBox field) {
        String value = field.getValue().trim().toLowerCase(java.util.Locale.ROOT).replace(" ", "");
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
        return amount;
    }

    private void showInvalidFluidRate() {
        this.minecraft.player.displayClientMessage(Component.translatable(
                "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_fluid")
                .withStyle(ChatFormatting.RED), true);
    }

    public int getContainerId() {
        return this.menu.containerId;
    }

    private void playButtonSound() {
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
            this.minecraft.player.displayClientMessage(fluidRateRejectMessage(reason)
                    .copy().withStyle(ChatFormatting.RED), true);
        }
    }

    private Component fluidRateRejectMessage(RateRejectReason reason) {
        return switch (reason) {
            case LIMIT_SIDE -> Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.limit_side",
                    formatUnitRate(InfinityWhatConfig.maxFluidRatePerSideMb()) + this.rateUnit.suffix());
            case LIMIT_TOTAL -> Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.limit_total",
                    formatUnitRate(InfinityWhatConfig.maxFluidTotalRateMb()) + this.rateUnit.suffix());
            case INVALID -> Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_fluid");
            default -> Component.translatable(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.rejected");
        };
    }

    private void returnToMainPage() {
        setPage(GeneratorPage.MAIN, -1);
    }

    private boolean isOverTank(double mouseX, double mouseY) {
        int x = this.leftPos + InfFluidGeneratorMenu.TANK_X;
        int y = this.topPos + InfFluidGeneratorMenu.TANK_Y;
        return mouseX >= x && mouseX < x + InfFluidGeneratorMenu.TANK_WIDTH
                && mouseY >= y && mouseY < y + InfFluidGeneratorMenu.TANK_HEIGHT;
    }

    private void renderControlTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        Component tooltip = null;
        if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_CLEAR_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.clear.tooltip");
        } else if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_IO_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.tooltip");
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
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.output.tooltip");
        } else if (isIoPage() && isFaceDetailPage() && isInsideRateLabel(x, y, true)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.unit.tooltip",
                    this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInsideRateLabel(x, y, false)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.unit.tooltip",
                    this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInside(x, y, IO_RATE_FIELD_X,
                IO_RATE_FIELD_Y, IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".io.rate.fluid.tooltip");
        } else if (isIoPage() && isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y,
                IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
            tooltip = Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid.io.back.tooltip");
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
        if (tooltip != null) {
            graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
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
