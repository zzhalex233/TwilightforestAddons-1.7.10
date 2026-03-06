package com.twilightforestaddons.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.twilightforestaddons.map.AdvancedMagicMapTeleportService;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketTeleportToBossFeature implements IMessage {

    private int mapId;
    private byte featureId;
    private byte centerX;
    private byte centerZ;
    private int mapCenterX;
    private int mapCenterZ;
    private byte mapScale;

    public PacketTeleportToBossFeature() {}

    public PacketTeleportToBossFeature(int mapId, byte featureId, byte centerX, byte centerZ, int mapCenterX,
        int mapCenterZ, byte mapScale) {
        this.mapId = mapId;
        this.featureId = featureId;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.mapCenterX = mapCenterX;
        this.mapCenterZ = mapCenterZ;
        this.mapScale = mapScale;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mapId = buf.readInt();
        this.featureId = buf.readByte();
        this.centerX = buf.readByte();
        this.centerZ = buf.readByte();
        this.mapCenterX = buf.readInt();
        this.mapCenterZ = buf.readInt();
        this.mapScale = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.mapId);
        buf.writeByte(this.featureId);
        buf.writeByte(this.centerX);
        buf.writeByte(this.centerZ);
        buf.writeInt(this.mapCenterX);
        buf.writeInt(this.mapCenterZ);
        buf.writeByte(this.mapScale);
    }

    public int getMapId() {
        return mapId;
    }

    public byte getFeatureId() {
        return featureId;
    }

    public byte getCenterX() {
        return centerX;
    }

    public byte getCenterZ() {
        return centerZ;
    }

    public int getMapCenterX() {
        return mapCenterX;
    }

    public int getMapCenterZ() {
        return mapCenterZ;
    }

    public byte getMapScale() {
        return mapScale;
    }

    public static class Handler implements IMessageHandler<PacketTeleportToBossFeature, IMessage> {

        @Override
        public IMessage onMessage(PacketTeleportToBossFeature message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            AdvancedMagicMapTeleportService.handleTeleportRequest(player, message);
            return null;
        }
    }
}
