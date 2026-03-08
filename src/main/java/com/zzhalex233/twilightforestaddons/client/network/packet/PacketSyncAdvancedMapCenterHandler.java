package com.zzhalex233.twilightforestaddons.client.network.packet;

import com.zzhalex233.twilightforestaddons.item.ItemAdvancedMagicMap;
import com.zzhalex233.twilightforestaddons.map.AdvancedMagicMapDataUtils;
import com.zzhalex233.twilightforestaddons.network.packet.PacketSyncAdvancedMapCenter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import twilightforest.TFMagicMapData;

@SideOnly(Side.CLIENT)
public class PacketSyncAdvancedMapCenterHandler implements IMessageHandler<PacketSyncAdvancedMapCenter, IMessage> {
    @Override
    public IMessage onMessage(final PacketSyncAdvancedMapCenter message, MessageContext ctx) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return null;
        }

        mc.addScheduledTask(() -> apply(message, mc));
        return null;
    }

    private static void apply(PacketSyncAdvancedMapCenter message, Minecraft mc) {
        if (mc.world == null) {
            return;
        }

        String mapName = ItemAdvancedMagicMap.MAP_DATA_PREFIX + "_" + message.getMapId();
        TFMagicMapData mapData = (TFMagicMapData) mc.world.loadData(TFMagicMapData.class, mapName);
        if (mapData == null) {
            return;
        }

        mapData.scale = message.getScale();
        mapData.xCenter = message.getXCenter();
        mapData.zCenter = message.getZCenter();
        AdvancedMagicMapDataUtils.setMapDimension(mapData, message.getDimension());
        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.tfDecorations);
        mapData.markDirty();
    }
}