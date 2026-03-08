package com.zzhalex233.twilightforestaddons.client.renderer;

import com.zzhalex233.twilightforestaddons.item.ItemAdvancedMagicMap;
import com.zzhalex233.twilightforestaddons.map.AdvancedMagicMapDataUtils;
import com.zzhalex233.twilightforestaddons.map.BossFeatureRegistry;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import twilightforest.TFFeature;
import twilightforest.TFMagicMapData;

public class AdvancedMagicMapItemRenderer {
    private static final ResourceLocation TWILIGHT_MAP_ICONS = new ResourceLocation("twilightforest", "textures/gui/mapicons.png");
    private static final ResourceLocation VANILLA_MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");
    private static final int MAP_SIZE = 128;
    private static final float MAP_CENTER = 64.0F;
    private static final float MAP_MIN = -2.0F;
    private static final float MAP_MAX = 130.0F;
    private static final int DECORATION_VISIBILITY_RADIUS = 1;
    private static final Field MAP_DECORATIONS_FIELD = resolveMapDecorationsField();

    public TFMagicMapData getClientMapData(int mapId, World world) {
        if (world == null) {
            return null;
        }

        String mapName = ItemAdvancedMagicMap.MAP_DATA_PREFIX + "_" + mapId;
        return (TFMagicMapData) world.loadData(TFMagicMapData.class, mapName);
    }

    public void renderGuiMap(EntityPlayer player, TextureManager textureManager, TFMagicMapData mapData, float left, float top, float size) {
        if (player == null || textureManager == null || mapData == null) {
            return;
        }

        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.tfDecorations);

        Minecraft mc = Minecraft.getMinecraft();
        MapItemRenderer mapRenderer = mc.entityRenderer == null ? null : mc.entityRenderer.getMapItemRenderer();
        if (mapRenderer == null) {
            return;
        }

        float scale = size / MAP_SIZE;
        float offsetX = getDisplayOffsetX(player, mapData);
        float offsetZ = getDisplayOffsetZ(player, mapData);
        Map<String, MapDecoration> originalDecorations = detachVanillaDecorations(mapData);

