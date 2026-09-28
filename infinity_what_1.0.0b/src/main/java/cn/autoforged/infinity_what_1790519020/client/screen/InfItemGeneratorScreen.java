package cn.autoforged.infinity_what_1790519020.client.screen;

import cn.autoforged.infinity_what_1790519020.InfinityWhat;
import cn.autoforged.infinity_what_1790519020.menu.InfItemGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the Infinity Item Generator UI: the single generator slot plus the green LED indicator
 * under it (bright when the block is redstone powered, dark otherwise) with a hover tooltip.
 */
public class InfItemGeneratorScreen extends AbstractContainerScreen<InfItemGeneratorMenu> {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(InfinityWhat.MOD_ID, "textures/gui/container/inf_item_generator.png");

    // The background PNG is 176x166 (derived from the 176x166 region of vanilla dispenser.png),
    // NOT the usual 256x256 GUI sheet. The 7-arg GuiGraphics#blit assumes a 256x256 texture and
    // normalizes UVs by 256, so it would sample only the top-left 176/256 x 166/256 of the image
    // and stretch that to the full GUI (~1.45x "over-magnified" background). Passing the real
    // texture size makes the blit 1:1.
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;

    // LED geometry inside the GUI, centred horizontally under the generator slot
    // (slot spans x = SLOT_X .. SLOT_X + 16, so an 8px LED starts at SLOT_X + 4).
    private static final int LED_X = InfItemGeneratorMenu.SLOT_X + 4;
    private static final int LED_Y = 57;
    private static final int LED_SIZE = 8;

    private static final int COLOR_LED_FRAME = 0xFF303030;
    private static final int COLOR_LED_OFF = 0xFF0E3B0E;
    private static final int COLOR_LED_OFF_FRAME = 0xFF1E5A1E;
    private static final int COLOR_LED_ON = 0xFF33FF33;
    private static final int COLOR_LED_ON_HIGHLIGHT = 0xFFB8FFB8;

    public InfItemGeneratorScreen(InfItemGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        renderLed(graphics);
    }

    private void renderLed(GuiGraphics graphics) {
        boolean on = this.menu.isPowered();
        int x = this.leftPos + LED_X;
        int y = this.topPos + LED_Y;
        graphics.fill(x - 1, y - 1, x + LED_SIZE + 1, y + LED_SIZE + 1, COLOR_LED_FRAME);
        if (on) {
            graphics.fill(x, y, x + LED_SIZE, y + LED_SIZE, COLOR_LED_ON);
            graphics.fill(x + 1, y + 1, x + LED_SIZE - 1, y + 1, COLOR_LED_ON_HIGHLIGHT);
        } else {
            graphics.fill(x, y, x + LED_SIZE, y + LED_SIZE, COLOR_LED_OFF);
            graphics.fill(x, y, x + LED_SIZE, y + 1, COLOR_LED_OFF_FRAME);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderStoredInfo(graphics);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (isOverLed(mouseX, mouseY)) {
            graphics.renderTooltip(this.font,
                    Component.translatable("gui." + InfinityWhat.MOD_ID
                            + (this.menu.isPowered() ? ".items.led.on" : ".items.led.off")),
                    mouseX, mouseY);
        }
    }

    /**
     * Shows the stored item's name and registry id on the two text rows directly above the slot.
     * Both lines are centred over the slot and trimmed with an ellipsis so they can never cross
     * the GUI border. The old infinity badge was removed because it overlapped the vanilla stack
     * count overlay.
     */
    private void renderStoredInfo(GuiGraphics graphics) {
        ItemStack stored = this.menu.getStoredItem();
        if (stored.isEmpty()) {
            return;
        }
        int maxWidth = this.imageWidth - 16;
        String name = truncate(stored.getHoverName().getString(), maxWidth);
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stored.getItem());
        String id = truncate(key.toString(), maxWidth);

        graphics.drawString(this.font, name,
                this.leftPos + (this.imageWidth - this.font.width(name)) / 2,
                this.topPos + 15, 0x404040, false);
        graphics.drawString(this.font, id,
                this.leftPos + (this.imageWidth - this.font.width(id)) / 2,
                this.topPos + 25, 0x707070, false);
    }

    private String truncate(String value, int maxWidth) {
        if (this.font.width(value) > maxWidth) {
            return this.font.plainSubstrByWidth(value, maxWidth - this.font.width("...")) + "...";
        }
        return value;
    }

    private boolean isOverLed(double mouseX, double mouseY) {
        int x = this.leftPos + LED_X;
        int y = this.topPos + LED_Y;
        return mouseX >= x && mouseX < x + LED_SIZE && mouseY >= y && mouseY < y + LED_SIZE;
    }
}
