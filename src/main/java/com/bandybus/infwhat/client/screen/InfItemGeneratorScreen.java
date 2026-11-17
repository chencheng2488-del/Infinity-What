package com.bandybus.infwhat.client.screen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import com.bandybus.infwhat.network.ModNetworking;
import com.bandybus.infwhat.network.RateRejectReason;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.ChatFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

/** Item generator main page and the shared active-output I/O pages. */
public class InfItemGeneratorScreen extends AbstractContainerScreen<InfItemGeneratorMenu> {

    public static final Identifier TEXTURE = new Identifier(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_item_generator.png");
    public static final Identifier IO_TEXTURE = new Identifier(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_io.png");
    public static final Identifier FACE_TEXTURE = new Identifier(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_face.png");
    public static final Identifier REDSTONE_TEXTURE = new Identifier(
            InfinityWhat.MOD_ID, "textures/gui/container/inf_source_redstone.png");
    public static final Identifier BATCH_TEXTURE = new Identifier(
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
    private TextFieldWidget outputRateField;
    private final TextFieldWidget[] sideRateFields = new TextFieldWidget[IO_SIDE_ORDER.length];
    private TextFieldWidget pulseOnField;
    private TextFieldWidget pulseOffField;
    private TextFieldWidget batchSizeField;
    /** Field the player is currently typing in; protected from any server-driven refresh. */
    private TextFieldWidget editingField;
    private int refreshCooldown;
    private RateUnit rateUnit = RateUnit.PER_TICK;

    public InfItemGeneratorScreen(InfItemGeneratorMenu menu) {
        this(menu, MinecraftClient.getInstance().player.inventory,
                new TranslatableComponent("container.infwhat.inf_item_generator"));
    }

    public InfItemGeneratorScreen(InfItemGeneratorMenu menu, PlayerInventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.containerWidth = GUI_WIDTH;
        this.containerHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.outputRateField = new TextFieldWidget(this.font,
                this.left + IO_RATE_FIELD_X, this.top + IO_RATE_FIELD_Y,
                IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT,
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.input").getString());
        this.outputRateField.setMaxLength(16);
        this.outputRateField.setEditableColor(BUTTON_TEXT);
        this.outputRateField.setHasBorder(false);
        this.outputRateField.setVisible(isIoPage());
        this.children.add(this.outputRateField);

        for (int i = 0; i < this.sideRateFields.length; i++) {
            TextFieldWidget field = new TextFieldWidget(this.font,
                    this.left + DETAIL_RATE_X, this.top + DETAIL_RATE_Y,
                    IO_RATE_FIELD_RIGHT - DETAIL_RATE_X, DETAIL_RATE_HEIGHT,
                    new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.side.rate.input").getString());
            field.setMaxLength(16);
            field.setEditableColor(BUTTON_TEXT);
            field.setHasBorder(false);
            field.setVisible(false);
            this.sideRateFields[i] = field;
            this.children.add(field);
        }
        this.pulseOnField = createPulseField(
                RS_PULSE_ON_FIELD_X, RS_PULSE_ON_FIELD_RIGHT, RS_PULSE_ON_LABEL_Y);
        this.pulseOffField = createPulseField(
                RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_FIELD_RIGHT, RS_PULSE_OFF_LABEL_Y);
        this.batchSizeField = new TextFieldWidget(this.font,
                this.left + BATCH_FIELD_X, this.top + BATCH_LABEL_Y,
                BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT,
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".batch.input").getString());
        this.batchSizeField.setMaxLength(2);
        this.batchSizeField.setEditableColor(BUTTON_TEXT);
        this.batchSizeField.setHasBorder(false);
        this.batchSizeField.setVisible(false);
        this.children.add(this.batchSizeField);
        setPage(GeneratorPage.MAIN, -1);
    }

    private TextFieldWidget createPulseField(int x, int right, int y) {
        TextFieldWidget field = new TextFieldWidget(this.font,
                this.left + x, this.top + y,
                right - x, RS_PULSE_FIELD_HEIGHT,
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.input").getString());
        field.setMaxLength(5);
        field.setEditableColor(BUTTON_TEXT);
        field.setHasBorder(false);
        field.setVisible(false);
        this.children.add(field);
        return field;
    }

    private void refreshOutputRateFields() {
        boolean mixed = this.container.hasMixedSideRates();
        if (!this.outputRateField.isFocused() && this.outputRateField != this.editingField) {
            this.outputRateField.setText(mixed ? "" : formatUnitRate(this.container.getUniformSideRate()));
            this.outputRateField.setSuggestion(mixed
                    ? new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.mixed")
                            .getString()
                    : "");
        }
        for (int i = 0; i < this.sideRateFields.length; i++) {
            TextFieldWidget field = this.sideRateFields[i];
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
        return new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.label").getString()
                + this.rateUnit.suffix();
    }

    private String formatUnitRate(long perTick) {
        return Long.toString(this.rateUnit.fromPerTick(perTick));
    }

    private boolean isInsideRateLabel(int x, int y, boolean detailPage) {
        if (detailPage) {
            return isInside(x, y, DETAIL_RATE_LABEL_X, DETAIL_RATE_Y - 1,
                    this.font.getStringWidth(rateLabelText()), 10);
        }
        return isInside(x, y, IO_RATE_LABEL_BOX_LEFT, IO_RATE_LABEL_Y - 1,
                IO_RATE_LABEL_BOX_RIGHT - IO_RATE_LABEL_BOX_LEFT, 10);
    }

    private boolean isHoveringRateLabel(int mouseX, int mouseY, boolean detailPage) {
        return isInsideRateLabel(mouseX - this.left, mouseY - this.top, detailPage);
    }

    private void cycleRateUnit() {
        this.outputRateField.method_1876(false);
        for (TextFieldWidget field : this.sideRateFields) {
            field.method_1876(false);
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
            this.outputRateField.method_1876(false);
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
            this.outputRateField.method_1876(false);
        }
        this.pulseOnField.setVisible(redstone);
        this.pulseOffField.setVisible(redstone);
        if (!redstone) {
            this.pulseOnField.method_1876(false);
            this.pulseOffField.method_1876(false);
        }
        this.batchSizeField.setVisible(batch);
        if (!batch) {
            this.batchSizeField.method_1876(false);
        }
        updateSideFieldVisibility();
    }

    private void updateSideFieldVisibility() {
        for (int i = 0; i < this.sideRateFields.length; i++) {
            TextFieldWidget field = this.sideRateFields[i];
            boolean show = isFaceDetailPage() && this.faceDetail == i;
            if (show) {
                field.setX(this.left + DETAIL_RATE_X);
                field.y = this.top + DETAIL_RATE_Y;
                field.setWidth(IO_RATE_FIELD_RIGHT - DETAIL_RATE_X);
            }
            field.setVisible(show);
            if (!show) {
                field.method_1876(false);
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
            this.editingField.method_1876(false);
            this.editingField = null;
            this.refreshCooldown = 10;
        }
    }

    @Override
    protected void drawBackground(float partialTick, int mouseX, int mouseY) {
        GuiLighting.disable();
        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.page.isSecondary()) {
            Identifier texture = isFaceDetailPage() ? FACE_TEXTURE
                    : this.page.isRedstone() ? REDSTONE_TEXTURE
                    : this.page.isBatch() ? BATCH_TEXTURE : IO_TEXTURE;
            this.minecraft.getTextureManager().bindTexture(texture);
            // 1.14 的 9 参 blit 把最后两个参数当成 (u 除数, v 除数)，与非方纹理的
            // (textureWidth, textureHeight) 顺序相反；8 参重载是正确顺序。
            DrawableHelper.blit(this.left, this.top, 0.0F, 0.0F,
                    GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            this.minecraft.getTextureManager().bindTexture(TEXTURE);
            DrawableHelper.blit(this.left, this.top, 0.0F, 0.0F,
                    GUI_WIDTH, GUI_HEIGHT, MAIN_TEXTURE_WIDTH, GUI_HEIGHT);
        }
    }

    @Override
    protected void drawForeground(int mouseX, int mouseY) {
        if (this.page == GeneratorPage.MAIN) {
            this.font.draw(this.title.getString(), 12, 8, 0xFFE9F1F4);
            this.font.draw(this.playerInventory.getDisplayName().getString(),
                    12, 86, 0xFFE9F1F4);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        if (this.editingField != null && this.editingField.isVisible()
                && !this.editingField.isFocused()) {
            this.editingField.method_1876(true);
        }
        // 与 Forge 1.13.2/1.14.x 分支一致：不调用 renderBackground()，避免额外压暗世界。
        // Keep JEI, FTB and other third-party controls in their original screen coordinates.
        super.render(mouseX, mouseY, partialTick);
        // ContainerScreen.render 结束时重新打开 GUI 光照，之后自绘的色块/文字会被压暗。
        GuiLighting.disable();
        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
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
            renderStoredInfo();
            renderMainButtons(mouseX, mouseY);
        }
        renderTabs(mouseX, mouseY);
        this.drawMouseoverTooltip(mouseX, mouseY);
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

    private Component mainTabLabel() {
        return new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".page.main");
    }

    private Component ioTabLabel() {
        return new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".page.io");
    }

    private Component redstoneTabLabel() {
        return new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".page.redstone");
    }

    private Component batchTabLabel() {
        return new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".page.batch");
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
        return this.font.getStringWidth(label.getString()) + TAB_PADDING * 2;
    }

    private void drawTab(int x, int width, Component label,
                         boolean active, int mouseX, int mouseY) {
        int absoluteX = this.left + x;
        int absoluteY = this.top + TAB_TOP_Y;
        boolean hovered = isHovering(x, TAB_TOP_Y, width, TAB_HEIGHT, mouseX, mouseY);
        DrawableHelper.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + TAB_HEIGHT, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (active ? BUTTON_ON : BUTTON_OFF);
        DrawableHelper.fill(absoluteX, absoluteY, absoluteX + width,
                absoluteY + TAB_HEIGHT, fill);
        this.drawCenteredString(this.font, label.getString(),
                absoluteX + width / 2, absoluteY + (TAB_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void renderMainButtons(int mouseX, int mouseY) {
        drawButton(MAIN_INFINITE_X, MAIN_INFINITE_Y,
                MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT,
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".items.infinite_mode."
                        + (this.container.isInfiniteModeEnabled() ? "on" : "off")),
                this.container.isInfiniteModeEnabled(), isHovering(MAIN_INFINITE_X, MAIN_INFINITE_Y,
                        MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT, mouseX, mouseY));
        drawButton(MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT,
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".items.io.open"), false,
                isHovering(MAIN_IO_X, MAIN_IO_Y, MAIN_IO_WIDTH, MAIN_IO_HEIGHT, mouseX, mouseY));
    }

    private void renderIoPage(int mouseX, int mouseY) {
        if (isFaceDetailPage()) {
            renderFaceDetail(mouseX, mouseY);
        } else {
            renderIoList(mouseX, mouseY);
        }
    }

    private void renderIoList(int mouseX, int mouseY) {
        String title = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.title_total",
                formatUnitRate(this.container.getTotalEffectiveRate()) + this.rateUnit.suffix()).getString();
        this.font.draw(truncate(title, IO_MODE_X - 16),
                this.left + 12, this.top + 8, BUTTON_TEXT);
        drawButton(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT,
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.mode."
                        + (this.advancedMode ? "simple" : "advanced")), false,
                isHovering(IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT, mouseX, mouseY));
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                new TextComponent("×"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".items.io.output."
                        + (this.container.isOutputEnabled() ? "on" : "off")),
                this.container.isOutputEnabled(), isHovering(IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));
        String rateLabel = rateLabelText();
        int rateLabelX = IO_RATE_LABEL_BOX_LEFT + (IO_RATE_LABEL_BOX_RIGHT - IO_RATE_LABEL_BOX_LEFT
                - this.font.getStringWidth(rateLabel)) / 2;
        this.font.draw(rateLabel,
                this.left + rateLabelX, this.top + IO_RATE_LABEL_Y,
                isHoveringRateLabel(mouseX, mouseY, false) ? 0xFFFFD27F : BUTTON_TEXT);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                new TextComponent("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        for (int row = 0; row < IO_SIDE_LAYOUT.length; row++) {
            for (int column = 0; column < IO_SIDE_LAYOUT[row].length; column++) {
                Direction direction = IO_SIDE_LAYOUT[row][column];
                int x = column == 0 ? IO_SIDE_LEFT_X : IO_SIDE_RIGHT_X;
                int y = IO_SIDE_ROW_YS[row];
                boolean enabled = this.container.isOutputSideEnabled(direction);
                boolean hovered = isHovering(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT, mouseX, mouseY);
                if (this.advancedMode) {
                    drawFaceButton(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                            direction, enabled, hovered);
                } else {
                    drawButton(x, y, IO_SIDE_WIDTH, IO_SIDE_HEIGHT,
                            new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".items.io.side."
                                    + direction.getName() + (enabled ? ".on" : ".off")),
                            enabled, hovered);
                }
            }
        }
    }

    private void renderFaceDetail(int mouseX, int mouseY) {
        Direction direction = IO_SIDE_ORDER[this.faceDetail];
        String title = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.detail.title",
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.side.short."
                        + direction.getName())).getString();
        this.font.draw(truncate(title, DETAIL_BACK_X - 16),
                this.left + 12, this.top + 8, BUTTON_TEXT);
        drawButton(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.detail.back"), false,
                isHovering(DETAIL_BACK_X, DETAIL_BACK_Y, DETAIL_BACK_WIDTH, DETAIL_BACK_HEIGHT,
                        mouseX, mouseY));
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                new TextComponent("×"), false,
                isHovering(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        drawButton(IO_OUTPUT_X, IO_OUTPUT_Y, IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT,
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.detail.output."
                        + (this.container.isOutputSideEnabled(direction) ? "on" : "off")),
                this.container.isOutputSideEnabled(direction), isHovering(IO_OUTPUT_X, IO_OUTPUT_Y,
                        IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT, mouseX, mouseY));
        this.font.draw(rateLabelText(),
                this.left + DETAIL_RATE_LABEL_X, this.top + DETAIL_RATE_Y,
                isHoveringRateLabel(mouseX, mouseY, true) ? 0xFFFFD27F : BUTTON_TEXT);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                new TextComponent("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));

        BlockPos pos = this.container.getBlockEntity().getPos().offset(direction);
        this.font.draw(truncate(new TranslatableComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.detail.facing", getNeighborName(direction)).getString(),
                        GUI_WIDTH - 24), this.left + 12, this.top + 56, BUTTON_TEXT);
        this.font.draw(truncate(new TranslatableComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.detail.id", getNeighborId(direction)).getString(),
                        GUI_WIDTH - 24), this.left + 12, this.top + 70, 0xFF9FB8C2);
        this.font.draw(new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.detail.pos",
                        pos.getX(), pos.getY(), pos.getZ()).getString(),
                this.left + 12, this.top + 84, 0xFF9FB8C2);
        this.font.draw(
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.detail.hint").getString(),
                this.left + 12, this.top + 137, 0xFF9FB8C2);
    }

