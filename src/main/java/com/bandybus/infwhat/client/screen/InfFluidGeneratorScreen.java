package com.bandybus.infwhat.client.screen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.network.ModNetworking;
import com.bandybus.infwhat.network.RateRejectReason;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.math.BigDecimal;

/** Fluid generator main page and its active-output I/O settings page. */
public class InfFluidGeneratorScreen extends ContainerScreen<InfFluidGeneratorMenu> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(InfinityWhat.MOD_ID,
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
    private TextFieldWidget outputRateField;
    private final TextFieldWidget[] sideRateFields = new TextFieldWidget[IO_SIDE_ORDER.length];
    private TextFieldWidget pulseOnField;
    private TextFieldWidget pulseOffField;
    private TextFieldWidget batchSizeField;
    /** Field the player is currently typing in; protected from any server-driven refresh. */
    private TextFieldWidget editingField;
    private int refreshCooldown;
    private RateUnit rateUnit = RateUnit.PER_TICK;

    public InfFluidGeneratorScreen(InfFluidGeneratorMenu menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.outputRateField = new TextFieldWidget(this.font,
                this.leftPos + IO_RATE_FIELD_X, this.topPos + IO_RATE_FIELD_Y,
                IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.input")
                        .getString());
        this.outputRateField.setMaxLength(32);
        this.outputRateField.setTextColor(BUTTON_TEXT);
        this.outputRateField.setBordered(false);
        this.outputRateField.setVisible(isIoPage());
        this.children.add(this.outputRateField);

        for (int i = 0; i < this.sideRateFields.length; i++) {
            TextFieldWidget field = new TextFieldWidget(this.font,
                    this.leftPos + DETAIL_RATE_X, this.topPos + DETAIL_RATE_Y,
                    IO_RATE_FIELD_RIGHT - DETAIL_RATE_X, DETAIL_RATE_HEIGHT,
                    new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.side.rate.input")
                            .getString());
            field.setMaxLength(32);
            field.setTextColor(BUTTON_TEXT);
            field.setBordered(false);
            field.setVisible(false);
            this.sideRateFields[i] = field;
            this.children.add(field);
        }
        this.pulseOnField = createPulseField(
                RS_PULSE_ON_FIELD_X, RS_PULSE_ON_FIELD_RIGHT, RS_PULSE_ON_LABEL_Y);
        this.pulseOffField = createPulseField(
                RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_FIELD_RIGHT, RS_PULSE_OFF_LABEL_Y);
        this.batchSizeField = new TextFieldWidget(this.font,
                this.leftPos + BATCH_FIELD_X, this.topPos + BATCH_LABEL_Y,
                BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".batch.input")
                        .getString());
        this.batchSizeField.setMaxLength(2);
        this.batchSizeField.setTextColor(BUTTON_TEXT);
        this.batchSizeField.setBordered(false);
        this.batchSizeField.setVisible(false);
        this.children.add(this.batchSizeField);
        setPage(GeneratorPage.MAIN, -1);
    }

    private TextFieldWidget createPulseField(int x, int right, int y) {
        TextFieldWidget field = new TextFieldWidget(this.font,
                this.leftPos + x, this.topPos + y,
                right - x, RS_PULSE_FIELD_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.input")
                        .getString());
        field.setMaxLength(5);
        field.setTextColor(BUTTON_TEXT);
        field.setBordered(false);
        field.setVisible(false);
        this.children.add(field);
        return field;
    }

    private void refreshOutputRateFields() {
        boolean mixed = this.menu.hasMixedSideRates();
        if (!this.outputRateField.isFocused() && this.outputRateField != this.editingField) {
            this.outputRateField.setValue(mixed ? "" : formatUnitRate(this.menu.getUniformSideRateMb()));
            this.outputRateField.setSuggestion(mixed
                    ? new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.mixed").getString()
                    : "");
        }
        for (int i = 0; i < this.sideRateFields.length; i++) {
            TextFieldWidget field = this.sideRateFields[i];
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
        return new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.label").getString()
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
        this.outputRateField.setFocus(false);
        for (TextFieldWidget field : this.sideRateFields) {
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
            TextFieldWidget field = this.sideRateFields[i];
            boolean show = isFaceDetailPage() && this.faceDetail == i;
            if (show) {
                field.x = this.leftPos + DETAIL_RATE_X;
                field.y = this.topPos + DETAIL_RATE_Y;
                field.setWidth(IO_RATE_FIELD_RIGHT - DETAIL_RATE_X);
            }
            field.setVisible(show);
            if (!show) {
                field.setFocus(false);
            }
        }
    }

    /** The visible input field under the given GUI-relative point, or null. */
    private TextFieldWidget inputFieldAt(int x, int y) {
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
    protected void renderBg(float partialTick, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.page.isSecondary()) {
            ResourceLocation texture = isFaceDetailPage() ? FACE_TEXTURE
                    : this.page.isRedstone() ? REDSTONE_TEXTURE
                    : this.page.isBatch() ? BATCH_TEXTURE : IO_TEXTURE;
            this.minecraft.getTextureManager().bind(texture);
            AbstractGui.blit(this.leftPos, this.topPos, 0.0F, 0.0F,
                    GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            this.minecraft.getTextureManager().bind(TEXTURE);
            AbstractGui.blit(this.leftPos, this.topPos, 0.0F, 0.0F,
                    this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        if (this.page == GeneratorPage.MAIN) {
            renderFluid();
        }
    }

    private void renderFluid() {
        FluidStack stack = this.menu.getFluidStack();
        if (stack.isEmpty()) {
            return;
        }

        // 1.16.5 exposes fluid appearance through FluidAttributes.
        FluidAttributes attributes = stack.getFluid().getAttributes();
        ResourceLocation stillTexture = attributes.getStillTexture();
        if (stillTexture == null) {
            return;
        }

        int fill = Math.round(InfFluidGeneratorMenu.TANK_HEIGHT * this.menu.getFillRatio());
        if (fill <= 0) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas()
                .getSprite(stillTexture);
        int tint = attributes.getColor();
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
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(AtlasTexture.LOCATION_BLOCKS);
        enableScissor(x, yTop, x + InfFluidGeneratorMenu.TANK_WIDTH, yBottom);
        for (int drawn = 0; drawn < fill; drawn += tile) {
            int tileY = yBottom - drawn - tile;
            blitSpriteTinted(sprite, x, tileY,
                    InfFluidGeneratorMenu.TANK_WIDTH, tile, red, green, blue, alpha);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** 1.16.5 has no tinted sprite blit; draw the sprite as a POSITION_TEX_COLOR quad. */
    private void blitSpriteTinted(TextureAtlasSprite sprite,
                                  int x, int y, int width, int height,
                                  float red, float green, float blue, float alpha) {
        BufferBuilder buffer = Tessellator.getInstance().getBuilder();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float z = this.blitOffset;
        // POSITION_TEX_COLOR 的元素顺序是 Position → UV0 → Color，调用顺序必须一致；
        // 写成 color 再 uv 会少填一个元素，endVertex 抛 "Not filled all elements of the vertex".
        buffer.vertex(x, y + height, z)
                .uv(u0, v1).color(red, green, blue, alpha).endVertex();
        buffer.vertex(x + width, y + height, z)
                .uv(u1, v1).color(red, green, blue, alpha).endVertex();
        buffer.vertex(x + width, y, z)
                .uv(u1, v0).color(red, green, blue, alpha).endVertex();
        buffer.vertex(x, y, z)
                .uv(u0, v0).color(red, green, blue, alpha).endVertex();
        Tessellator.getInstance().end();
    }

    @Override
    protected void renderLabels(int mouseX, int mouseY) {
        if (this.page == GeneratorPage.MAIN) {
            this.drawString(this.font, this.title.getString(), 12,
                    8, COLOR_TITLE);
            this.drawString(this.font, this.inventory.getDisplayName().getString(),
                    12, 86, COLOR_TITLE);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        if (this.editingField != null && this.editingField.isVisible()
                && !this.editingField.isFocused()) {
            this.editingField.setFocus(true);
        }
        this.renderBackground();
        // Keep JEI, FTB and other third-party controls in their original screen coordinates.
        super.render(mouseX, mouseY, partialTick);

        if (this.page.isSecondary()) {
            if (this.refreshCooldown > 0) {
                this.refreshCooldown--;
            } else {
                refreshOutputRateFields();
            }
        }
        if (isIoPage()) {
            renderIoPage(mouseX, mouseY);
            this.outputRateField.render(mouseX, mouseY, partialTick);
            for (TextFieldWidget field : this.sideRateFields) {
                field.render(mouseX, mouseY, partialTick);
            }
        } else if (this.page.isRedstone()) {
            renderRedstonePage(mouseX, mouseY);
            this.pulseOnField.render(mouseX, mouseY, partialTick);
            this.pulseOffField.render(mouseX, mouseY, partialTick);
        } else if (this.page.isBatch()) {
            renderBatchPage(mouseX, mouseY);
            this.batchSizeField.render(mouseX, mouseY, partialTick);
        } else {
            renderFluidInfo();
            renderMainButtons(mouseX, mouseY);
        }

        renderTabs(mouseX, mouseY);
        this.renderTooltip(mouseX, mouseY);
        renderControlTooltip(mouseX, mouseY);
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

    private ITextComponent mainTabLabel() {
        return new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".page.main");
    }

    private ITextComponent ioTabLabel() {
        return new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".page.io");
    }

    private ITextComponent redstoneTabLabel() {
        return new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".page.redstone");
    }

    private ITextComponent batchTabLabel() {
        return new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".page.batch");
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

    private int tabWidth(ITextComponent label) {
        return this.font.width(label.getString()) + TAB_PADDING * 2;
    }

    private void drawTab(int x, int width, ITextComponent label,
                         boolean active, int mouseX, int mouseY) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + TAB_TOP_Y;
        boolean hovered = isHovering(x, TAB_TOP_Y, width, TAB_HEIGHT, mouseX, mouseY);
        AbstractGui.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + TAB_HEIGHT, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (active ? BUTTON_ON : BUTTON_OFF);
        AbstractGui.fill(absoluteX, absoluteY, absoluteX + width,
                absoluteY + TAB_HEIGHT, fill);
        this.drawCenteredString(this.font, label.getString(),
                absoluteX + width / 2, absoluteY + (TAB_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void renderFluidInfo() {
        FluidStack stack = this.menu.getFluidStack();
        if (stack.isEmpty()) {
            this.drawString(this.font,
                    new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".empty").getString(),
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

        drawTruncated(
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid_name", stack.getDisplayName()),
                21, COLOR_TITLE);
        drawTruncated(
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid_id", id),
                34, COLOR_TEXT);
        drawTruncated(
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

    private String truncate(String value, int maxWidth) {
        if (this.font.width(value) > maxWidth) {
            return this.font.substrByWidth(value, maxWidth - this.font.width("...")) + "...";
        }
        return value;
    }

    private void drawTruncated(ITextComponent text, int y, int color) {
        String value = text.getString();
        if (this.font.width(value) > TEXT_MAX_WIDTH) {
            value = this.font.substrByWidth(value, TEXT_MAX_WIDTH - this.font.width("...")) + "...";
        }
        this.drawString(this.font, value, this.leftPos + TEXT_X,
                this.topPos + y, color);
    }

    private void renderMainButtons(int mouseX, int mouseY) {
        drawButton(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.clear"), false,
                isHovering(MAIN_CLEAR_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
        drawButton(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_CLEAR_Y, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, mouseX, mouseY));
    }

    private void renderIoPage(int mouseX, int mouseY) {
        if (isFaceDetailPage()) {
            renderFaceDetail(mouseX, mouseY);
        } else {
            renderIoList(mouseX, mouseY);
        }
    }

    private void renderIoList(int mouseX, int mouseY) {
        String title = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.title_total",
                formatUnitRate(this.menu.getTotalEffectiveRateMb()) + this.rateUnit.suffix()).getString();
        this.drawString(this.font, truncate(title, IO_MODE_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);
        drawButton(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.mode."
                        + (this.advancedMode ? "simple" : "advanced")), false,
                isHovering(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT, mouseX, mouseY));

        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.back"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.output."
                        + (this.menu.isOutputEnabled() ? "on" : "off")),
                this.menu.isOutputEnabled(),
                isHovering(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));

        String rateLabel = rateLabelText();
        int rateLabelX = IO_RATE_LABEL_BOX_LEFT + (IO_RATE_LABEL_BOX_RIGHT - IO_RATE_LABEL_BOX_LEFT
                - this.font.width(rateLabel)) / 2;
        this.drawString(this.font, rateLabel,
                this.leftPos + rateLabelX, this.topPos + IO_RATE_LABEL_Y,
                isHoveringRateLabel(mouseX, mouseY, false) ? 0xFFFFD27F : BUTTON_TEXT);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, new StringTextComponent("\u2713"), false,
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
                    drawFaceButton(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                            direction, enabled, hovered);
                } else {
                    drawButton(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                            new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.side."
                                    + direction.getName() + (enabled ? ".on" : ".off")),
                            enabled, hovered);
                }
            }
        }
    }

    private void renderFaceDetail(int mouseX, int mouseY) {
        Direction direction = IO_SIDE_ORDER[this.faceDetail];
        String title = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.detail.title",
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.side.short."
                        + direction.getName())).getString();
        this.drawString(this.font, truncate(title, DETAIL_BACK_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);
        drawButton(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.detail.back"), false,
                isHovering(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                        mouseX, mouseY));
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.back"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.detail.output."
                        + (this.menu.isOutputSideEnabled(direction) ? "on" : "off")),
                this.menu.isOutputSideEnabled(direction), isHovering(IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));
        this.drawString(this.font, rateLabelText(),
                this.leftPos + DETAIL_RATE_LABEL_X, this.topPos + DETAIL_RATE_Y,
                isHoveringRateLabel(mouseX, mouseY, true) ? 0xFFFFD27F : BUTTON_TEXT);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                new StringTextComponent("\u2713"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        BlockPos pos = this.menu.getBlockEntity().getBlockPos().relative(direction);
        this.drawString(this.font, truncate(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.detail.facing", getNeighborName(direction)).getString(),
                        GUI_WIDTH - 24), this.leftPos + 12, this.topPos + 56, BUTTON_TEXT);
        this.drawString(this.font, truncate(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.detail.id", getNeighborId(direction)).getString(),
                        GUI_WIDTH - 24), this.leftPos + 12, this.topPos + 70, COLOR_TEXT);
        this.drawString(this.font, new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.detail.pos",
                        pos.getX(), pos.getY(), pos.getZ()).getString(),
                this.leftPos + 12, this.topPos + 84, COLOR_TEXT);
        this.drawString(this.font,
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.detail.hint").getString(),
                this.leftPos + 12, this.topPos + 137, COLOR_TEXT);
    }

    private void renderRedstonePage(int mouseX, int mouseY) {
        this.drawString(this.font,
                truncate(new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".redstone.title").getString(),
                        IO_MODE_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);
        RedstoneMode mode = this.menu.getRedstoneMode();
        RedstoneMode[] modes = RedstoneMode.values();
        for (int i = 0; i < modes.length && i < RS_MODE_X.length; i++) {
            drawModeButton(i, new TranslationTextComponent("gui." + InfinityWhat.MOD_ID
                    + ".redstone.mode." + modes[i].key()), mode == modes[i], mouseX, mouseY);
        }
        drawCenteredInBox(
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.on"),
                12, 40, RS_PULSE_ON_LABEL_Y);
        drawCenteredInBox(
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.off"),
                10, 80, RS_PULSE_OFF_LABEL_Y);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                new StringTextComponent("\u2713"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        this.drawString(this.font, truncate(redstoneStatus().getString(), 74),
                this.leftPos + 14, this.topPos + RS_STATUS_Y, redstoneStatusColor());
        this.drawString(this.font, truncate(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".redstone.comparator",
                        this.menu.getBlockEntity().getComparatorOutput()).getString(), 74),
                this.leftPos + 98, this.topPos + RS_STATUS_Y, COLOR_TEXT);
        this.drawString(this.font, truncate(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".redstone.pulse.note").getString(), 74),
                this.leftPos + 14, this.topPos + RS_NOTE_Y, COLOR_TEXT);
        this.drawString(this.font, truncate(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".redstone.pulse.range").getString(), 74),
                this.leftPos + 98, this.topPos + RS_NOTE_Y, COLOR_TEXT);
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                new StringTextComponent("\u00D7"), false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            this.drawString(this.font, truncate(new TranslationTextComponent(
                            "gui." + InfinityWhat.MOD_ID + ".redstone.help." + (i + 1)).getString(), 158),
                    this.leftPos + 14, this.topPos + RS_HELP_Y + i * RS_HELP_LINE, COLOR_TEXT);
        }
    }

    private void renderBatchPage(int mouseX, int mouseY) {
        this.drawString(this.font,
                truncate(new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".batch.title").getString(),
                        IO_MODE_X - 16),
                this.leftPos + 12, this.topPos + 8, BUTTON_TEXT);
        drawCenteredInBox(
                new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".batch.label"),
                BATCH_LABEL_BOX_LEFT, BATCH_LABEL_BOX_WIDTH, BATCH_LABEL_Y);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                new StringTextComponent("\u2713"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        this.drawString(this.font, truncate(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".batch.info.period", this.menu.getBatchSize()).getString(), 77),
                this.leftPos + 12, this.topPos + BATCH_INFO_Y, BUTTON_TEXT);
        this.drawString(this.font, truncate(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".batch.info.burst", formatOutputRate(
                                (long) this.menu.getBatchSize() * this.menu.getTotalEffectiveRateMb())).getString(), 77),
                this.leftPos + 96, this.topPos + BATCH_INFO_Y, BUTTON_TEXT);
        this.drawString(this.font, truncate(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".batch.note.once").getString(), 77),
                this.leftPos + 12, this.topPos + BATCH_NOTE_Y, COLOR_TEXT);
        this.drawString(this.font, truncate(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".batch.note.gated").getString(), 77),
                this.leftPos + 96, this.topPos + BATCH_NOTE_Y, COLOR_TEXT);
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                new StringTextComponent("\u00D7"), false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            this.drawString(this.font, truncate(new TranslationTextComponent(
                            "gui." + InfinityWhat.MOD_ID + ".batch.help." + (i + 1)).getString(), 158),
                    this.leftPos + 14, this.topPos + BATCH_HELP_Y + i * BATCH_HELP_LINE, COLOR_TEXT);
        }
    }

    private void drawModeButton(int index, ITextComponent label,
                                boolean active, int mouseX, int mouseY) {
        int x = RS_MODE_X[index];
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + RS_MODE_Y;
        boolean hovered = isHovering(x, RS_MODE_Y, RS_MODE_WIDTH, RS_MODE_HEIGHT, mouseX, mouseY);
        if (active || hovered) {
            AbstractGui.fill(absoluteX + 2, absoluteY + 2,
                    absoluteX + RS_MODE_WIDTH - 1, absoluteY + RS_MODE_HEIGHT - 1,
                    hovered ? BUTTON_HOVER : BUTTON_ON);
        }
        if (index > 0) {
            AbstractGui.fill(absoluteX, absoluteY, absoluteX + 1,
                    absoluteY + RS_MODE_HEIGHT, BUTTON_BORDER);
        }
        this.drawCenteredString(this.font, label.getString(),
                absoluteX + RS_MODE_WIDTH / 2, absoluteY + (RS_MODE_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void drawCenteredInBox(ITextComponent label,
                                   int boxX, int boxWidth, int y) {
        int x = boxX + (boxWidth - this.font.width(label.getString())) / 2;
        this.drawString(this.font, label.getString(), this.leftPos + x,
                this.topPos + y, BUTTON_TEXT);
    }

    private int redstoneStatusColor() {
        if (!this.menu.isOutputEnabled() || this.menu.getTotalEffectiveRateMb() <= 0L) {
            return COLOR_TEXT;
        }
        return this.menu.getBlockEntity().isRedstoneAllowed() ? 0xFF7CE38B : 0xFFFFC66D;
    }

    private ITextComponent redstoneStatus() {
        if (!this.menu.isOutputEnabled()) {
            return new TranslationTextComponent(
                    "gui." + InfinityWhat.MOD_ID + ".redstone.status.output_off");
        }
        if (this.menu.getTotalEffectiveRateMb() <= 0L) {
            return new TranslationTextComponent(
                    "gui." + InfinityWhat.MOD_ID + ".redstone.status.no_sides");
        }
        if (!this.menu.getBlockEntity().isRedstoneAllowed()) {
            switch (this.menu.getRedstoneMode()) {
                case HIGH:
                    return new TranslationTextComponent(
                            "gui." + InfinityWhat.MOD_ID + ".redstone.status.waiting_signal");
                case LOW:
                    return new TranslationTextComponent(
                            "gui." + InfinityWhat.MOD_ID + ".redstone.status.waiting_low");
                case PULSE:
                    return new TranslationTextComponent(
                            "gui." + InfinityWhat.MOD_ID + ".redstone.status.pulse_off");
                default:
                    return new TranslationTextComponent(
                            "gui." + InfinityWhat.MOD_ID + ".redstone.status.running");
            }
        }
        return new TranslationTextComponent(
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
        this.minecraft.player.displayClientMessage(new TranslationTextComponent(
                "gui." + InfinityWhat.MOD_ID + ".redstone.pulse.invalid")
                .withStyle(TextFormatting.RED), true);
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
        this.minecraft.player.displayClientMessage(new TranslationTextComponent(
                "gui." + InfinityWhat.MOD_ID + ".batch.invalid")
                .withStyle(TextFormatting.RED), true);
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
        return state.isAir() ? "-" : ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
    }

    private void drawButton(int x, int y, int width, int height,
                            ITextComponent label, boolean enabled, boolean hovered) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        AbstractGui.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF);
        AbstractGui.fill(absoluteX, absoluteY, absoluteX + width,
                absoluteY + height, fill);
        this.drawCenteredString(this.font, label.getString(),
                absoluteX + width / 2, absoluteY + (height - 8) / 2, BUTTON_TEXT);
    }

    /** Advanced-mode face button: color carries the on/off state, so no explicit switch text. */
    private void drawFaceButton(int x, int y, int width, int height,
                                Direction direction, boolean enabled, boolean hovered) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        AbstractGui.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = enabled ? (hovered ? FACE_ON_HOVER : BUTTON_ON)
                : (hovered ? FACE_OFF_HOVER : BUTTON_OFF);
        AbstractGui.fill(absoluteX, absoluteY, absoluteX + width,
                absoluteY + height, fill);
        ITextComponent label = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.side."
                + direction.getName());
        this.drawCenteredString(this.font, label.getString(), absoluteX + width / 2,
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
            TextFieldWidget field = inputFieldAt(x, y);
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
        TextFieldWidget focusedField = focusedInputField();
        if (focusedField != null) {
            // While an input field is focused, swallow every other key so the inventory
            // key (E) and hotbar keys never close the GUI or swap items mid-typing.
            focusedField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private TextFieldWidget focusedInputField() {
        if (this.editingField != null && this.editingField.isVisible()) {
            this.editingField.setFocus(true);
            return this.editingField;
        }
        if (this.outputRateField != null && this.outputRateField.isVisible()
                && this.outputRateField.isFocused()) {
            return this.outputRateField;
        }
        for (TextFieldWidget field : this.sideRateFields) {
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

    private void submitRate(TextFieldWidget field, int side) {
        try {
            long value = parseFluidAmount(field);
            if (value < 0) {
                throw new NumberFormatException();
            }
            if (!this.rateUnit.isExact(value)) {
                field.setValue("");
                this.minecraft.player.displayClientMessage(new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.inexact",
                        Long.toString(this.rateUnit.ticksPerUnit()))
                        .withStyle(TextFormatting.RED), true);
                return;
            }
            long perTick = this.rateUnit.toPerTick(value);
            if (perTick > InfFluidGeneratorBlockEntity.MAX_OUTPUT_RATE_MB) {
                throw new NumberFormatException();
            }
            ModNetworking.sendOutputRate(this.menu.containerId, true, side, perTick);
        } catch (NumberFormatException exception) {
            field.setValue("");
            showInvalidFluidRate();
        } catch (ArithmeticException exception) {
            field.setValue("");
            showInvalidFluidRate();
        }
    }

    private long parseFluidAmount(TextFieldWidget field) {
        String value = field.getValue().trim().toLowerCase(java.util.Locale.ROOT).replace(" ", "");
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
        return amount;
    }

    private void showInvalidFluidRate() {
        this.minecraft.player.displayClientMessage(new TranslationTextComponent(
                "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_fluid")
                .withStyle(TextFormatting.RED), true);
    }

    public int getContainerId() {
        return this.menu.containerId;
    }

    private void playButtonSound() {
        this.minecraft.getSoundManager().play(
                SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public void onOutputRateResult(int side, boolean success, long amount, RateRejectReason reason) {
        this.refreshCooldown = 10;
        if (side >= 0 && side < this.sideRateFields.length) {
            TextFieldWidget field = this.sideRateFields[side];
            if (field != this.editingField) {
                field.setValue(success ? formatUnitRate(amount) : "");
            }
        } else {
            if (this.outputRateField != this.editingField) {
                this.outputRateField.setValue(success ? formatUnitRate(amount) : "");
            }
            if (success) {
                for (TextFieldWidget field : this.sideRateFields) {
                    if (field != this.editingField) {
                        field.setValue(formatUnitRate(amount));
                    }
                }
            }
        }
        if (success) {
            this.minecraft.player.displayClientMessage(new TranslationTextComponent(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.saved")
                    .withStyle(TextFormatting.GREEN), true);
        } else {
            this.minecraft.player.displayClientMessage(fluidRateRejectMessage(reason)
                    .withStyle(TextFormatting.RED), true);
        }
    }

    private ITextComponent fluidRateRejectMessage(RateRejectReason reason) {
        switch (reason) {
            case LIMIT_SIDE:
                return new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.limit_side",
                        formatUnitRate(InfinityWhatConfig.maxFluidRatePerSideMb()) + this.rateUnit.suffix());
            case LIMIT_TOTAL:
                return new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.limit_total",
                        formatUnitRate(InfinityWhatConfig.maxFluidTotalRateMb()) + this.rateUnit.suffix());
            case INVALID:
                return new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_fluid");
            default:
                return new TranslationTextComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.rejected");
        }
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

    private void renderControlTooltip(int mouseX, int mouseY) {
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        ITextComponent tooltip = null;
        if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_CLEAR_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.clear.tooltip");
        } else if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_IO_X, MAIN_CLEAR_Y,
                MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.tooltip");
        } else if (this.page.isRedstone() && isInside(x, y, RS_MODE_X[0], RS_MODE_Y,
                RS_MODE_X[RS_MODE_X.length - 1] + RS_MODE_WIDTH - RS_MODE_X[0], RS_MODE_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".redstone.mode.tooltip");
        } else if (this.page.isRedstone()
                && (isInside(x, y, RS_PULSE_ON_FIELD_X, RS_PULSE_ON_LABEL_Y,
                        RS_PULSE_ON_FIELD_RIGHT - RS_PULSE_ON_FIELD_X, RS_PULSE_FIELD_HEIGHT)
                    || isInside(x, y, RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_LABEL_Y,
                        RS_PULSE_OFF_FIELD_RIGHT - RS_PULSE_OFF_FIELD_X, RS_PULSE_FIELD_HEIGHT))) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.tooltip");
        } else if (this.page.isBatch() && isInside(x, y, BATCH_FIELD_X, BATCH_LABEL_Y,
                BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".batch.tooltip");
        } else if (isIoPage() && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.output.tooltip");
        } else if (isIoPage() && isFaceDetailPage() && isInsideRateLabel(x, y, true)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.unit.tooltip",
                    this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInsideRateLabel(x, y, false)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.unit.tooltip",
                    this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInside(x, y, IO_RATE_FIELD_X,
                IO_RATE_FIELD_Y, IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.fluid.tooltip");
        } else if (isIoPage() && isInside(x, y, IO_CLOSE_X, IO_CLOSE_Y,
                IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".fluid.io.back.tooltip");
        } else if (isIoPage() && !isFaceDetailPage()
                && isInside(x, y, IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT)) {
            tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.mode.tooltip");
        } else if (isIoPage() && !isFaceDetailPage() && this.advancedMode) {
            Direction direction = getHoveredFace(x, y);
            if (direction != null) {
                tooltip = new TranslationTextComponent("gui." + InfinityWhat.MOD_ID + ".io.neighbor.tooltip",
                        getNeighborName(direction), getNeighborId(direction));
            }
        }
        if (tooltip != null) {
            this.renderTooltip(tooltip.getString(), mouseX, mouseY);
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

    /**
     * 1.16.5 has no {@code AbstractGui#enableScissor}; reproduce it here by converting the GUI
     * coordinates into the window-space rectangle {@link RenderSystem#enableScissor} expects.
     */
    private static void enableScissor(int x1, int y1, int x2, int y2) {
        MainWindow window = Minecraft.getInstance().window;
        double scale = window.getGuiScale();
        int sx = (int) (x1 * scale);
        int sy = (int) (window.getHeight() - y2 * scale);
        int sw = Math.max(0, (int) ((x2 - x1) * scale));
        int sh = Math.max(0, (int) ((y2 - y1) * scale));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }
}
