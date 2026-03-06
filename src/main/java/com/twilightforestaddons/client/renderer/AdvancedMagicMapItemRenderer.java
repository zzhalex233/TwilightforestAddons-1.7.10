package com.twilightforestaddons.client.renderer;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import com.twilightforestaddons.item.ModItems;
import com.twilightforestaddons.map.AdvancedMagicMapDataUtils;

import twilightforest.TFMagicMapData;
import twilightforest.client.renderer.TFMagicMapRenderer;

public class AdvancedMagicMapItemRenderer extends TFMagicMapRenderer {

    public AdvancedMagicMapItemRenderer(net.minecraft.client.settings.GameSettings gameSettings,
        net.minecraft.client.renderer.texture.TextureManager textureManager) {
        super(gameSettings, textureManager);
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return item != null && item.getItem() == ModItems.advancedMagicMap && type == ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public void renderMap(EntityPlayer player, TextureManager textureManager, TFMagicMapData mapData) {
        super.renderMap(player, textureManager, mapData);
        DefeatedBossIconOverlayRenderer.renderDefeatedBossOverlays(
            player,
            textureManager,
            AdvancedMagicMapDataUtils.createDedupedFeatureSnapshot(mapData));
    }
}
