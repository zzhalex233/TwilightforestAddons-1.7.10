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
    private byte mapCoordX;
    private byte mapCoordZ;

    public PacketTeleportToBossFeature() {}

    public PacketTeleportToBossFeature(int mapId, byte featureId, byte mapCoordX, byte mapCoordZ) {
        this.mapId = mapId;
        this.featureId = featureId;
        this.mapCoordX = mapCoordX;
        this.mapCoordZ = mapCoordZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mapId = buf.readInt();
        this.featureId = buf.readByte();
        this.mapCoordX = buf.readByte();
        this.mapCoordZ = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.mapId);
        buf.writeByte(this.featureId);
        buf.writeByte(this.mapCoordX);
        buf.writeByte(this.mapCoordZ);
    }

    public int getMapId() {
        return this.mapId;
    }

    public byte getFeatureId() {
        return this.featureId;
    }

    public byte getMapCoordX() {
        return this.mapCoordX;
    }

    public byte getMapCoordZ() {
        return this.mapCoordZ;
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