    private void renderRedstonePage(int mouseX, int mouseY) {
        this.font.draw(
                truncate(new TranslatableComponent(
                        "gui." + InfinityWhat.MOD_ID + ".redstone.title").getString(),
                        IO_MODE_X - 16),
                this.left + 12, this.top + 8, BUTTON_TEXT);
        RedstoneMode mode = this.container.getRedstoneMode();
        RedstoneMode[] modes = RedstoneMode.values();
        for (int i = 0; i < modes.length && i < RS_MODE_X.length; i++) {
            drawModeButton(i, new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                    + ".redstone.mode." + modes[i].key()), mode == modes[i], mouseX, mouseY);
        }
        drawCenteredInBox(
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.on"),
                12, 40, RS_PULSE_ON_LABEL_Y);
        drawCenteredInBox(
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.off"),
                10, 80, RS_PULSE_OFF_LABEL_Y);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                new TextComponent("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        this.font.draw(truncate(redstoneStatus().getString(), 74),
                this.left + 14, this.top + RS_STATUS_Y, redstoneStatusColor());
        this.font.draw(truncate(new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                        + ".redstone.comparator",
                        this.container.getBlockEntity().getComparatorOutput()).getString(), 74),
                this.left + 98, this.top + RS_STATUS_Y, 0xFF9FB8C2);
        this.font.draw(truncate(new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                        + ".redstone.pulse.note").getString(), 74),
                this.left + 14, this.top + RS_NOTE_Y, 0xFF9FB8C2);
        this.font.draw(truncate(new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                        + ".redstone.pulse.range").getString(), 74),
                this.left + 98, this.top + RS_NOTE_Y, 0xFF9FB8C2);
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                new TextComponent("×"), false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            this.font.draw(truncate(new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                            + ".redstone.help." + (i + 1)).getString(), 158),
                    this.left + 14, this.top + RS_HELP_Y + i * RS_HELP_LINE,
                    0xFF9FB8C2);
        }
    }

    private void renderBatchPage(int mouseX, int mouseY) {
        this.font.draw(
                truncate(new TranslatableComponent(
                        "gui." + InfinityWhat.MOD_ID + ".batch.title").getString(),
                        IO_MODE_X - 16),
                this.left + 12, this.top + 8, BUTTON_TEXT);
        drawCenteredInBox(
                new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".batch.label"),
                BATCH_LABEL_BOX_LEFT, BATCH_LABEL_BOX_WIDTH, BATCH_LABEL_Y);
        drawButton(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT,
                new TextComponent("✓"), false, isHovering(IO_RATE_APPLY_X, IO_RATE_APPLY_Y,
                        IO_RATE_APPLY_WIDTH, IO_RATE_APPLY_HEIGHT, mouseX, mouseY));
        this.font.draw(truncate(new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                        + ".batch.info.period", this.container.getBatchSize()).getString(), 77),
                this.left + 12, this.top + BATCH_INFO_Y, BUTTON_TEXT);
        this.font.draw(truncate(new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                        + ".batch.info.burst", Long.toString(
                                (long) this.container.getBatchSize() * this.container.getTotalEffectiveRate())).getString(), 77),
                this.left + 96, this.top + BATCH_INFO_Y, BUTTON_TEXT);
        this.font.draw(truncate(new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                        + ".batch.note.once").getString(), 77),
                this.left + 12, this.top + BATCH_NOTE_Y, 0xFF9FB8C2);
        this.font.draw(truncate(new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                        + ".batch.note.gated").getString(), 77),
                this.left + 96, this.top + BATCH_NOTE_Y, 0xFF9FB8C2);
        drawButton(IO_CLOSE_X, IO_CLOSE_Y, IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT,
                new TextComponent("×"), false, isHovering(IO_CLOSE_X, IO_CLOSE_Y,
                        IO_CLOSE_WIDTH, IO_CLOSE_HEIGHT, mouseX, mouseY));
        for (int i = 0; i < 6; i++) {
            this.font.draw(truncate(new TranslatableComponent("gui." + InfinityWhat.MOD_ID
                            + ".batch.help." + (i + 1)).getString(), 158),
                    this.left + 14, this.top + BATCH_HELP_Y + i * BATCH_HELP_LINE,
                    0xFF9FB8C2);
        }
    }

    private void drawModeButton(int index, Component label,
                                boolean active, int mouseX, int mouseY) {
        int x = RS_MODE_X[index];
        int absoluteX = this.left + x;
        int absoluteY = this.top + RS_MODE_Y;
        boolean hovered = isHovering(x, RS_MODE_Y, RS_MODE_WIDTH, RS_MODE_HEIGHT, mouseX, mouseY);
        if (active || hovered) {
            DrawableHelper.fill(absoluteX + 2, absoluteY + 2,
                    absoluteX + RS_MODE_WIDTH - 1, absoluteY + RS_MODE_HEIGHT - 1,
                    hovered ? BUTTON_HOVER : BUTTON_ON);
        }
        if (index > 0) {
            DrawableHelper.fill(absoluteX, absoluteY, absoluteX + 1,
                    absoluteY + RS_MODE_HEIGHT, BUTTON_BORDER);
        }
        this.drawCenteredString(this.font, label.getString(),
                absoluteX + RS_MODE_WIDTH / 2, absoluteY + (RS_MODE_HEIGHT - 8) / 2, BUTTON_TEXT);
    }

    private void drawCenteredInBox(Component label,
                                   int boxX, int boxWidth, int y) {
        int x = boxX + (boxWidth - this.font.getStringWidth(label.getString())) / 2;
        this.font.draw(label.getString(), this.left + x, this.top + y, BUTTON_TEXT);
    }

    private int redstoneStatusColor() {
        if (!this.container.isOutputEnabled() || this.container.getTotalEffectiveRate() <= 0L) {
            return 0xFF9FB8C2;
        }
        return this.container.getBlockEntity().isRedstoneAllowed() ? 0xFF7CE38B : 0xFFFFC66D;
    }

    private Component redstoneStatus() {
        if (!this.container.isOutputEnabled()) {
            return new TranslatableComponent(
                    "gui." + InfinityWhat.MOD_ID + ".redstone.status.output_off");
        }
        if (this.container.getTotalEffectiveRate() <= 0L) {
            return new TranslatableComponent(
                    "gui." + InfinityWhat.MOD_ID + ".redstone.status.no_sides");
        }
        if (!this.container.getBlockEntity().isRedstoneAllowed()) {
            switch (this.container.getRedstoneMode()) {
                case HIGH:
                    return new TranslatableComponent(
                            "gui." + InfinityWhat.MOD_ID + ".redstone.status.waiting_signal");
                case LOW:
                    return new TranslatableComponent(
                            "gui." + InfinityWhat.MOD_ID + ".redstone.status.waiting_low");
                case PULSE:
                    return new TranslatableComponent(
                            "gui." + InfinityWhat.MOD_ID + ".redstone.status.pulse_off");
                default:
                    return new TranslatableComponent(
                            "gui." + InfinityWhat.MOD_ID + ".redstone.status.running");
            }
        }
        return new TranslatableComponent(
                "gui." + InfinityWhat.MOD_ID + ".redstone.status.running");
    }

    private void submitPulse() {
        try {
            int onTicks = Integer.parseInt(this.pulseOnField.getText().trim());
            int offTicks = Integer.parseInt(this.pulseOffField.getText().trim());
            if (onTicks < InfItemGeneratorBlockEntity.MIN_PULSE_TICKS
                    || onTicks > InfItemGeneratorBlockEntity.MAX_PULSE_TICKS
                    || offTicks < InfItemGeneratorBlockEntity.MIN_PULSE_TICKS
                    || offTicks > InfItemGeneratorBlockEntity.MAX_PULSE_TICKS) {
                throw new NumberFormatException();
            }
            ModNetworking.sendRedstone(this.container.syncId, false,
                    this.container.getRedstoneMode().ordinal(), onTicks, offTicks);
            this.pulseOnField.setText(Integer.toString(onTicks));
            this.pulseOffField.setText(Integer.toString(offTicks));
        } catch (NumberFormatException exception) {
            showInvalidPulse();
        }
    }

    private void showInvalidPulse() {
        this.minecraft.player.addChatMessage(new TranslatableComponent(
                "gui." + InfinityWhat.MOD_ID + ".redstone.pulse.invalid")
                .applyFormat(ChatFormat.RED), true);
    }

    private void submitBatch() {
        try {
            int size = Integer.parseInt(this.batchSizeField.getText().trim());
            if (size < InfItemGeneratorBlockEntity.MIN_BATCH_SIZE
                    || size > InfItemGeneratorBlockEntity.MAX_BATCH_SIZE) {
                throw new NumberFormatException();
            }
            ModNetworking.sendBatch(this.container.syncId, false, size);
            this.batchSizeField.setText(Integer.toString(size));
        } catch (NumberFormatException exception) {
            showInvalidBatch();
        }
    }

    private void showInvalidBatch() {
        this.minecraft.player.addChatMessage(new TranslatableComponent(
                "gui." + InfinityWhat.MOD_ID + ".batch.invalid")
                .applyFormat(ChatFormat.RED), true);
    }

    private String getNeighborName(Direction direction) {
        if (this.minecraft.world == null) {
            return "-";
        }
        BlockPos pos = this.container.getBlockEntity().getPos().offset(direction);
        if (!this.minecraft.world.isHeightValidAndBlockLoaded(pos)) {
            return "-";
        }
        BlockState state = this.minecraft.world.getBlockState(pos);
        return state.isAir() ? "-" : new TranslatableComponent(state.getBlock().getTranslationKey()).getString();
    }

    private String getNeighborId(Direction direction) {
        if (this.minecraft.world == null) {
            return "-";
        }
        BlockPos pos = this.container.getBlockEntity().getPos().offset(direction);
        if (!this.minecraft.world.isHeightValidAndBlockLoaded(pos)) {
            return "-";
        }
        BlockState state = this.minecraft.world.getBlockState(pos);
        return state.isAir() ? "-" : Registry.BLOCK.getId(state.getBlock()).toString();
    }

    private void drawButton(int x, int y, int width, int height,
                            Component label, boolean enabled, boolean hovered) {
        int absoluteX = this.left + x;
        int absoluteY = this.top + y;
        DrawableHelper.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = hovered ? BUTTON_HOVER : (enabled ? BUTTON_ON : BUTTON_OFF);
        DrawableHelper.fill(absoluteX, absoluteY, absoluteX + width, absoluteY + height, fill);
        this.drawCenteredString(this.font, label.getString(),
                absoluteX + width / 2, absoluteY + (height - 8) / 2, BUTTON_TEXT);
    }

    /** Advanced-mode face button: color carries the on/off state, so no explicit switch text. */
    private void drawFaceButton(int x, int y, int width, int height,
                                Direction direction, boolean enabled, boolean hovered) {
        int absoluteX = this.left + x;
        int absoluteY = this.top + y;
        DrawableHelper.fill(absoluteX - 1, absoluteY - 1, absoluteX + width + 1,
                absoluteY + height + 1, BUTTON_BORDER);
        int fill = enabled ? (hovered ? FACE_ON_HOVER : BUTTON_ON)
                : (hovered ? FACE_OFF_HOVER : BUTTON_OFF);
        DrawableHelper.fill(absoluteX, absoluteY, absoluteX + width, absoluteY + height, fill);
        Component label = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".items.io.side."
                + direction.getName());
        this.drawCenteredString(this.font, label.getString(), absoluteX + width / 2,
                absoluteY + (height - 8) / 2, BUTTON_TEXT);
    }

    private void renderStoredInfo() {
        ItemStack stored = this.container.getStoredItem();
        if (stored.isEmpty()) return;
        int maxWidth = GUI_WIDTH - 24;
        String name = truncate(stored.getItem().getTextComponent().getString(), maxWidth);
        Identifier key = Registry.ITEM.getId(stored.getItem());
        String id = truncate(key.toString(), maxWidth);
        this.drawCenteredString(this.font, name, this.left + GUI_WIDTH / 2,
                this.top + 24, 0xFFE9F1F4);
        this.drawCenteredString(this.font, id, this.left + GUI_WIDTH / 2,
                this.top + 35, 0xFF9FB8C2);
    }

    private String truncate(String value, int maxWidth) {
        if (this.font.getStringWidth(value) > maxWidth) {
            return this.font.trimToWidth(value, maxWidth - this.font.getStringWidth("...")) + "...";
        }
        return value;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - this.left;
        int y = (int) mouseY - this.top;
        if (button == 0) {
            TextFieldWidget field = inputFieldAt(x, y);
            if (field != null) {
                clearEditingField();
                this.editingField = field;
                field.method_1876(true);
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
                    this.minecraft.interactionManager.clickButton(
                            this.container.syncId, InfItemGeneratorMenu.BUTTON_TOGGLE_INFINITE);
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
                        this.minecraft.interactionManager.clickButton(this.container.syncId,
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
                            + direction.getId();
                    this.minecraft.interactionManager.clickButton(this.container.syncId, buttonId);
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
                    this.minecraft.interactionManager.clickButton(
                            this.container.syncId, InfItemGeneratorMenu.BUTTON_TOGGLE_OUTPUT);
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
                                setPage(GeneratorPage.FACE_DETAIL, direction.getId());
                            } else {
                                int buttonId = InfItemGeneratorMenu.BUTTON_TOGGLE_SIDE_BASE
                                        + direction.getId();
                                this.minecraft.interactionManager.clickButton(
                                        this.container.syncId, buttonId);
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

    private void submitRate(TextFieldWidget field, int side) {
        try {
            long value = Long.parseLong(field.getText().trim());
            if (value < 0) throw new NumberFormatException();
            if (!this.rateUnit.isExact(value)) {
                field.setText("");
                this.minecraft.player.addChatMessage(new TranslatableComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.inexact",
                        Long.toString(this.rateUnit.ticksPerUnit()))
                        .applyFormat(ChatFormat.RED), true);
                return;
            }
            long perTick = this.rateUnit.toPerTick(value);
            if (perTick > Integer.MAX_VALUE) throw new NumberFormatException();
            ModNetworking.sendOutputRate(this.container.syncId, false, side, perTick);
        } catch (NumberFormatException exception) {
            field.setText("");
            showInvalidItemRate();
        }
    }

    private void showInvalidItemRate() {
        this.minecraft.player.addChatMessage(new TranslatableComponent(
                "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_item")
                .applyFormat(ChatFormat.RED), true);
    }

    private void playButtonSound() {
        this.minecraft.getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public int getContainerId() {
        return this.container.syncId;
    }

    public void onOutputRateResult(int side, boolean success, long amount, RateRejectReason reason) {
        this.refreshCooldown = 10;
        if (side >= 0 && side < this.sideRateFields.length) {
            TextFieldWidget field = this.sideRateFields[side];
            if (field != this.editingField) {
                field.setText(success ? formatUnitRate(amount) : "");
            }
        } else {
            if (this.outputRateField != this.editingField) {
                this.outputRateField.setText(success ? formatUnitRate(amount) : "");
            }
            if (success) {
                for (TextFieldWidget field : this.sideRateFields) {
                    if (field != this.editingField) {
                        field.setText(formatUnitRate(amount));
                    }
                }
            }
        }
        if (success) {
            this.minecraft.player.addChatMessage(new TranslatableComponent(
                    "gui." + InfinityWhat.MOD_ID + ".io.rate.saved")
                    .applyFormat(ChatFormat.GREEN), true);
        } else {
            this.minecraft.player.addChatMessage(itemRateRejectMessage(reason)
                    .copy().applyFormat(ChatFormat.RED), true);
        }
    }

    private Component itemRateRejectMessage(RateRejectReason reason) {
        switch (reason) {
            case LIMIT_SIDE:
                return new TranslatableComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.limit_side",
                        formatUnitRate(InfinityWhatConfig.maxItemRatePerSide())
                                + this.rateUnit.suffix());
            case LIMIT_TOTAL:
                return new TranslatableComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.limit_total",
                        formatUnitRate(InfinityWhatConfig.maxItemTotalRate())
                                + this.rateUnit.suffix());
            case INVALID:
                return new TranslatableComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.invalid_item");
            default:
                return new TranslatableComponent(
                        "gui." + InfinityWhat.MOD_ID + ".io.rate.rejected");
        }
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
            this.editingField.method_1876(true);
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

    private void returnToMainPage() {
        setPage(GeneratorPage.MAIN, -1);
    }

    private void renderControlTooltip(int mouseX, int mouseY) {
        int x = mouseX - this.left;
        int y = mouseY - this.top;
        Component tooltip = null;
        if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_INFINITE_X, MAIN_INFINITE_Y,
                MAIN_INFINITE_WIDTH, MAIN_INFINITE_HEIGHT)) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".items.infinite_mode.tooltip");
        } else if (this.page == GeneratorPage.MAIN && isInside(x, y, MAIN_IO_X, MAIN_IO_Y,
                MAIN_IO_WIDTH, MAIN_IO_HEIGHT)) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".items.io.tooltip");
        } else if (this.page.isRedstone() && isInside(x, y, RS_MODE_X[0], RS_MODE_Y,
                RS_MODE_X[RS_MODE_X.length - 1] + RS_MODE_WIDTH - RS_MODE_X[0], RS_MODE_HEIGHT)) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".redstone.mode.tooltip");
        } else if (this.page.isRedstone()
                && (isInside(x, y, RS_PULSE_ON_FIELD_X, RS_PULSE_ON_LABEL_Y,
                        RS_PULSE_ON_FIELD_RIGHT - RS_PULSE_ON_FIELD_X, RS_PULSE_FIELD_HEIGHT)
                    || isInside(x, y, RS_PULSE_OFF_FIELD_X, RS_PULSE_OFF_LABEL_Y,
                        RS_PULSE_OFF_FIELD_RIGHT - RS_PULSE_OFF_FIELD_X, RS_PULSE_FIELD_HEIGHT))) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".redstone.pulse.tooltip");
        } else if (this.page.isBatch() && isInside(x, y, BATCH_FIELD_X, BATCH_LABEL_Y,
                BATCH_FIELD_RIGHT - BATCH_FIELD_X, RS_PULSE_FIELD_HEIGHT)) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".batch.tooltip");
        } else if (isIoPage() && isInside(x, y, IO_OUTPUT_X, IO_OUTPUT_Y,
                IO_OUTPUT_WIDTH, IO_OUTPUT_HEIGHT)) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".items.io.output.tooltip");
        } else if (isIoPage() && isFaceDetailPage() && isInsideRateLabel(x, y, true)) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.unit.tooltip",
                    this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInsideRateLabel(x, y, false)) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.unit.tooltip",
                    this.rateUnit.suffix());
        } else if (isIoPage() && !isFaceDetailPage() && isInside(x, y, IO_RATE_FIELD_X,
                IO_RATE_FIELD_Y, IO_RATE_FIELD_RIGHT - IO_RATE_FIELD_X, IO_RATE_FIELD_HEIGHT)) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.rate.item.tooltip");
        } else if (isIoPage() && !isFaceDetailPage()
                && isInside(x, y, IO_MODE_X, IO_MODE_Y, IO_MODE_WIDTH, IO_MODE_HEIGHT)) {
            tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.mode.tooltip");
        } else if (isIoPage() && !isFaceDetailPage() && this.advancedMode) {
            Direction direction = getHoveredFace(x, y);
            if (direction != null) {
                tooltip = new TranslatableComponent("gui." + InfinityWhat.MOD_ID + ".io.neighbor.tooltip",
                        getNeighborName(direction), getNeighborId(direction));
            }
        }
        if (tooltip != null) this.renderTooltip(tooltip.getString(), mouseX, mouseY);
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
        return isInside(mouseX - this.left, mouseY - this.top, x, y, width, height);
    }

    private static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
