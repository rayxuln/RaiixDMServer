package com.raymondlxtech.raiixdmserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raymondlxtech.raiixdmserver.command.*;
import com.raymondlxtech.raiixdmserver.config.Config;
import com.raymondlxtech.raiixdmserver.config.ConfigHelper;
import com.raymondlxtech.raiixdmserver.log.Logger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.launch.FabricServerTweaker;
import net.fabricmc.loader.launch.common.FabricLauncher;
import net.fabricmc.loom.util.FabricApiExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class RaiixDMServer implements ModInitializer, BiliBiliDMPlugin {

    public ConfigHelper theConfigHelper;
    public Logger theLogger;
    public HashMap<String, RaiixDMServerRoom> theRooms;

    public MinecraftServer theMinecraftServer;

    public RaiixDMSCommand dmsReconnectCommand;
    public RaiixDMSCommand dmsInfoCommand;
    public RaiixDMSCommand dmsConnectCommand;
    public RaiixDMSCommand dmsDisconnectCommand;
    public RaiixDMSCommand dmsGetCommand;
    public RaiixDMSCommand dmsSetCommand;
    public RaiixDMSCommand dmsListCommand;
    public RaiixDMSCommand dmsReloadCommand;
    public RaiixDMSCommand dmsAddRoomCommand;
    public RaiixDMSCommand dmsRemoveRoomCommand;
    public RaiixDMSCommand dmsPreviewCommand;

    public RaiixDMServer() {
        theConfigHelper = new ConfigHelper(this);
        theLogger = new Logger("RaiixDMServer");
        theRooms = new HashMap<>();
    }

    @Override
    public void onInitialize() {
        theRooms.clear();
        theConfigHelper.loadConfig();

        ServerStartCallback.EVENT.register(minecraftServer -> {
            theMinecraftServer = minecraftServer;
        });

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dmsConnectCommand = new DMSConnectCommand(this).registry(dispatcher);
            dmsDisconnectCommand = new DMSDisconnectCommand(this).registry(dispatcher);
            dmsInfoCommand = new DMSInfoCommand(this).registry(dispatcher);
            dmsListCommand = new DMSListCommand(this).registry(dispatcher);
            dmsSetCommand = new DMSSetCommand(this).registry(dispatcher);
            dmsGetCommand = new DMSGetCommand(this).registry(dispatcher);
            dmsReloadCommand = new DMSReloadCommand(this).registry(dispatcher);
            dmsReconnectCommand = new DMSReconnectCommand(this).registry(dispatcher);
            dmsRemoveRoomCommand = new DMSRemoveRoomCommand(this).registry(dispatcher);
            dmsAddRoomCommand = new DMSAddRoomCommand(this).registry(dispatcher);
            dmsPreviewCommand = new DMSPreviewCommand(this).registry(dispatcher);

            loadAddedRooms();
        });

        ServerStopCallback.EVENT.register(server -> {
            disconnectDMServer("all");
            theMinecraftServer = null;
        });


    }

    public void loadAddedRooms(){
        ArrayList<String> roomIDs = theConfigHelper.getConfig().addedRooms;
        for (String roomID:roomIDs) {
            dmsAddRoomCommand.execute(null, new String[]{roomID});
        }
    }

    public void saveAddedRooms(){
        theConfigHelper.getConfig().addedRooms = new ArrayList<>(theRooms.keySet());
        theConfigHelper.saveConfig();
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
        BiliBiliDMClient theDMClient = theRooms.get(roomID).theClient;
        if (theDMClient != null) {
            try {
                theDMClient.working = false;
                theDMClient.disconnect();
            } catch (Exception e) {
                theLogger.error("Error happened when try to disconnect!");
                e.printStackTrace();
            } finally {
                theLogger.info("Disconnected with room " + roomID);
            }
        }
    }

    public String connectBiliBiliDMServer(String roomID, Entity executor) {
        if(roomID.equals("all")) return "错误的房间号！";
        if(theRooms.get(roomID) == null)
        {
            dmsAddRoomCommand.execute(executor, new String[]{roomID});
        }
        RaiixDMServerRoom theRoom = theRooms.get(roomID);
        BiliBiliDMClient theDMClient = theRoom.theClient;
        if (theDMClient != null && theDMClient.isWorking())
            return roomID + "已连接!";

        theDMClient = new BiliBiliDMClient(theRoom, this);
        theRoom.theExecutor = executor;
        theRoom.theClient = theDMClient;
        return theDMClient.connect();
    }

    @Override
    public ConfigHelper getConfigHelper() {
        return theConfigHelper;
    }

    @Override
    public Logger getTheLogger() {
        return theLogger;
    }

    @Override
    public HashMap<String, RaiixDMServerRoom> getRooms() {
        return theRooms;
    }

    @Override
    public void sendChatMessageToTheExecutor(String msg, BiliBiliDMClient client) {
        sendChatMessageToTheExecutor(new TranslatableText(msg), client);
    }

    @Override
    public void handleDMMessage(String msg, BiliBiliDMClient client) {
        JsonObject msg_jo = new JsonParser().parse(msg).getAsJsonObject();
        String cmd = msg_jo.get("cmd").getAsString();
        //System.out.println("[Raiix] get a cmd: " + cmd);
        //System.out.println("[RaiixDebug] msg: " + msg);
        if (cmd.equals("DANMU_MSG")) {
            JsonArray info = msg_jo.get("info").getAsJsonArray();
            String danmu_msg = info.get(1).getAsString();
            if(!validateDanMu(danmu_msg, client.theRoom.roomID))
            {
//                                    thePlugin.theLogger.info("Receive a danmu but blocked due to the black list policy." + danmu_msg);
                return;
            }
            String danmu_authur = info.get(2).getAsJsonArray().get(1).getAsString();
            int u_level = info.get(4).getAsJsonArray().get(0).getAsInt();

            //String danmu = String.format("[弹幕][UL%d]<%s>: %s", u_level, danmu_authur, danmu_msg);

            //System.out.println(danmu);

            // Get room config
            Config rc = theConfigHelper.getConfig();
            HashMap<String, String> mapStr = new HashMap<>();
            for(Map.Entry<String, String> e : rc.customKeys.entrySet())
            {
                mapStr.put(e.getKey(), e.getValue());
            }
            if(rc.roomConfigs.get(client.theRoom.roomID) != null)
            {
                rc = rc.roomConfigs.get(client.theRoom.roomID);
                for(Map.Entry<String, String> e : rc.customKeys.entrySet())
                {
                    mapStr.put(e.getKey(), e.getValue());
                }
            }
            mapStr.put("uLevel", String.valueOf(u_level));
            mapStr.put("danmuAuthur", danmu_authur);
            mapStr.put("danmuMsg", danmu_msg);
            mapStr.put("roomTitle", client.theRoom.roomTitle);
            mapStr.put("roomOwner", client.theRoom.ownerName);

            // Parse styled msg
            Text theDanmuText = mapStringToStyledText(rc.chat_dm_style, mapStr);
//            theRoom.theMinecraftServer.getPlayerManager().sendToAll(theDanmuText);
            theMinecraftServer.getPlayerManager().broadcastChatMessage(theDanmuText, MessageType.CHAT, Util.NIL_UUID);
        } else if (cmd.equals("SEND_GIFT")) {
            JsonObject data = msg_jo.get("data").getAsJsonObject();

            String giftName = data.get("giftName").getAsString();
            int num = data.get("num").getAsInt();
            String uname = data.get("uname").getAsString();
            String actionName = data.get("action").getAsString();

//                                String gift_msg = String.format("[礼物] %s%s%d个%s", uname, actionName, num, giftName);



//                                System.out.println("[Raiix] " + uname + " has sent a gift!");

            // Get room config
            Config rc = theConfigHelper.getConfig();
            HashMap<String, String> mapStr = new HashMap<>();
            for(Map.Entry<String, String> e : rc.customKeys.entrySet())
            {
                mapStr.put(e.getKey(), e.getValue());
            }
            if(rc.roomConfigs.get(client.theRoom.roomID) != null)
            {
                rc = rc.roomConfigs.get(client.theRoom.roomID);
                for(Map.Entry<String, String> e : rc.customKeys.entrySet())
                {
                    mapStr.put(e.getKey(), e.getValue());
                }
            }
            mapStr.put("danmuAuthur", uname);
            mapStr.put("num", String.valueOf(num));
            mapStr.put("actionName", actionName);
            mapStr.put("giftName", giftName);
            mapStr.put("roomTitle", client.theRoom.roomTitle);
            mapStr.put("roomOwner", client.theRoom.ownerName);

            // Parse styled msg
            Text theDanmuText = mapStringToStyledText(rc.gift_dm_style, mapStr);
//            theRoom.theMinecraftServer.getPlayerManager().sendToAll(theDanmuText);
            theMinecraftServer.getPlayerManager().broadcastChatMessage(theDanmuText, MessageType.CHAT, Util.NIL_UUID);
        }
//                            else if (cmd.equals("PREPARING")) {
//                                //System.out.println("[Raiix] live is preparing...");
//                            } else if (cmd.equals("LIVE")) {
//                                //System.out.println("[Raiix] live is started!");
//                            } else if (cmd.equals("GUARD_MSG")) {
//
//                            }
    }

    @Override
    public void broadcastMessage(String msg) {
        if(theMinecraftServer != null)
            theMinecraftServer.getPlayerManager().broadcastChatMessage(new TranslatableText(msg), MessageType.CHAT, Util.NIL_UUID);
    }



    @Override
    public void onClientNeedToBeDisconnect(String roomID) {
        disconnectDMServer(roomID);

        RaiixDMServerRoom room = theRooms.get(roomID);
        boolean is_auto_reconnect = getConfigHelper().getConfig().auto_reconnect;
        long delay = getConfigHelper().getConfig().auto_reconnect_delay;

        Config roomConfig = getConfigHelper().getConfig().roomConfigs.get(roomID);
        if(roomConfig != null)
        {
            is_auto_reconnect = getConfigHelper().getConfig().roomConfigs.get(roomID).auto_reconnect;
            delay = getConfigHelper().getConfig().roomConfigs.get(roomID).auto_reconnect_delay;
        }
        if(is_auto_reconnect && room != null && room.state != RaiixDMServerRoom.State.Reconnecting)
        {
            broadcastMessage("准备重新连接弹幕房间" + roomID);
            room.state = RaiixDMServerRoom.State.Reconnecting;
            long finalDelay = delay;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(finalDelay);
                        if(theRooms.containsKey(roomID))
                            dmsConnectCommand.execute(null, new String[]{roomID});
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public boolean validateDanMu(String dm, String roomID)
    {
        Config theConfig = theConfigHelper.getConfig();
        if(theConfig.roomConfigs.get(roomID) != null) theConfig = theConfig.roomConfigs.get(roomID);

//        thePlugin.theLogger.info("Test with ["+dm+"] <=> ["+theConfig.black_dm+"].");

        if("white".equals(theConfig.mode.toLowerCase()))
        {
            String[] words = theConfig.white_dm.split("\\|");
            for (String w : words) {
                if (dm.contains(w)){
                    return true;
                }
            }
            return  false;
        }
        String[] words = theConfig.black_dm.split("\\|");
        for (String w: words) {
            if(!w.isEmpty() && dm.contains(w)) {
                return false;
            }
        }
        return true;
    }

    public static Text mapStringToStyledText(String style, HashMap<String, String> mapStr)
    {
        return new StyleParserV2().parse(style, mapStr);
    }

    public void sendChatMessageToTheExecutor(Text msg, BiliBiliDMClient client) {
        if(client.theRoom.theExecutor == null)
        {
            theLogger.info(msg.getString());
            return;
        }
        client.theRoom.theExecutor.sendSystemMessage(msg, Util.NIL_UUID);
    }

    private static class StyleParser{
        /*
         *     _styleString := _colorPattern | _keyPattern | _styleString _colorPattern | _styleString _keyPattern
         *     _colorPattern := % _letters %
         *     _keyPattern := {{ _letters }}
         */

        /*
        *      _styleString := _colorPattern | _keyPattern | _plainString | _styleString _colorPattern | _styleString _keyPattern | _styleString _plainString
        *      _plainString := _plainChar | _plainString _plainChar
        *      _plainChar := \{ | \% | [^{%]
        *      _colorPattern := % _letters %
        *      _keyPattern := { _styleString { _letters } _styleString } | { _styleString { ! _letters } _styleString }
        */

        private class WrongPatternException extends Exception {
            public WrongPatternException(String s) {
                super(s);
            }
        }

        private String style;
        private HashMap<String, String> mapStr;
        private int next;
        private MutableText result;
        private Formatting currentColor;

        public StyleParser()
        {
            next = 0;
        }

        public Text parse(String s, HashMap<String, String> ms)
        {
            result = new TranslatableText("").setStyle(Style.EMPTY.withColor(Formatting.WHITE));
            currentColor = Formatting.WHITE;
            style = s;
            next = 0;
            mapStr = ms;
            styleString();
            return result;
        }

        private void letters(){
            while(Character.isLetter(style.charAt(next)) || style.charAt(next) == '_' || Character.isDigit(style.charAt(next))) next += 1;
        }

        private void colorPattern()  throws StyleParser.WrongPatternException {
            match('%');
            letters();
            match('%');
        }

        private void keyPattern()  throws StyleParser.WrongPatternException {
            match('{');
            match('{');
            letters();
            match('}');
            match('}');
        }

        private void styleString()
        {
            int aStart = 0;
            while(next < style.length())
            {
                boolean hasIn = (next - 1 < 0 || style.charAt(next-1) != '\\');
                hasIn = hasIn && style.charAt(next) == '%' || (style.charAt(next) == '{' && style.charAt(next+1) == '{');
                if(hasIn)
                {
                    int aEnd = next;
                    if(aStart < aEnd && aStart >= 0 && aEnd < style.length())
                    {
                        String piece = style.substring(aStart, aEnd);
//                        System.out.println("[Raiix parse] piece=" + piece);
                        result.append(new TranslatableText(piece).setStyle(Style.EMPTY.withColor(currentColor)));


                        aStart = aEnd + 1;
                    }
                }


                if((next - 1 < 0 || style.charAt(next-1) != '\\'))
                {
                    int start = 0;
                    int end = 0;
                    boolean success = false;

                    if(style.charAt(next) == '%')
                    {
                        hasIn = true;
                        start = next + 1;
                        success = true;
                        try {
                            colorPattern();
                        }catch (StyleParser.WrongPatternException e)
                        {
                            Text temp = new TranslatableText("<ERROR>").setStyle(Style.EMPTY.withColor(Formatting.RED));


                            result.append(temp);
                            success = false;
                        }
                        end = next - 1;

//                        System.out.println("[Raiix parsing color] start="+start + ", end="+end+" , success="+success);
                        if(success && end - start > 0)
                        {
                            if(start >= 0 && end < style.length() )
                            {
                                String colorName = style.substring(start, end);
//                                System.out.println("ColorName=" + colorName);
                                Formatting color = null;
                                try {
                                    color = Formatting.valueOf(colorName.toUpperCase());
                                }catch (IllegalArgumentException e) {}
                                if(color != null) {
                                    currentColor = color;
//                                    System.out.println("Color found!");
                                }
                                else {
                                    currentColor = Formatting.WHITE;
//                                    System.out.println("Color not found!");
                                }

                            }
                        }

                        aStart = next;
                        continue;
                    }else if(style.charAt(next) == '{' && style.charAt(next+1) == '{')
                    {
                        hasIn = true;
                        start = next + 2;
                        success = true;
                        try {
                            keyPattern();
                        }catch (StyleParser.WrongPatternException e)
                        {
                            Text temp = new TranslatableText("<ERROR>").setStyle(Style.EMPTY.withColor(Formatting.RED));
                            result.append(temp);
                            success = false;
                        }
                        end = next - 2;

//                        System.out.println("[Raiix parsing key] start="+start + ", end="+end+" , success="+success);
                        if(success && end - start > 0)
                        {
                            if(start >= 0 && end < style.length() ) {
                                String keyName = style.substring(start, end);
//                                System.out.println("KeyName="+keyName);
                                String value = mapStr.get(keyName);
                                if (value != null) {
                                    result.append(new TranslatableText(value).setStyle(Style.EMPTY.withColor(currentColor)));
                                }
                            }
                        }

                        aStart = next;
                        continue;
                    }
                }
                next += 1;
            }
        }

        private void match(char c) throws StyleParser.WrongPatternException
        {
            if(style.charAt(next) == c)
            {
                next += 1;
                return;
            }
            throw new StyleParser.WrongPatternException("missmatch with " + c);
        }
    }

    private static class StyleParserV2{
        /*
         *      _styleString := _colorPattern | _keyPattern | _plainString | _styleString _colorPattern | _styleString _keyPattern | _styleString _plainString
         *      _plainString := _plainChar | _plainString _plainChar
         *      _plainChar := \{ | \} | \# | \% | \\ | [^{}%#]
         *      _colorPattern := % _letters %
         *      _keyPattern := { #_styleString# { _letters } #_styleString# } | { #_styleString# { ! _letters } #_styleString# }
         */

        private class WrongPatternException extends Exception {
            public WrongPatternException(String s) {
                super(s);
            }
        }

        private String style;
        private HashMap<String, String> mapStr;
        private int next;
        private MutableText result;
        private Formatting currentColor;

        public StyleParserV2()
        {
            next = 0;
        }

        public Text parse(String s, HashMap<String, String> ms)
        {
            result = new TranslatableText("").setStyle(Style.EMPTY.withColor(Formatting.WHITE));
            currentColor = Formatting.WHITE;
            style = s;
            next = 0;
            mapStr = ms;
            styleString();
            return result;
        }

        private boolean plainChar() {
            if(style.charAt(next) == '\\')
            {
                if(next+1 < style.length() && style.charAt(next+1) == '{' || style.charAt(next+1) == '}' || style.charAt(next+1) == '%' || style.charAt(next+1) == '\\' || style.charAt(next+1) == '#')
                {
                    next += 2;
                    return true;
                }
            }
            if(style.charAt(next) == '{' || style.charAt(next) == '}' || style.charAt(next) == '%' || style.charAt(next) == '#')
                return false;
            next += 1;
            return true;
        }

        private void plainString() {
            int start = next;
            while(plainChar());
            int end = next;
            System.out.println("[Style] found plain: " + style.substring(start, end));
        }

        private void letters(){
            while(Character.isLetter(style.charAt(next)) || style.charAt(next) == '_' || Character.isDigit(style.charAt(next))) next += 1;
        }

        private void colorPattern()  throws StyleParserV2.WrongPatternException {
            match('%');
            int start = next;
            letters();
            int end = next;
            System.out.println("[Style] found color: " + style.substring(start, end));
            match('%');
        }

        private void keyPattern()  throws StyleParserV2.WrongPatternException {
            match('{');
            if(style.charAt(next) == '#')
            {
                match('#');
                styleString();
                match('#');
            }
            match('{');
            if(style.charAt(next) == '!')
            {
                next += 1;
                System.out.println("[Style] key inverse");
            }
            int start = next;
            letters();
            int end = next;
            System.out.println("[Style] found key: " + style.substring(start, end));

            match('}');
            if(style.charAt(next) == '#')
            {
                match('#');
                styleString();
                match('#');
            }
            match('}');
        }

        private void styleString()
        {
            while(next < style.length() && style.charAt(next) != '#')
            {
                plainString();
                if(next >= style.length()) break;
                if(style.charAt(next) == '{')
                {
                    try{
                        keyPattern();
                    }catch (WrongPatternException e){System.out.println(e.getMessage());}
                    catch (Exception e){e.printStackTrace();}
                }
                if(next >= style.length()) break;
                if(style.charAt(next) == '%')
                {
                    try {
                        colorPattern();
                    }catch (WrongPatternException e){System.out.println(e.getMessage());}
                    catch (Exception e){e.printStackTrace();}
                }
            }
        }

        private void match(char c) throws StyleParserV2.WrongPatternException
        {
            if(next < style.length() && style.charAt(next) == c)
            {
                next += 1;
                return;
            }
            throw new StyleParserV2.WrongPatternException("missmatch with " + c);
        }
    }
}
