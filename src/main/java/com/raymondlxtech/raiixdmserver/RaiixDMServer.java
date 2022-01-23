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
import net.minecraft.entity.Entity;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.*;
import java.util.regex.Pattern;


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
        try {
            JsonObject msg_jo = JsonParser.parseString(msg).getAsJsonObject();
            String cmd = msg_jo.get("cmd").getAsString();
            //System.out.println("[Raiix] get a cmd: " + cmd);
            //System.out.println("[RaiixDebug] msg: " + msg);

            // Get room config
            Config rc = theConfigHelper.getConfig();
            HashMap<String, String> mapStr = new HashMap<>();
            for (Map.Entry<String, String> e : rc.customKeys.entrySet()) {
                mapStr.put(e.getKey(), e.getValue());
            }
            if (rc.roomConfigs.get(client.theRoom.roomID) != null) {
                rc = rc.roomConfigs.get(client.theRoom.roomID);
                for (Map.Entry<String, String> e : rc.customKeys.entrySet()) {
                    mapStr.put(e.getKey(), e.getValue());
                }
            }

            if (cmd.equals("DANMU_MSG") && rc.enable_chat_dm) {
                JsonArray info = msg_jo.get("info").getAsJsonArray();
                //System.out.println("[danmu info]: " + info.toString());
                String danmu_msg = info.get(1).getAsString();
                if (!validateDanMu(danmu_msg, client.theRoom.roomID)) {
//                                    thePlugin.theLogger.info("Receive a danmu but blocked due to the black list policy." + danmu_msg);
                    return;
                }
                String danmu_authur = info.get(2).getAsJsonArray().get(1).getAsString();
                JsonArray fanBandInfo = info.get(3).getAsJsonArray();

                int danmu_authur_fan_band_level = 0;
                String danmu_authur_fan_band_name = "";
                int danmu_authur_fan_guard_level = 0;
                boolean hasFanBandInfo = false;
                if (fanBandInfo.size() > 0) {
                    danmu_authur_fan_band_level = info.get(3).getAsJsonArray().get(0).getAsInt();
                    danmu_authur_fan_band_name = info.get(3).getAsJsonArray().get(1).getAsString();
                    danmu_authur_fan_guard_level = info.get(3).getAsJsonArray().get(10).getAsInt();
                    hasFanBandInfo = info.get(3).getAsJsonArray().get(11).getAsInt() != 0;
                }

                int u_level = info.get(4).getAsJsonArray().get(0).getAsInt();

                mapStr.put("uLevel", String.valueOf(u_level));
                mapStr.put("danmuAuthur", danmu_authur);
                mapStr.put("danmuMsg", danmu_msg);
                if (hasFanBandInfo) {
                    mapStr.put("fanLevel", String.valueOf(danmu_authur_fan_band_level));
                    mapStr.put("fanName", danmu_authur_fan_band_name);
                    if (danmu_authur_fan_guard_level > 0) {
                        mapStr.put("fanGuard", String.valueOf(danmu_authur_fan_guard_level));
                    }
                }
                mapStr.put("roomTitle", client.theRoom.roomTitle);
                mapStr.put("roomOwner", client.theRoom.ownerName);

                // Parse styled msg
                Text theDanmuText = mapStringToStyledText(rc.chat_dm_style, mapStr);
//            theRoom.theMinecraftServer.getPlayerManager().sendToAll(theDanmuText);
                if (!theDanmuText.getString().isEmpty())
                    theMinecraftServer.getPlayerManager().broadcast(theDanmuText, MessageType.CHAT, Util.NIL_UUID);
            } else if (cmd.equals("SEND_GIFT") && rc.enable_gift_dm) {
                JsonObject data = msg_jo.get("data").getAsJsonObject();

                String giftName = data.get("giftName").getAsString();
                int num = data.get("num").getAsInt();
                String uname = data.get("uname").getAsString();
                String actionName = data.get("action").getAsString();

                JsonObject fanInfo = data.get("medal_info").getAsJsonObject();
                int fanLevel = fanInfo.get("medal_level").getAsInt();
                String fanName = fanInfo.get("medal_name").getAsString();
                boolean fanShow = fanInfo.get("is_lighted").getAsInt() != 0;
                int fanGuardLevel = fanInfo.get("guard_level").getAsInt();

                mapStr.put("danmuAuthur", uname);
                mapStr.put("num", String.valueOf(num));
                mapStr.put("actionName", actionName);
                mapStr.put("giftName", giftName);
                mapStr.put("roomTitle", client.theRoom.roomTitle);
                mapStr.put("roomOwner", client.theRoom.ownerName);
                if (fanShow) {
                    mapStr.put("fanLevel", String.valueOf(fanLevel));
                    mapStr.put("fanName", fanName);
                    if (fanGuardLevel > 0) {
                        mapStr.put("fanGuard", String.valueOf(fanGuardLevel));
                    }
                }

                // Parse styled msg
                Text theDanmuText = mapStringToStyledText(rc.gift_dm_style, mapStr);
//            theRoom.theMinecraftServer.getPlayerManager().sendToAll(theDanmuText);
                if (!theDanmuText.getString().isEmpty())
                    theMinecraftServer.getPlayerManager().broadcast(theDanmuText, MessageType.CHAT, Util.NIL_UUID);
            } else if (cmd.equals("INTERACT_WORD"))/// msg_type=1 => Enter room, 2 => Subscribe, guard_level > 0 =>
            {
                JsonObject data = msg_jo.get("data").getAsJsonObject();
                String danmu_authur = data.get("uname").getAsString();
                //System.out.println("[Welcome DM]" + danmu_authur + ": " + data.toString());
                int msg_type = data.get("msg_type").getAsInt();

                JsonObject fanInfo = data.get("fans_medal").getAsJsonObject();
                int fanLevel = fanInfo.get("medal_level").getAsInt();
                String fanName = fanInfo.get("medal_name").getAsString();
                boolean fanShow = fanInfo.get("is_lighted").getAsInt() != 0;
                int fanGuardLevel = fanInfo.get("guard_level").getAsInt();

                mapStr.put("danmuAuthur", danmu_authur);
                if (fanShow) {
                    mapStr.put("fanLevel", String.valueOf(fanLevel));
                    mapStr.put("fanName", fanName);
                    if (fanGuardLevel > 0) {
                        mapStr.put("fanGuard", String.valueOf(fanGuardLevel));
                    }
                }
                mapStr.put("roomTitle", client.theRoom.roomTitle);
                mapStr.put("roomOwner", client.theRoom.ownerName);
                if (msg_type == 1  && rc.enable_welcome_dm) // Enter room
                {
                    // Parse styled msg
                    Text theDanmuText = mapStringToStyledText(rc.welcome_dm_style, mapStr);
                    if (!theDanmuText.getString().isEmpty())
                        theMinecraftServer.getPlayerManager().broadcast(theDanmuText, MessageType.CHAT, Util.NIL_UUID);

                } else if (msg_type == 2 && rc.enable_subscribe_dm) // Subscribe
                {
                    // Parse styled msg
                    Text theDanmuText = mapStringToStyledText(rc.subscribe_dm_style, mapStr);
                    if (!theDanmuText.getString().isEmpty())
                        theMinecraftServer.getPlayerManager().broadcast(theDanmuText, MessageType.CHAT, Util.NIL_UUID);
                }


            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void broadcastMessage(String msg) {
        if(theMinecraftServer != null)
            theMinecraftServer.getPlayerManager().broadcast(new TranslatableText(msg), MessageType.CHAT, Util.NIL_UUID);
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

        //System.out.println("Test with ["+dm+"] <=> ["+theConfig.black_dm+"].");

        if("white".equals(theConfig.mode.toLowerCase()))
        {
            return Pattern.matches(theConfig.white_dm, dm);
        }
        if("black".equals(theConfig.mode.toLowerCase()))
        {
            return !Pattern.matches(theConfig.black_dm, dm);
        }
        return true;
    }

    public static Text mapStringToStyledText(String style, HashMap<String, String> mapStr)
    {
        return new StyleParser().parse(style, mapStr);
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
         *      _styleString := _colorPattern | _keyPattern | _plainString | _styleString _colorPattern | _styleString _keyPattern | _styleString _plainString
         *      _plainString := _plainChar | _plainString _plainChar
         *      _plainChar := \{ | \} | \# | \% | \\ | [^{}%#]
         *      _colorPattern := % _letters %
         *      _keyPattern := { #_styleString# { _letters } #_styleString# } | { #_styleString# { ! _letters } #_styleString# } | { #_styleString# { _letters = _numbers} #_styleString# }
         */

        private class WrongPatternException extends Exception {
            public WrongPatternException(String s) {
                super(s);
            }
        }

        private class StyleBaseNode{
            protected Style style = Style.EMPTY;

            public void setStyle(Style s){
                style = s;
            }

            public Style getStyle(){
                return style;
            }
        }

        private interface IStyleNode{
            Text execute();
        }

        private class StyleNode extends StyleBaseNode implements IStyleNode{
            private Queue<IStyleNode> queue;

            private KeyNode relatedNode;

            public StyleNode(){
                queue = new LinkedList<>();
                relatedNode = null;
            }

            public StyleNode(KeyNode _r){
                queue = new LinkedList<>();
                relatedNode = _r;
            }

            public void add(IStyleNode n){
                queue.offer(n);
            }

            @Override
            public Text execute() {
                if(relatedNode == null || (!relatedNode.expression() && ((!relatedNode.inverse() && relatedNode.getValue() != null) || (relatedNode.inverse() && relatedNode.getValue() == null))) || (relatedNode.expression() && relatedNode.calcExpression()))
                {
                    MutableText res = new TranslatableText("").setStyle(Style.EMPTY.withColor(Formatting.WHITE));
                    while(!queue.isEmpty())
                    {
                        IStyleNode n = queue.poll();
                        res.append(n.execute());
                    }
                    return res;
                }
                return new TranslatableText("").setStyle(style);
            }
        }

        private class KeyNode extends StyleBaseNode implements IStyleNode{

            private String key;
            private HashMap<String, String> mapStr;
            private boolean isInverse;
            private boolean isExpression;
            private int rightNum;

            public KeyNode(HashMap<String, String> _m, Style _s){
                mapStr = _m;
                style = _s;

                isInverse = false;
                isExpression = false;
                rightNum = 0;
            }

            private KeyNode() {
            }

            public void setKey(String _k){
                key = _k;
            }

            public boolean inverse(){
                return isInverse;
            }
            public void setInverse(boolean v){
                isInverse = v;
            }

            public boolean expression(){
                return isExpression;
            }
            public void setExpression(boolean v){
                isExpression = v;
            }

            public void setRightNum(int v){
                rightNum = v;
            }
            public boolean calcExpression(){
                if(mapStr.containsKey(key))
                {
                    int leftNum = Integer.parseInt(mapStr.get(key));
                    return leftNum == rightNum;
                }
                return false;
            }

            private String getValue(){
                if(mapStr.containsKey(key))
                    return mapStr.get(key);
                return null;
            }

            @Override
            public Text execute() {
                String value = getValue();
                if(!inverse() && !expression() && value != null)
                    return new TranslatableText(value).setStyle(style);
                return new TranslatableText("").setStyle(style);
            }
        }

        private class PlainNode extends StyleBaseNode implements IStyleNode{
            private String text;

            public PlainNode(String _t, Style _s){
                style = _s;
                text = _t;
            }

            private PlainNode(){}

            @Override
            public Text execute() {
                return new TranslatableText(text).setStyle(style);
            }
        }

        private String style;
        private HashMap<String, String> mapStr;
        private int next;
        //private MutableText result;
        private Style currentStyle;

        public StyleParser()
        {
            next = 0;
        }

        public Text parse(String s, HashMap<String, String> ms)
        {
            //result = new TranslatableText("").setStyle(Style.EMPTY.withColor(Formatting.WHITE));
            StyleNode result = new StyleNode();
            currentStyle = Style.EMPTY;
            style = s;
            next = 0;
            mapStr = ms;
            styleString(result);
            return result.execute();
        }

        private boolean plainChar() {
            if(next >= style.length()) return false;
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

        private void plainString(StyleNode node) {
            if(next >= style.length()) return;
            int start = next;
            while(plainChar());
            int end = next;
            String plain = style.substring(start, end);
            //System.out.println("[Style] found plain: " + plain);


            PlainNode plainNode = new PlainNode(plain, currentStyle);
            node.add(plainNode);
        }

        private void letters(){
            while(Character.isLetter(style.charAt(next)) || style.charAt(next) == '_' || Character.isDigit(style.charAt(next))) next += 1;
        }

        private void numbers(){
            while(Character.isDigit(style.charAt(next))) next += 1;
        }

        private Formatting strToColor(String colorName){
            return Formatting.valueOf(colorName.toUpperCase());
        }

        private void colorPattern(StyleNode node)  throws WrongPatternException {
            match('%');
            int start = next;
            letters();
            int end = next;
            String colorName = style.substring(start, end);
            //System.out.println("[Style] found color: " + colorName);
            match('%');

            try {
                currentStyle = currentStyle.withColor(strToColor(colorName));
            } catch (IllegalArgumentException ignored) {
                PlainNode errorMsg = new PlainNode("<ERROR:wrong color name: " + colorName + "!>", Style.EMPTY.withColor(Formatting.RED));
                node.add(errorMsg);
                next = style.length();
            }
        }

        private void keyPattern(StyleNode node)  throws WrongPatternException {
            match('{');
            KeyNode keyNode = new KeyNode(mapStr, currentStyle);
            if(style.charAt(next) == '#')
            {
                match('#');
                Style saveStyle = currentStyle;
                StyleNode child = new StyleNode(keyNode);
                styleString(child);
                match('#');
                node.add(child);
                currentStyle = saveStyle;
            }
            match('{');

            boolean isInverse = false;
            if(style.charAt(next) == '!')
            {
                next += 1;
                isInverse = true;
                //System.out.println("[Style] key inverse");
            }

            int start = next;
            letters();
            int end = next;
            String key = style.substring(start, end);
            //System.out.println("[Style] found key: " + key);
            keyNode.setKey(key);
            keyNode.setInverse(isInverse);
            node.add(keyNode);

            if(!isInverse)
            {
                if(style.charAt(next) == '=')
                {
                    match('=');

                    int num_start = next;
                    numbers();
                    int num_end = next;

                    int num = Integer.parseInt(style.substring(num_start, num_end));
                    keyNode.setExpression(true);
                    keyNode.setRightNum(num);
                }
            }


            match('}');
            if(style.charAt(next) == '#')
            {
                match('#');
                Style saveStyle = currentStyle;
                StyleNode child = new StyleNode(keyNode);
                styleString(child);
                match('#');
                node.add(child);
                currentStyle = saveStyle;
            }
            match('}');
        }

        private void styleString(StyleNode node)
        {
            while(next < style.length() && style.charAt(next) != '#')
            {
                int start = next;
                plainString(node);
                if(next >= style.length()) break;
                if(style.charAt(next) == '{')
                {
                    try{
                        keyPattern(node);
                    }catch (WrongPatternException e){
                        PlainNode errorMsg = new PlainNode("<ERROR:wrong key pattern! " + e.getMessage() + ">", Style.EMPTY.withColor(Formatting.RED));
                        node.add(errorMsg);
                        next = style.length();
                    }
                }
                if(next >= style.length()) break;
                if(style.charAt(next) == '%')
                {
                    try {
                        colorPattern(node);
                    }catch (WrongPatternException e){
                        PlainNode errorMsg = new PlainNode("<ERROR:wrong color pattern! " + e.getMessage() + ">", Style.EMPTY.withColor(Formatting.RED));
                        node.add(errorMsg);
                        next = style.length();
                    }
                }
                int end = next;
                if(start == end){
                    PlainNode errorMsg = new PlainNode("<ERROR:wrong style pattern!>", Style.EMPTY.withColor(Formatting.RED));
                    node.add(errorMsg);
                    next = style.length();
                }
            }
        }

        private void match(char c) throws WrongPatternException
        {
            if(next < style.length() && style.charAt(next) == c)
            {
                next += 1;
                return;
            }
            throw new WrongPatternException("missmatch with " + c);
        }
    }
}
