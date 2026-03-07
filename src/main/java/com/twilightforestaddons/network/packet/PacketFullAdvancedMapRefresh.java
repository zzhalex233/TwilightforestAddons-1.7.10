package com.twilightforestaddons.network.packet;

import java.util.Arrays;

import net.minecraft.client.Minecraft;

import com.twilightforestaddons.map.AdvancedMagicMapDataUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import twilightforest.TFMagicMapData;
import twilightforest.item.ItemTFMagicMap;

public class PacketFullAdvancedMapRefresh implements IMessage {

    private int mapId;
    private int xCenter;
    private int zCenter;
    private int dimension;
    private byte scale;
    private byte[] colors;
    private byte[] features;

    public PacketFullAdvancedMapRefresh() {}

    public PacketFullAdvancedMapRefresh(int mapId, TFMagicMapData mapData) {
        this.mapId = mapId;
        this.xCenter = mapData.xCenter;
        this.zCenter = mapData.zCenter;
        this.dimension = mapData.dimension;
        this.scale = mapData.scale;
        this.colors = Arrays.copyOf(mapData.colors, mapData.colors.length);
        this.features = mapData.makeFeatureStorageArray();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mapId = buf.readInt();
        this.xCenter = buf.readInt();
        this.zCenter = buf.readInt();
        this.dimension = buf.readInt();
        this.scale = buf.readByte();

        int colorLength = buf.readUnsignedShort();
        this.colors = new byte[colorLength];
        buf.readBytes(this.colors);

        int featureLength = buf.readUnsignedShort();
        this.features = new byte[featureLength];
        buf.readBytes(this.features);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.mapId);
        buf.writeInt(this.xCenter);
        buf.writeInt(this.zCenter);
        buf.writeInt(this.dimension);
        buf.writeByte(this.scale);
        buf.writeShort(this.colors.length);
        buf.writeBytes(this.colors);
        buf.writeShort(this.features.length);
        buf.writeBytes(this.features);
    }

    public static class Handler implements IMessageHandler<PacketFullAdvancedMapRefresh, IMessage> {

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

            TFMagicMapData mapData = ItemTFMagicMap.getMPMapData(message.mapId, mc.theWorld);
            if (mapData == null) {
                return;
            }

            mapData.xCenter = message.xCenter;
            mapData.zCenter = message.zCenter;
            mapData.dimension = message.dimension;
            mapData.scale = message.scale;

            Arrays.fill(mapData.colors, (byte) 0);
            System.arraycopy(message.colors, 0, mapData.colors, 0, Math.min(mapData.colors.length, message.colors.length));

            mapData.featuresVisibleOnMap.clear();
            if (message.features != null && message.features.length > 0) {
                mapData.updateMPMapData(message.features);
            }
            AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.featuresVisibleOnMap);
        }
    }
}