        try {
            GlStateManager.pushMatrix();
            GlStateManager.translate(left, top, 0.0F);
            GlStateManager.scale(scale, scale, 1.0F);

            GlStateManager.pushMatrix();
            GlStateManager.translate(-offsetX, -offsetZ, 0.0F);
            mapRenderer.renderMap(mapData, false);
            GlStateManager.popMatrix();

            renderTfDecorations(textureManager, mapData, offsetX, offsetZ);
            renderCenteredPlayerIcon(textureManager, player);
            GlStateManager.popMatrix();
        } finally {
            restoreVanillaDecorations(mapData, originalDecorations);
        }
    }

    public HoveredFeature findHoveredFeature(EntityPlayer player, TFMagicMapData mapData, int mouseX, int mouseY, float left, float top, float size) {
        if (player == null || mapData == null) {
            return null;
        }

        float scale = size / MAP_SIZE;
        float iconHalfSize = 4.0F * scale;
        float offsetX = getDisplayOffsetX(player, mapData);
        float offsetZ = getDisplayOffsetZ(player, mapData);

        for (TFMagicMapData.TFMapDecoration decoration : mapData.tfDecorations) {
            if (!shouldRenderBossHover(mapData, decoration)) {
                continue;
            }

            float drawX = getDecorationMapX(decoration) - offsetX;
            float drawY = getDecorationMapY(decoration) - offsetZ;
            if (!isWithinVisibleMap(drawX, drawY)) {
                continue;
            }

            float iconCenterX = left + drawX * scale;
            float iconCenterY = top + drawY * scale;
            if (Math.abs(mouseX - iconCenterX) <= iconHalfSize && Math.abs(mouseY - iconCenterY) <= iconHalfSize) {
                return new HoveredFeature(AdvancedMagicMapDataUtils.getFeatureId(decoration), decoration.getX(), decoration.getY());
            }
        }

        return null;
    }

    public int mapPixelToWorldX(EntityPlayer player, TFMagicMapData mapData, float localPixelX) {
        return MathHelper.floor(mapData.xCenter + ((localPixelX + getDisplayOffsetX(player, mapData)) - MAP_CENTER) * getBlocksPerPixel(mapData));
    }

    public int mapPixelToWorldZ(EntityPlayer player, TFMagicMapData mapData, float localPixelZ) {
        return MathHelper.floor(mapData.zCenter + ((localPixelZ + getDisplayOffsetZ(player, mapData)) - MAP_CENTER) * getBlocksPerPixel(mapData));
    }

    private void renderTfDecorations(TextureManager textureManager, TFMagicMapData mapData, float offsetX, float offsetZ) {
        textureManager.bindTexture(TWILIGHT_MAP_ICONS);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int layer = 0;
        for (TFMagicMapData.TFMapDecoration decoration : mapData.tfDecorations) {
            if (!shouldRenderDecoration(mapData, decoration)) {
                continue;
            }

            int featureId = AdvancedMagicMapDataUtils.getFeatureId(decoration);
            TFFeature feature = TFFeature.getFeatureByID(featureId);
            if (feature == null || !feature.isStructureEnabled) {
                continue;
            }

            float drawX = getDecorationMapX(decoration) - offsetX;
            float drawY = getDecorationMapY(decoration) - offsetZ;
            if (!isWithinVisibleMap(drawX, drawY)) {
                continue;
            }

            drawFeatureIcon(featureId, drawX, drawY, decoration.getRotation(), layer++);
        }
    }

    private void drawFeatureIcon(int featureId, float drawX, float drawY, byte rotation, int layer) {
        float u0 = (featureId % 8) / 8.0F;
        float v0 = (featureId / 8) / 8.0F;
        float u1 = ((featureId % 8) + 1) / 8.0F;
        float v1 = ((featureId / 8) + 1) / 8.0F;
        double depth = layer * -0.001D;

        GlStateManager.pushMatrix();
        GlStateManager.translate(drawX, drawY, -0.02F);
        GlStateManager.rotate(rotation * 360.0F / 16.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(4.0F, 4.0F, 3.0F);
        GlStateManager.translate(-0.125F, 0.125F, 0.0F);
        drawTexturedQuad(u0, v0, u1, v1, depth);
        GlStateManager.popMatrix();
    }

    private void renderCenteredPlayerIcon(TextureManager textureManager, EntityPlayer player) {
        textureManager.bindTexture(VANILLA_MAP_ICONS);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.pushMatrix();
        GlStateManager.translate(MAP_CENTER, MAP_CENTER, -0.03F);
        GlStateManager.rotate(player.rotationYaw, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(4.0F, 4.0F, 3.0F);
        GlStateManager.translate(-0.125F, 0.125F, 0.0F);
        drawTexturedQuad(0.0D, 0.0D, 0.25D, 0.25D, 0.0D);
        GlStateManager.popMatrix();
    }

    private void drawTexturedQuad(double u0, double v0, double u1, double v1, double depth) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-1.0D, 1.0D, depth).tex(u0, v0).endVertex();
        buffer.pos(1.0D, 1.0D, depth).tex(u1, v0).endVertex();
        buffer.pos(1.0D, -1.0D, depth).tex(u1, v1).endVertex();
        buffer.pos(-1.0D, -1.0D, depth).tex(u0, v1).endVertex();
        tessellator.draw();
    }

    @SuppressWarnings("unchecked")
    private Map<String, MapDecoration> detachVanillaDecorations(MapData mapData) {
        try {
            Map<String, MapDecoration> decorations = (Map<String, MapDecoration>) MAP_DECORATIONS_FIELD.get(mapData);
            Map<String, MapDecoration> copy = new LinkedHashMap<>(decorations);
            decorations.clear();
            return copy;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access vanilla map decorations.", e);
        }
    }

    private void restoreVanillaDecorations(MapData mapData, Map<String, MapDecoration> originalDecorations) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, MapDecoration> decorations = (Map<String, MapDecoration>) MAP_DECORATIONS_FIELD.get(mapData);
            decorations.clear();
            decorations.putAll(originalDecorations);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to restore vanilla map decorations.", e);
        }
    }

    private boolean shouldRenderDecoration(TFMagicMapData mapData, TFMagicMapData.TFMapDecoration decoration) {
        return decoration != null && hasExploredPixelNear(mapData, getDecorationMapX(decoration), getDecorationMapY(decoration));
    }

    private boolean shouldRenderBossHover(TFMagicMapData mapData, TFMagicMapData.TFMapDecoration decoration) {
        if (!shouldRenderDecoration(mapData, decoration)) {
            return false;
        }

        int featureId = AdvancedMagicMapDataUtils.getFeatureId(decoration);
        return BossFeatureRegistry.isBossFeature(featureId);
    }

    private boolean isWithinVisibleMap(float drawX, float drawY) {
        return drawX >= MAP_MIN && drawX <= MAP_MAX && drawY >= MAP_MIN && drawY <= MAP_MAX;
    }

    private boolean hasExploredPixelNear(TFMagicMapData mapData, float mapPixelX, float mapPixelY) {
        int centerX = MathHelper.clamp(Math.round(mapPixelX), 0, MAP_SIZE - 1);
        int centerY = MathHelper.clamp(Math.round(mapPixelY), 0, MAP_SIZE - 1);
        for (int dx = -DECORATION_VISIBILITY_RADIUS; dx <= DECORATION_VISIBILITY_RADIUS; dx++) {
            for (int dy = -DECORATION_VISIBILITY_RADIUS; dy <= DECORATION_VISIBILITY_RADIUS; dy++) {
                int x = MathHelper.clamp(centerX + dx, 0, MAP_SIZE - 1);
                int y = MathHelper.clamp(centerY + dy, 0, MAP_SIZE - 1);
                if (mapData.colors[x + y * MAP_SIZE] != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private float getDecorationMapX(TFMagicMapData.TFMapDecoration decoration) {
        return decoration.getX() / 2.0F + MAP_CENTER;
    }

    private float getDecorationMapY(TFMagicMapData.TFMapDecoration decoration) {
        return decoration.getY() / 2.0F + MAP_CENTER;
    }

    private float getDisplayOffsetX(EntityPlayer player, TFMagicMapData mapData) {
        return (float) ((player.posX - mapData.xCenter) / getBlocksPerPixel(mapData));
    }

    private float getDisplayOffsetZ(EntityPlayer player, TFMagicMapData mapData) {
        return (float) ((player.posZ - mapData.zCenter) / getBlocksPerPixel(mapData));
    }

    private int getBlocksPerPixel(TFMagicMapData mapData) {
        return 1 << mapData.scale;
    }

    private static Field resolveMapDecorationsField() {
        for (String candidateName : new String[] {"mapDecorations", "field_76203_h"}) {
            try {
                Field field = MapData.class.getDeclaredField(candidateName);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        throw new IllegalStateException("Unable to access vanilla map decorations field.");
    }

    public static final class HoveredFeature {
        public final int featureId;
        public final byte mapCoordX;
        public final byte mapCoordZ;

        private HoveredFeature(int featureId, byte mapCoordX, byte mapCoordZ) {
            this.featureId = featureId;
            this.mapCoordX = mapCoordX;
            this.mapCoordZ = mapCoordZ;
        }
    }
}