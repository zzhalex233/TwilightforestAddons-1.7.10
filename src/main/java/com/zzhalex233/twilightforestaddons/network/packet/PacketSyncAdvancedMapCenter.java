package com.zzhalex233.twilightforestaddons.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketSyncAdvancedMapCenter implements IMessage {
    private int mapId;
    private int xCenter;
    private int zCenter;
    private int dimension;
    private byte scale;

    public PacketSyncAdvancedMapCenter() {
    }

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
}