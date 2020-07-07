package com.raymondlxtech.raiixdmserver;

import net.minecraft.entity.Entity;

public class RaiixDMServerRoom {
    public String ownerName = "Unknown";
    public String roomID = "-1";
    public String roomTitle = "Unknown Title";

    public BiliBiliDMClient theClient;
    public Entity theExecutor;

    public enum State{
        Connected,
        Disconnected,
        Reconnecting
    }
    public State state = State.Disconnected;

    public int viewerNumber = 0;
}
