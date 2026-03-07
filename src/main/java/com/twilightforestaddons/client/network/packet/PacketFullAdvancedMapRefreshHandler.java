package com.twilightforestaddons.client.network.packet;

import java.util.Arrays;

import net.minecraft.client.Minecraft;

import com.twilightforestaddons.map.AdvancedMagicMapDataUtils;
import com.twilightforestaddons.network.packet.PacketFullAdvancedMapRefresh;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import twilightforest.TFMagicMapData;
import twilightforest.item.ItemTFMagicMap;

@SideOnly(Side.CLIENT)
public class PacketFullAdvancedMapRefreshHandler implements IMessageHandler<PacketFullAdvancedMapRefresh, IMessage> {

    @Override
    public IMessage onMessage(final PacketFullAdvancedMapRefresh message, MessageContext ctx) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return null;
        }

        mc.func_152344_a(new Runnable() {

            @Override
            public void run() {
                apply(message, mc);
            }
        });
        return null;
    }

    private static void apply(PacketFullAdvancedMapRefresh message, Minecraft mc) {
        if (mc.theWorld == null) {
            return;
        }

        TFMagicMapData mapData = ItemTFMagicMap.getMPMapData(message.getMapId(), mc.theWorld);
        if (mapData == null) {
            return;
        }

        mapData.xCenter = message.getXCenter();
        mapData.zCenter = message.getZCenter();
        mapData.dimension = message.getDimension();
        mapData.scale = message.getScale();

        Arrays.fill(mapData.colors, (byte) 0);
        System.arraycopy(
            message.getColors(),
            0,
            mapData.colors,
            0,
            Math.min(mapData.colors.length, message.getColors().length));

        mapData.featuresVisibleOnMap.clear();
        if (message.getFeatures() != null && message.getFeatures().length > 0) {
            mapData.updateMPMapData(message.getFeatures());
        }
        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.featuresVisibleOnMap);
    }
}
