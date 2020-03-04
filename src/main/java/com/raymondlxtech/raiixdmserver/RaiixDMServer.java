package com.raymondlxtech.raiixdmserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raymondlxtech.raiixdmserver.command.*;
import com.raymondlxtech.raiixdmserver.config.ConfigHelper;
import com.raymondlxtech.raiixdmserver.log.Logger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;


public class RaiixDMServer implements ModInitializer {

    public ConfigHelper theConfigHelper;
    public Logger theLogger;
    public HashMap<String, RaiixDMServerRoom> theRooms;

    public DMSReconnectCommand dmsReconnectCommand;
    public DMSInfoCommand dmsInfoCommand;
    public DMSConnectCommand dmsConnectCommand;
    public DMSDisconnectCommand dmsDisconnectCommand;
    public DMSGetCommand dmsGetCommand;
    public DMSSetCommand dmsSetCommand;
    public DMSListCommand dmsListCommand;
    public DMSReloadCommand dmsReloadCommand;

    public RaiixDMServer() {
        theConfigHelper = new ConfigHelper(this);
        theLogger = new Logger("RaiixDMServer");
        theRooms = new HashMap<>();
    }

    @Override
    public void onInitialize() {
        theRooms.clear();
        theConfigHelper.loadConfig();

        // Register commands
        CommandRegistry.INSTANCE.register(false, dispatcher -> {
            dmsConnectCommand = new DMSConnectCommand(this).registry(dispatcher);
            dmsDisconnectCommand = new DMSDisconnectCommand(this).registry(dispatcher);
            dmsInfoCommand = new DMSInfoCommand(this).registry(dispatcher);
            dmsListCommand = new DMSListCommand(this).registry(dispatcher);
            dmsSetCommand = new DMSSetCommand(this).registry(dispatcher);
            dmsGetCommand = new DMSGetCommand(this).registry(dispatcher);
            dmsReloadCommand = new DMSReloadCommand(this).registry(dispatcher);
            dmsReconnectCommand = new DMSReconnectCommand(this).registry(dispatcher);
        });

        ServerStopCallback.EVENT.register(server -> {
            disconnectDMServer("all");
        });
    }

    public void disconnectDMServer(String roomID) {
        if(roomID.equals("all"))
        {
            for(RaiixDMServerRoom room : theRooms.values())
            {
                if(room != null)
                    disconnectDMServer(room.roomID);
            }
            return;
        }
        if(theRooms.get(roomID) == null) return;
        DMClientThreadRun theDMClient = theRooms.get(roomID).theClient;
        if (theDMClient != null) {
            try {
                theDMClient.working = false;
                theDMClient.disconnect();
            } catch (Exception e) {
                theLogger.error("Error happened when try to disconnect!");
                e.printStackTrace();
            } finally {
                theRooms.put(roomID, null);
                theLogger.info("Disconnected with room " + roomID);
            }
        }
    }

    public JsonObject getFromURL(String url) throws IOException {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = con.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuilder res = new StringBuilder();

            while ((line = in.readLine()) != null) {
                res.append(line);
            }
            in.close();

            if (code != 200) return null;
            JsonObject resData = (new JsonParser()).parse(res.toString()).getAsJsonObject();
            return resData;
        } catch (Exception e) {
            throw e;
        }
    }

    public String connectBiliBiliDMServer(String roomID, Entity executor, MinecraftServer server) {
        if(roomID.equals("all")) return "错误的房间号！";
        RaiixDMServerRoom theRoom = theRooms.get(roomID);
        if(theRoom == null)
        {
            theRoom = new RaiixDMServerRoom();
            theRooms.put(roomID, theRoom);
        }
        DMClientThreadRun theDMClient = theRoom.theClient;
        if (theDMClient != null && theDMClient.isWorking())
            return roomID + "已连接!";

        String confURL = "https://api.live.bilibili.com/room/v1/Danmu/getConf?room_id=";
        String roomInfoURL = "https://api.live.bilibili.com/xlive/web-room/v1/index/getInfoByRoom?room_id=";
        try {
            JsonObject resData = getFromURL(confURL + roomID);
            if(resData == null)
            {
                theLogger.warning("Try to get info of " + roomID + " fail!");
                return "未找到" + roomID + "房间，" + "请确认房间号是否正确！";
            }

            String host = resData.get("data").getAsJsonObject().get("host").getAsString();
            String port = resData.get("data").getAsJsonObject().get("port").getAsString();

            theDMClient = new BiliBiliDMClientThreadRun(host, Integer.parseInt(port), theRoom, this, executor);
            // Set up information of the room
            theRoom.theClient = theDMClient;
            theRoom.roomID = roomID;
            theRoom.theMinecraftServer = server;

            // Now connect to the actual danmu server
            new Thread((Runnable) theDMClient, "BiliBiliDMClientThread").start();

            // Last, get some info of the room
            resData = getFromURL(roomInfoURL + roomID);
            if(resData != null)
            {
                //theLogger.info("[room" + roomID + "]:" + resData.toString());
                theRoom.ownerName = resData.get("data").getAsJsonObject().get("anchor_info").getAsJsonObject().get("base_info").getAsJsonObject().get("uname").getAsString();
                theRoom.roomTitle = resData.get("data").getAsJsonObject().get("room_info").getAsJsonObject().get("title").getAsString();
            }else
            {
                theLogger.warning("Get info of room " + roomID + " fail!");
            }

        } catch (Exception e) {
            theLogger.error("request fail!");
            e.printStackTrace();
            theRooms.put(roomID, null);
            return "连接弹幕服务器失败，请重新尝试！";
        }
        return "";
    }
}
