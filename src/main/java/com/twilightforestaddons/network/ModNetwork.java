package com.twilightforestaddons.network;

import com.twilightforestaddons.TwilightForestAddons;
import com.twilightforestaddons.network.packet.PacketSyncAdvancedMapCenter;
import com.twilightforestaddons.network.packet.PacketTeleportToBossFeature;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ModNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE
        .newSimpleChannel(TwilightForestAddons.MODID);

    private ModNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(
            PacketTeleportToBossFeature.Handler.class,
            PacketTeleportToBossFeature.class,
            0,
            Side.SERVER);
        CHANNEL.registerMessage(
            PacketSyncAdvancedMapCenter.Handler.class,
            PacketSyncAdvancedMapCenter.class,
            1,
            Side.CLIENT);
    }
}
