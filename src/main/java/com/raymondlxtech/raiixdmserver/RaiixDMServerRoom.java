package com.raymondlxtech.raiixdmserver;

import net.minecraft.server.MinecraftServer;

public class RaiixDMServerRoom {
    public String ownerName = "Unknown";
    public String roomID = "-1";
    public String roomTitle = "Unknown Title";

    public RaiixDMServer theMod;

    public DMClientThreadRun theClient;
    public MinecraftServer theMinecraftServer;

    public int viewerNumber = 0;
}
