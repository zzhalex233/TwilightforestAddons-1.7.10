package com.twilightforestaddons.network.packet;

import java.util.Arrays;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import twilightforest.TFMagicMapData;

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

    public int getMapId() {
        return this.mapId;
    }

    public int getXCenter() {
        return this.xCenter;
    }

    public int getZCenter() {
        return this.zCenter;
    }

    public int getDimension() {
        return this.dimension;
    }

    public byte getScale() {
        return this.scale;
    }

    public byte[] getColors() {
        return this.colors;
    }

    public byte[] getFeatures() {
        return this.features;
    }
}
