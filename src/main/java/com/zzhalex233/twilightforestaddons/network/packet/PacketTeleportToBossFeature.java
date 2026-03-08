package com.zzhalex233.twilightforestaddons.network.packet;

import com.zzhalex233.twilightforestaddons.map.AdvancedMagicMapTeleportService;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTeleportToBossFeature implements IMessage {
    private int mapId;
    private int featureId;
    private byte mapCoordX;
    private byte mapCoordZ;
    private int approxWorldX;
    private int approxWorldZ;

    public PacketTeleportToBossFeature() {
    }

    public PacketTeleportToBossFeature(int mapId, int featureId, byte mapCoordX, byte mapCoordZ, int approxWorldX, int approxWorldZ) {
        this.mapId = mapId;
        this.featureId = featureId;
        this.mapCoordX = mapCoordX;
        this.mapCoordZ = mapCoordZ;
        this.approxWorldX = approxWorldX;
        this.approxWorldZ = approxWorldZ;
    }

    public int getMapId() {
        return this.mapId;
    }

    public int getFeatureId() {
        return this.featureId;
    }

    public byte getMapCoordX() {
        return this.mapCoordX;
    }

    public byte getMapCoordZ() {
        return this.mapCoordZ;
    }

    public int getApproxWorldX() {
        return this.approxWorldX;
    }

    public int getApproxWorldZ() {
        return this.approxWorldZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mapId = buf.readInt();
        this.featureId = buf.readInt();
        this.mapCoordX = buf.readByte();
        this.mapCoordZ = buf.readByte();
        this.approxWorldX = buf.readInt();
        this.approxWorldZ = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.mapId);
        buf.writeInt(this.featureId);
        buf.writeByte(this.mapCoordX);
        buf.writeByte(this.mapCoordZ);
        buf.writeInt(this.approxWorldX);
        buf.writeInt(this.approxWorldZ);
    }

    public static final class Handler implements IMessageHandler<PacketTeleportToBossFeature, IMessage> {
        @Override
        public IMessage onMessage(PacketTeleportToBossFeature message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> AdvancedMagicMapTeleportService.handleTeleportRequest(player, message));
            return null;
        }
    }
}