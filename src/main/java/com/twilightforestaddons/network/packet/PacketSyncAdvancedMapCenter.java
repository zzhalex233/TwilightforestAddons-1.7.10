package com.twilightforestaddons.network.packet;

import net.minecraft.client.Minecraft;

import com.twilightforestaddons.map.AdvancedMagicMapDataUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import twilightforest.TFMagicMapData;
import twilightforest.item.ItemTFMagicMap;

public class PacketSyncAdvancedMapCenter implements IMessage {

    private int mapId;
    private int xCenter;
    private int zCenter;
    private int dimension;
    private byte scale;

    public PacketSyncAdvancedMapCenter() {}

    public PacketSyncAdvancedMapCenter(int mapId, int xCenter, int zCenter, int dimension, byte scale) {
        this.mapId = mapId;
        this.xCenter = xCenter;
        this.zCenter = zCenter;
        this.dimension = dimension;
        this.scale = scale;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mapId = buf.readInt();
        this.xCenter = buf.readInt();
        this.zCenter = buf.readInt();
        this.dimension = buf.readInt();
        this.scale = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.mapId);
        buf.writeInt(this.xCenter);
        buf.writeInt(this.zCenter);
        buf.writeInt(this.dimension);
        buf.writeByte(this.scale);
    }

    public static class Handler implements IMessageHandler<PacketSyncAdvancedMapCenter, IMessage> {

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

            TFMagicMapData mapData = ItemTFMagicMap.getMPMapData(message.mapId, mc.theWorld);
            if (mapData == null) {
                return;
            }

            mapData.scale = message.scale;
            int dx = message.xCenter - mapData.xCenter;
            int dz = message.zCenter - mapData.zCenter;
            int shiftXPixels = dx >> mapData.scale;
            int shiftZPixels = dz >> mapData.scale;

            AdvancedMagicMapDataUtils.shiftMapContent(mapData, shiftXPixels, shiftZPixels, false);
            mapData.xCenter = message.xCenter;
            mapData.zCenter = message.zCenter;
            mapData.dimension = message.dimension;
        }
    }
}
