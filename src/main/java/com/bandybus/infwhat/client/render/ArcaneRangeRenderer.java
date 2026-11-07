package com.bandybus.infwhat.client.render;

import com.bandybus.infwhat.api.IRangePreviewProvider;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a WorldEdit-style box around every arcane machine whose range preview is switched on.
 *
 * <p>The box is a translucent shell plus a bright outline; it stays visible in the world until the
 * machine's preview toggle is switched off. Machine positions are rescanned twice per second.
 */
@SideOnly(Side.CLIENT)
public final class ArcaneRangeRenderer {

    private static final long REFRESH_INTERVAL_MS = 500L;
    private static final double MAX_DISTANCE_SQUARED = 128.0D * 128.0D;

    private static final List<int[]> BOXES = new ArrayList<int[]>();
    private static World cachedWorld;
    private static long nextRefresh;

    public ArcaneRangeRenderer() {
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || minecraft.thePlayer == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (minecraft.theWorld != cachedWorld || now >= nextRefresh) {
            rebuild(minecraft.theWorld);
            cachedWorld = minecraft.theWorld;
            nextRefresh = now + REFRESH_INTERVAL_MS;
        }
        if (BOXES.isEmpty()) {
            return;
        }

        double cameraX = RenderManager.instance.viewerPosX;
        double cameraY = RenderManager.instance.viewerPosY;
        double cameraZ = RenderManager.instance.viewerPosZ;

        List<int[]> visible = new ArrayList<int[]>(BOXES.size());
        for (int[] box : BOXES) {
            double dx = box[0] + 0.5D - cameraX;
            double dy = box[1] + 0.5D - cameraY;
            double dz = box[2] + 0.5D - cameraZ;
            if (dx * dx + dy * dy + dz * dz <= MAX_DISTANCE_SQUARED) {
                visible.add(box);
            }
        }
        if (visible.isEmpty()) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        Tessellator tessellator = Tessellator.instance;

        // Translucent shell: the six boundary faces only, the interior stays empty.
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        for (int[] box : visible) {
            double minX = box[0] - box[3] - cameraX;
            double minY = box[1] - box[3] - cameraY;
            double minZ = box[2] - box[3] - cameraZ;
            double maxX = box[0] + box[3] + 1 - cameraX;
            double maxY = box[1] + box[3] + 1 - cameraY;
            double maxZ = box[2] + box[3] + 1 - cameraZ;
            tessellator.startDrawingQuads();
            tessellator.setColorRGBA_F(0.42F, 0.85F, 0.78F, 0.16F);
            addFace(tessellator, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ,
                    minX, minY, maxZ);
            addFace(tessellator, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ,
                    maxX, maxY, minZ);
            addFace(tessellator, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                    maxX, minY, minZ);
            addFace(tessellator, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ,
                    minX, maxY, maxZ);
            addFace(tessellator, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ,
                    minX, maxY, minZ);
            addFace(tessellator, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                    maxX, minY, maxZ);
            tessellator.draw();
        }
        GL11.glDepthMask(true);

        // Bright outline drawn through blocks so the exact bounds stay readable.
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(3.0F);
        for (int[] box : visible) {
            double minX = box[0] - box[3] - cameraX;
            double minY = box[1] - box[3] - cameraY;
            double minZ = box[2] - box[3] - cameraZ;
            double maxX = box[0] + box[3] + 1 - cameraX;
            double maxY = box[1] + box[3] + 1 - cameraY;
            double maxZ = box[2] + box[3] + 1 - cameraZ;
            tessellator.startDrawing(GL11.GL_LINES);
            tessellator.setColorRGBA_F(0.42F, 0.85F, 0.78F, 0.95F);
            addLine(tessellator, minX, minY, minZ, maxX, minY, minZ);
            addLine(tessellator, maxX, minY, minZ, maxX, minY, maxZ);
            addLine(tessellator, maxX, minY, maxZ, minX, minY, maxZ);
            addLine(tessellator, minX, minY, maxZ, minX, minY, minZ);
            addLine(tessellator, minX, maxY, minZ, maxX, maxY, minZ);
            addLine(tessellator, maxX, maxY, minZ, maxX, maxY, maxZ);
            addLine(tessellator, maxX, maxY, maxZ, minX, maxY, maxZ);
            addLine(tessellator, minX, maxY, maxZ, minX, maxY, minZ);
            addLine(tessellator, minX, minY, minZ, minX, maxY, minZ);
            addLine(tessellator, maxX, minY, minZ, maxX, maxY, minZ);
            addLine(tessellator, maxX, minY, maxZ, maxX, maxY, maxZ);
            addLine(tessellator, minX, minY, maxZ, minX, maxY, maxZ);
            tessellator.draw();
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    /** Rebuilds the list of machines whose persistent range preview is switched on. */
    private static void rebuild(World world) {
        BOXES.clear();
        for (Object object : world.loadedTileEntityList) {
            if (!(object instanceof IRangePreviewProvider)
                    || !(object instanceof TileEntity)) {
                continue;
            }
            IRangePreviewProvider provider = (IRangePreviewProvider) object;
            if (!provider.isRangePreviewEnabled()) {
                continue;
            }
            TileEntity tile = (TileEntity) object;
            BOXES.add(new int[]{tile.xCoord, tile.yCoord, tile.zCoord,
                    Math.max(1, provider.getRangePreviewRadius())});
        }
    }

    private static void addFace(Tessellator tessellator,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double x3, double y3, double z3,
                                double x4, double y4, double z4) {
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
        tessellator.addVertex(x3, y3, z3);
        tessellator.addVertex(x4, y4, z4);
    }

    private static void addLine(Tessellator tessellator,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2) {
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
    }
}
