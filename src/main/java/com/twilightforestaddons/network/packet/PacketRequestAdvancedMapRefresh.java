package com.twilightforestaddons.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.twilightforestaddons.map.AdvancedMagicMapRefreshService;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketRequestAdvancedMapRefresh implements IMessage {

    private int mapId;

    public PacketRequestAdvancedMapRefresh() {}

    public PacketRequestAdvancedMapRefresh(int mapId) {
        this.mapId = mapId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mapId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.mapId);
    }

    public int getMapId() {
        return this.mapId;
    }

    public static class Handler implements IMessageHandler<PacketRequestAdvancedMapRefresh, IMessage> {

        @Override
        public IMessage onMessage(PacketRequestAdvancedMapRefresh message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            AdvancedMagicMapRefreshService.handleRefreshRequest(player, message.getMapId());
            return null;
        }
    }
}
