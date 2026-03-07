package com.twilightforestaddons.client.network;

import com.twilightforestaddons.client.network.packet.PacketFullAdvancedMapRefreshHandler;
import com.twilightforestaddons.client.network.packet.PacketSyncAdvancedMapCenterHandler;
import com.twilightforestaddons.network.ModNetwork;
import com.twilightforestaddons.network.packet.PacketFullAdvancedMapRefresh;
import com.twilightforestaddons.network.packet.PacketSyncAdvancedMapCenter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientNetwork {

    private ClientNetwork() {}

    public static void initClient() {
        ModNetwork.CHANNEL.registerMessage(
            PacketSyncAdvancedMapCenterHandler.class,
            PacketSyncAdvancedMapCenter.class,
            1,
            Side.CLIENT);
        ModNetwork.CHANNEL.registerMessage(
            PacketFullAdvancedMapRefreshHandler.class,
            PacketFullAdvancedMapRefresh.class,
            3,
            Side.CLIENT);
    }
}
