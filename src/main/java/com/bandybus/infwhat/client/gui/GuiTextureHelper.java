package com.bandybus.infwhat.client.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

/**
 * Small drawing helpers that 1.6.4 does not provide.
 *
 * <p>1.6.4's {@code Gui.drawTexturedModalRect} assumes a 256x256 texture atlas, so the custom
 * sized GUI sheets are drawn with explicit texture dimensions here. 1.6.4 also has no
 * {@code drawHoveringText}, so a minimal dark tooltip box is drawn directly.
 */
final class GuiTextureHelper {

    private GuiTextureHelper() {
    }

    /** Draws a texture region with explicit texture dimensions instead of the fixed 256 scale. */
    static void drawTexturedRect(int x, int y, int u, int v, int width, int height,
                                 int textureWidth, int textureHeight) {
        float scaleU = 1.0F / (float) textureWidth;
        float scaleV = 1.0F / (float) textureHeight;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0.0D, (u) * scaleU,
                (v + height) * scaleV);
        tessellator.addVertexWithUV(x + width, y + height, 0.0D, (u + width) * scaleU,
                (v + height) * scaleV);
        tessellator.addVertexWithUV(x + width, y, 0.0D, (u + width) * scaleU, (v) * scaleV);
        tessellator.addVertexWithUV(x, y, 0.0D, (u) * scaleU, (v) * scaleV);
        tessellator.draw();
    }

    /**
     * Draws a single line tooltip near the cursor, kept inside the screen bounds.
     *
     * @param width  screen width used to clamp the box
     * @param height screen height used to clamp the box
     */
    static void drawTooltip(FontRenderer font, int width, int height, String text,
                            int mouseX, int mouseY) {
        int textWidth = font.getStringWidth(text);
        int boxWidth = textWidth + 8;
        int boxHeight = 12;
        int x = mouseX + 10;
        int y = mouseY - 12;
        if (x + boxWidth > width) {
            x = width - boxWidth - 2;
        }
        if (y + boxHeight > height) {
            y = height - boxHeight - 2;
        }
        if (y < 2) {
            y = 2;
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(0.06F, 0.04F, 0.09F, 0.95F);
        Gui.drawRect(x, y, x + boxWidth, y + boxHeight, 0xF0100010);
        GL11.glColor4f(0.42F, 0.85F, 0.78F, 1.0F);
        Gui.drawRect(x, y, x + boxWidth, y + 1, 0xFF6BD9C7);
        Gui.drawRect(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, 0xFF6BD9C7);
        Gui.drawRect(x, y, x + 1, y + boxHeight, 0xFF6BD9C7);
        Gui.drawRect(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, 0xFF6BD9C7);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        font.drawStringWithShadow(text, x + 4, y + 2, 0xFFFFFFFF);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
