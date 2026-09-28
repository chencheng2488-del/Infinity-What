package cn.autoforged.infinity_what_1790519020.client.screen;

import cn.autoforged.infinity_what_1790519020.InfinityWhat;
import cn.autoforged.infinity_what_1790519020.menu.InfFluidGeneratorMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Renders the fluid tank: the native still texture of the stored fluid, tinted with its own
 * tint colour and clipped to the fill height, plus the fluid name / id / amount text.
 */
public class InfFluidGeneratorScreen extends AbstractContainerScreen<InfFluidGeneratorMenu> {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(InfinityWhat.MOD_ID, "textures/gui/container/inf_fluid_generator.png");

    private static final int TEXT_X = 44;
    /**
     * The tank occupies x25..42 and the GUI is 176 wide, so the text column can start at 44 and
     * run up to the right border (132 px). {@code Long.MAX_VALUE + " mB"} needs ~130 px, which
     * lets the infinite amount be shown in the same mB unit the Forge fluid capability and the
     * AE2 storage bus use, without the line being trimmed.
     */
    private static final int TEXT_MAX_WIDTH = 132;
    private static final int COLOR_TITLE = 0x404040;
    private static final int COLOR_TEXT = 0x555555;
    private static final int COLOR_MUTED = 0x808080;

    public InfFluidGeneratorScreen(InfFluidGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        renderFluid(graphics);
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

        // Tile the fluid's native texture at its own resolution instead of stretching one
        // 16x16 sprite over the whole tank height. The scissor keeps the topmost partial
        // tile clipped at the fill line.
        int tile = Math.max(1, sprite.contents().height());
        graphics.enableScissor(x, yTop, x + InfFluidGeneratorMenu.TANK_WIDTH, yBottom);
        for (int drawn = 0; drawn < fill; drawn += tile) {
            int tileY = yBottom - drawn - tile;
            graphics.blit(x, tileY, 0, InfFluidGeneratorMenu.TANK_WIDTH, tile, sprite, red, green, blue, alpha);
        }
        graphics.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        FluidStack stack = this.menu.getFluidStack();
        if (stack.isEmpty()) {
            graphics.drawString(this.font,
                    Component.translatable("gui." + InfinityWhat.MOD_ID + ".empty"),
                    TEXT_X, 22, COLOR_MUTED, false);
            return;
        }

        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        String id = key == null ? "unknown" : key.toString();

        // The infinite source is shown in mB, the unit the Forge fluid capability (IFluidHandler)
        // and therefore the AE2 storage bus use. It used to say "B", which made the GUI claim a
        // value 1000x larger than what a connected AE2 network reports for the same block.
        String amount = this.menu.isInfinite()
                ? Long.MAX_VALUE + " mB"
                : this.menu.getAmount() + " / "
                        + cn.autoforged.infinity_what_1790519020.config.InfinityWhatConfig.finiteCapacity() + " mB";

        drawTruncated(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid_name", stack.getDisplayName()),
                TEXT_X, 20, COLOR_TITLE);
        drawTruncated(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".fluid_id", id),
                TEXT_X, 32, COLOR_TEXT);
        drawTruncated(graphics,
                Component.translatable("gui." + InfinityWhat.MOD_ID + ".amount", amount),
                TEXT_X, 44, COLOR_TEXT);
    }

    /** Draws a single line, trimming it with an ellipsis so it can never cross the border. */
    private void drawTruncated(GuiGraphics graphics, Component text, int x, int y, int color) {
        String value = text.getString();
        if (this.font.width(value) > TEXT_MAX_WIDTH) {
            value = this.font.plainSubstrByWidth(value, TEXT_MAX_WIDTH - this.font.width("...")) + "...";
        }
        graphics.drawString(this.font, value, x, y, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // The whole tank is the fluid slot target, not just the 16x16 top of it, so forward
        // any left/right click inside the tank rect to the menu as a PICKUP on the tank slot.
        if ((button == 0 || button == 1) && isOverTank(mouseX, mouseY)) {
            Slot slot = this.menu.slots.get(InfFluidGeneratorMenu.FLUID_SLOT_INDEX);
            this.slotClicked(slot, InfFluidGeneratorMenu.FLUID_SLOT_INDEX, button, ClickType.PICKUP);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverTank(double mouseX, double mouseY) {
        int x = this.leftPos + InfFluidGeneratorMenu.TANK_X;
        int y = this.topPos + InfFluidGeneratorMenu.TANK_Y;
        return mouseX >= x && mouseX < x + InfFluidGeneratorMenu.TANK_WIDTH
                && mouseY >= y && mouseY < y + InfFluidGeneratorMenu.TANK_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
