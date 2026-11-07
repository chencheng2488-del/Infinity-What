package com.bandybus.infwhat.client.render;

import com.bandybus.infwhat.api.IRangePreviewProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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
        if (minecraft.world == null || minecraft.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (minecraft.world != cachedWorld || now >= nextRefresh) {
            rebuild(minecraft.world);
            cachedWorld = minecraft.world;
            nextRefresh = now + REFRESH_INTERVAL_MS;
        }
        if (BOXES.isEmpty()) {
            return;
        }

        double cameraX = minecraft.getRenderManager().viewerPosX;
        double cameraY = minecraft.getRenderManager().viewerPosY;
        double cameraZ = minecraft.getRenderManager().viewerPosZ;

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

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Translucent shell: the six boundary faces only, the interior stays empty.
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        for (int[] box : visible) {
            double minX = box[0] - box[3] - cameraX;
            double minY = box[1] - box[3] - cameraY;
            double minZ = box[2] - box[3] - cameraZ;
            double maxX = box[0] + box[3] + 1 - cameraX;
            double maxY = box[1] + box[3] + 1 - cameraY;
            double maxZ = box[2] + box[3] + 1 - cameraZ;
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            addFace(buffer, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
            addFace(buffer, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
            addFace(buffer, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
            addFace(buffer, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
            addFace(buffer, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
            addFace(buffer, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
            tessellator.draw();
        }
        GlStateManager.depthMask(true);

        // Bright outline drawn through blocks so the exact bounds stay readable.
        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(3.0F);
        for (int[] box : visible) {
            double minX = box[0] - box[3] - cameraX;
            double minY = box[1] - box[3] - cameraY;
            double minZ = box[2] - box[3] - cameraZ;
            double maxX = box[0] + box[3] + 1 - cameraX;
            double maxY = box[1] + box[3] + 1 - cameraY;
            double maxZ = box[2] + box[3] + 1 - cameraZ;
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            addLine(buffer, minX, minY, minZ, maxX, minY, minZ);
            addLine(buffer, maxX, minY, minZ, maxX, minY, maxZ);
            addLine(buffer, maxX, minY, maxZ, minX, minY, maxZ);
            addLine(buffer, minX, minY, maxZ, minX, minY, minZ);
            addLine(buffer, minX, maxY, minZ, maxX, maxY, minZ);
            addLine(buffer, maxX, maxY, minZ, maxX, maxY, maxZ);
            addLine(buffer, maxX, maxY, maxZ, minX, maxY, maxZ);
            addLine(buffer, minX, maxY, maxZ, minX, maxY, minZ);
            addLine(buffer, minX, minY, minZ, minX, maxY, minZ);
            addLine(buffer, maxX, minY, minZ, maxX, maxY, minZ);
            addLine(buffer, maxX, minY, maxZ, maxX, maxY, maxZ);
            addLine(buffer, minX, minY, maxZ, minX, maxY, maxZ);
            tessellator.draw();
        }

        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
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
            BOXES.add(new int[]{tile.getPos().getX(), tile.getPos().getY(),
                    tile.getPos().getZ(), Math.max(1, provider.getRangePreviewRadius())});
        }
    }

    private static void addFace(BufferBuilder buffer,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double x3, double y3, double z3,
                                double x4, double y4, double z4) {
        buffer.pos(x1, y1, z1).color(0.42F, 0.85F, 0.78F, 0.16F).endVertex();
        buffer.pos(x2, y2, z2).color(0.42F, 0.85F, 0.78F, 0.16F).endVertex();
        buffer.pos(x3, y3, z3).color(0.42F, 0.85F, 0.78F, 0.16F).endVertex();
        buffer.pos(x4, y4, z4).color(0.42F, 0.85F, 0.78F, 0.16F).endVertex();
    }

    private static void addLine(BufferBuilder buffer,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2) {
        buffer.pos(x1, y1, z1).color(0.42F, 0.85F, 0.78F, 0.95F).endVertex();
        buffer.pos(x2, y2, z2).color(0.42F, 0.85F, 0.78F, 0.95F).endVertex();
    }
}
