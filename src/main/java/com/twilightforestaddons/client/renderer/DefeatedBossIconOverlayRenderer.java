package com.twilightforestaddons.client.renderer;

import java.util.List;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData.MapCoord;

import org.lwjgl.opengl.GL11;

import com.twilightforestaddons.map.BossFeatureRegistry;

public final class DefeatedBossIconOverlayRenderer {

    private static final ResourceLocation TWILIGHT_MAP_ICONS = new ResourceLocation("twilightforest:textures/gui/mapicons.png");

    private DefeatedBossIconOverlayRenderer() {}

    public static void renderDefeatedBossOverlays(EntityPlayer player, TextureManager textureManager, List<MapCoord> features) {
        if (player == null || textureManager == null || features == null || features.isEmpty()) {
            return;
        }

        textureManager.bindTexture(TWILIGHT_MAP_ICONS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tessellator tessellator = Tessellator.instance;
        for (MapCoord coord : features) {
            if (!BossFeatureRegistry.isBossFeature(coord.iconSize)
                || !BossFeatureRegistry.isBossDefeated(coord.iconSize, player)) {
                continue;
            }

            GL11.glPushMatrix();
            GL11.glTranslatef(coord.centerX / 2.0F + 64.0F, coord.centerZ / 2.0F + 64.0F, 0.01F);
            GL11.glRotatef(coord.iconRotation * 360.0F / 16.0F, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(4.0F, 4.0F, 3.0F);
            GL11.glTranslatef(-0.125F, 0.125F, 0.0F);
            GL11.glColor4f(0.35F, 0.35F, 0.35F, 1.0F);

            float u0 = (coord.iconSize % 8) / 8.0F;
            float v0 = (coord.iconSize / 8) / 8.0F;
            float u1 = ((coord.iconSize % 8) + 1) / 8.0F;
            float v1 = ((coord.iconSize / 8) + 1) / 8.0F;

            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(-1.0D, 1.0D, 0.0D, u0, v0);
            tessellator.addVertexWithUV(1.0D, 1.0D, 0.0D, u1, v0);
            tessellator.addVertexWithUV(1.0D, -1.0D, 0.0D, u1, v1);
            tessellator.addVertexWithUV(-1.0D, -1.0D, 0.0D, u0, v1);
            tessellator.draw();

            GL11.glPopMatrix();
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
