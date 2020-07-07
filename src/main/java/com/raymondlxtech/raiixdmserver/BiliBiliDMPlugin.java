package com.raymondlxtech.raiixdmserver;

import com.raymondlxtech.raiixdmserver.config.ConfigHelper;
import com.raymondlxtech.raiixdmserver.log.Logger;

import java.util.HashMap;


public interface BiliBiliDMPlugin {
    public ConfigHelper getConfigHelper();
    public Logger getTheLogger();
    public HashMap<String, RaiixDMServerRoom> getRooms();
    public void sendChatMessageToTheExecutor(String msg, BiliBiliDMClient client);
    public void handleDMMessage(String msg, BiliBiliDMClient client);
    public void broadcastMessage(String msg);
    public void onClientNeedToBeDisconnect(String roomID);
}
