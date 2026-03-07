package com.twilightforestaddons.network;

import com.twilightforestaddons.TwilightForestAddons;
import com.twilightforestaddons.network.packet.PacketRequestAdvancedMapRefresh;
import com.twilightforestaddons.network.packet.PacketTeleportToBossFeature;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ModNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE
        .newSimpleChannel(TwilightForestAddons.MODID);

    private ModNetwork() {}

    public static void initCommon() {
        CHANNEL.registerMessage(
            PacketTeleportToBossFeature.Handler.class,
            PacketTeleportToBossFeature.class,
            0,
            Side.SERVER);
        CHANNEL.registerMessage(
            PacketRequestAdvancedMapRefresh.Handler.class,
            PacketRequestAdvancedMapRefresh.class,
            2,
            Side.SERVER);
    }
}
