package com.twilightforestaddons.client.renderer;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData.MapCoord;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.lwjgl.opengl.GL11;

import com.twilightforestaddons.item.ModItems;
import com.twilightforestaddons.map.AdvancedMagicMapDataUtils;
import com.twilightforestaddons.map.BossFeatureRegistry;

import twilightforest.TFMagicMapData;
import twilightforest.client.renderer.TFMagicMapRenderer;
import twilightforest.item.ItemTFMagicMap;

public class AdvancedMagicMapItemRenderer implements IItemRenderer {

    private TFMagicMapRenderer delegate;

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return item != null && item.getItem() == ModItems.advancedMagicMap && type == ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type != ItemRenderType.FIRST_PERSON_MAP) {
            return;
        }

        TFMagicMapRenderer renderer = this.getDelegate();
        if (renderer == null || data.length < 3) {
            return;
        }

        EntityPlayer player = data[0] instanceof EntityPlayer ? (EntityPlayer) data[0] : null;
        TextureManager textureManager = data[1] instanceof TextureManager ? (TextureManager) data[1] : null;
        TFMagicMapData mapData = data[2] instanceof TFMagicMapData ? (TFMagicMapData) data[2] : null;
        if (player == null || textureManager == null || mapData == null) {
            return;
        }

        renderer.renderItem(type, item, data);
        DefeatedBossIconOverlayRenderer.renderDefeatedBossOverlays(
            player,
            textureManager,
            AdvancedMagicMapDataUtils.createDedupedFeatureSnapshot(mapData));
    }

    public TFMagicMapData getClientMapData(int mapId, World world) {
        return world == null ? null : ItemTFMagicMap.getMPMapData(mapId, world);
    }

    public void renderGuiMap(EntityPlayer player, TextureManager textureManager, TFMagicMapData mapData, float left,
        float top, float size) {
        TFMagicMapRenderer renderer = this.getDelegate();
        if (renderer == null || player == null || textureManager == null || mapData == null) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(left, top, 0.0F);
        GL11.glScalef(size / 128.0F, size / 128.0F, 1.0F);
        renderer.renderMap(player, textureManager, mapData);
        DefeatedBossIconOverlayRenderer.renderDefeatedBossOverlays(
            player,
            textureManager,
            AdvancedMagicMapDataUtils.createDedupedFeatureSnapshot(mapData));
        GL11.glPopMatrix();
    }

    public HoveredFeature findHoveredFeature(TFMagicMapData mapData, int mouseX, int mouseY, float left, float top,
        float size) {
        if (mapData == null) {
            return null;
        }

        float scale = size / 128.0F;
        float iconHalfSize = 4.0F * scale;
        List<MapCoord> features = AdvancedMagicMapDataUtils.createDedupedFeatureSnapshot(mapData);

        for (MapCoord coord : features) {
            if (!BossFeatureRegistry.isBossFeature(coord.iconSize)) {
                continue;
            }

            float iconCenterX = left + (coord.centerX / 2.0F + 64.0F) * scale;
            float iconCenterY = top + (coord.centerZ / 2.0F + 64.0F) * scale;
            if (Math.abs(mouseX - iconCenterX) <= iconHalfSize && Math.abs(mouseY - iconCenterY) <= iconHalfSize) {
                return new HoveredFeature(
                    coord.iconSize,
                    coord.centerX,
                    coord.centerZ);
            }
        }

        return null;
    }

    public int mapPixelToWorldX(TFMagicMapData mapData, float localPixelX) {
        return MathHelper.floor_double(
            mapData.xCenter + (localPixelX - 64.0F) * this.getBlocksPerPixel(mapData));
    }

    public int mapPixelToWorldZ(TFMagicMapData mapData, float localPixelZ) {
        return MathHelper.floor_double(
            mapData.zCenter + (localPixelZ - 64.0F) * this.getBlocksPerPixel(mapData));
    }

    private TFMagicMapRenderer getDelegate() {
        if (this.delegate == null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.gameSettings == null || mc.getTextureManager() == null) {
                return null;
            }
            this.delegate = new TFMagicMapRenderer(mc.gameSettings, mc.getTextureManager());
        }
        return this.delegate;
    }

    private int mapCoordToWorldX(TFMagicMapData mapData, MapCoord coord) {
        return MathHelper.floor_double(
            mapData.xCenter + coord.centerX * (double) this.getBlocksPerPixel(mapData) / 2.0D);
    }

    private int mapCoordToWorldZ(TFMagicMapData mapData, MapCoord coord) {
        return MathHelper.floor_double(
            mapData.zCenter + coord.centerZ * (double) this.getBlocksPerPixel(mapData) / 2.0D);
    }

    private int getBlocksPerPixel(TFMagicMapData mapData) {
        return 1 << mapData.scale;
    }

    public static final class HoveredFeature {

        public final byte featureId;
        public final byte mapCoordX;
        public final byte mapCoordZ;

        private HoveredFeature(byte featureId, byte mapCoordX, byte mapCoordZ) {
            this.featureId = featureId;
            this.mapCoordX = mapCoordX;
            this.mapCoordZ = mapCoordZ;
        }
    }
}
