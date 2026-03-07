package com.twilightforestaddons.client.network.packet;

import net.minecraft.client.Minecraft;

import com.twilightforestaddons.map.AdvancedMagicMapDataUtils;
import com.twilightforestaddons.network.packet.PacketSyncAdvancedMapCenter;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import twilightforest.TFMagicMapData;
import twilightforest.item.ItemTFMagicMap;

@SideOnly(Side.CLIENT)
public class PacketSyncAdvancedMapCenterHandler implements IMessageHandler<PacketSyncAdvancedMapCenter, IMessage> {

    @Override
    public IMessage onMessage(final PacketSyncAdvancedMapCenter message, MessageContext ctx) {
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

    private static void apply(PacketSyncAdvancedMapCenter message, Minecraft mc) {
        if (mc.theWorld == null) {
            return;
        }

        TFMagicMapData mapData = ItemTFMagicMap.getMPMapData(message.getMapId(), mc.theWorld);
        if (mapData == null) {
            return;
        }

        mapData.scale = message.getScale();
        int dx = message.getXCenter() - mapData.xCenter;
        int dz = message.getZCenter() - mapData.zCenter;
        int shiftXPixels = dx >> mapData.scale;
        int shiftZPixels = dz >> mapData.scale;

        AdvancedMagicMapDataUtils.shiftMapContent(mapData, shiftXPixels, shiftZPixels, false);
        mapData.xCenter = message.getXCenter();
        mapData.zCenter = message.getZCenter();
        mapData.dimension = message.getDimension();
        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.featuresVisibleOnMap);
    }
}
