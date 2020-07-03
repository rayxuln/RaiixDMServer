package com.raymondlxtech.raiixdmserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raymondlxtech.raiixdmserver.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class BiliBiliDMClientThreadRun extends DMClientThreadRun implements Runnable {
    String token;

    public BiliBiliDMClientThreadRun(String t, String h, int p, RaiixDMServerRoom r, RaiixDMServer plugin, Entity e) {
        super(h, p, r, plugin, e);
        token = t;
    }

    //big
    public byte[] shortToBytes(short x) {
        byte[] b = new byte[2];
        b[1] = (byte) (x & 0xff);
        b[0] = (byte) (x & 0xff00);
        return b;
    }

    //big
    public byte[] intToBytes(int x) {
        byte[] b = new byte[4];
        b[3] = (byte) (x & 0xff);
        b[2] = (byte) (x & 0xff00);
        b[1] = (byte) (x & 0xff0000);
        b[0] = (byte) (x & 0xff000000);
        return b;
    }

//    public HashMap<String, String> getMapStr()
//    {
//
//    }

    public void sendSocketData(int action) {
        sendSocketData(action, "");
    }

    public void sendSocketData(int action, String body) {
        sendSocketData(0, (short) 16, (short) 2, action, 1, body);
    }

    public void sendSocketData(int packetLength, short magic, short ver, int action, int param, String body) {
        if (clientSocket == null || clientSocket.isConnected() == false) return;
        try {
            byte[] bodyBytes = body.getBytes("utf-8");
            if (packetLength == 0) {
                packetLength = bodyBytes.length + 16;
            }


            ByteArrayOutputStream bout = new ByteArrayOutputStream(packetLength);
            bout.write(intToBytes(packetLength));
            bout.write(shortToBytes(magic));
            bout.write(shortToBytes(ver));
            bout.write(intToBytes(action));
            bout.write(intToBytes(param));

            if (bodyBytes.length > 0) {
                bout.write(bodyBytes);
            }

            byte[] buffer = bout.toByteArray();

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //System.out.println("[Raiix client] Writing buffer length: " + buffer.length);
                        OutputStream os = clientSocket.getOutputStream();
                        os.write(buffer);
                        os.flush();

//                        System.out.println("[Raiix client] Sended package data:");
//                        printBytes(buffer, 0, buffer.length);
                    } catch (Exception e) {
                        System.out.println("[Raiix client] Error: \n" + e.toString());
                        sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(2)!");
                        working = false;
                    }
                }
            });
            t.start();

        } catch (Exception e) {
            System.out.println("[Raiix] Error:\n" + e.toString());
            sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(3)!");
            working = false;
        }

    }

    //big
    public int bytesToInt(byte[] bs, int start) {
        int res = 0;
//        res = res | bs[start + 3];
//        res = res | bs[start + 2] << 8;
//        res = res | bs[start + 1] << 16;
//        res = res | bs[start + 0] << 24;
        ByteBuffer bb = ByteBuffer.wrap(bs, start, 4);
        res = bb.getInt();
        return res;
    }


    public void handleDMMessage(String msg)
    {
        JsonObject msg_jo = new JsonParser().parse(msg).getAsJsonObject();
        String cmd = msg_jo.get("cmd").getAsString();
        //System.out.println("[Raiix] get a cmd: " + cmd);
        if (cmd.equals("DANMU_MSG")) {
            JsonArray info = msg_jo.get("info").getAsJsonArray();
            String danmu_msg = info.get(1).getAsString();
            if(!validateDanMu(danmu_msg))
            {
//                                    thePlugin.theLogger.info("Receive a danmu but blocked due to the black list policy." + danmu_msg);
                return;
            }
            String danmu_authur = info.get(2).getAsJsonArray().get(1).getAsString();
            int u_level = info.get(4).getAsJsonArray().get(0).getAsInt();

            //String danmu = String.format("[弹幕][UL%d]<%s>: %s", u_level, danmu_authur, danmu_msg);

            //System.out.println(danmu);

            // Get room config
            Config rc = thePlugin.theConfigHelper.getConfig();
            HashMap<String, String> mapStr = new HashMap<>();
            for(Map.Entry<String, String> e : rc.customKeys.entrySet())
            {
                mapStr.put(e.getKey(), e.getValue());
            }
            if(rc.roomConfigs.get(theRoom.roomID) != null)
            {
                rc = rc.roomConfigs.get(theRoom.roomID);
                for(Map.Entry<String, String> e : rc.customKeys.entrySet())
                {
                    mapStr.put(e.getKey(), e.getValue());
                }
            }
            mapStr.put("uLevel", String.valueOf(u_level));
            mapStr.put("danmuAuthur", danmu_authur);
            mapStr.put("danmuMsg", danmu_msg);
            mapStr.put("roomTitle", theRoom.roomTitle);
            mapStr.put("roomOwner", theRoom.ownerName);

            // Parse styled msg
            Text theDanmuText = DMClientThreadRun.mapStringToStyledText(rc.chat_dm_style, mapStr);
            theRoom.theMinecraftServer.getPlayerManager().sendToAll(theDanmuText);

        } else if (cmd.equals("SEND_GIFT")) {
            JsonObject data = msg_jo.get("data").getAsJsonObject();

            String giftName = data.get("giftName").getAsString();
            int num = data.get("num").getAsInt();
            String uname = data.get("uname").getAsString();
            String actionName = data.get("action").getAsString();

//                                String gift_msg = String.format("[礼物] %s%s%d个%s", uname, actionName, num, giftName);



//                                System.out.println("[Raiix] " + uname + " has sent a gift!");

            // Get room config
            Config rc = thePlugin.theConfigHelper.getConfig();
            HashMap<String, String> mapStr = new HashMap<>();
            for(Map.Entry<String, String> e : rc.customKeys.entrySet())
            {
                mapStr.put(e.getKey(), e.getValue());
            }
            if(rc.roomConfigs.get(theRoom.roomID) != null)
            {
                rc = rc.roomConfigs.get(theRoom.roomID);
                for(Map.Entry<String, String> e : rc.customKeys.entrySet())
                {
                    mapStr.put(e.getKey(), e.getValue());
                }
            }
            mapStr.put("danmuAuthur", uname);
            mapStr.put("num", String.valueOf(num));
            mapStr.put("actionName", actionName);
            mapStr.put("giftName", giftName);
            mapStr.put("roomTitle", theRoom.roomTitle);
            mapStr.put("roomOwner", theRoom.ownerName);

            // Parse styled msg
            Text theDanmuText = DMClientThreadRun.mapStringToStyledText(rc.gift_dm_style, mapStr);
            theRoom.theMinecraftServer.getPlayerManager().sendToAll(theDanmuText);
        }
//                            else if (cmd.equals("PREPARING")) {
//                                //System.out.println("[Raiix] live is preparing...");
//                            } else if (cmd.equals("LIVE")) {
//                                //System.out.println("[Raiix] live is started!");
//                            } else if (cmd.equals("GUARD_MSG")) {
//
//                            }
    }


    public void disconnect() throws IOException {
        if (clientSocket == null || !clientSocket.isConnected() || clientSocket.isClosed()) return;
        clientSocket.close();
        System.out.println("Disconnected with dm server.");
        theRoom.theMinecraftServer.getPlayerManager().sendToAll(new TranslatableText("已经与直播房间("+theRoom.roomID+")断开连接!"));
    }

    @Override
    public void run() {
        try {
            clientSocket = new Socket(host, port);
            if (clientSocket.isConnected() && !clientSocket.isInputShutdown()) {
                System.out.println("[Raiix] Connect to danmu server successfully!");
                System.out.println("[Raiix] Sending join msg...");

                Random r = new Random();
                long tempuid = (long) (1e14 + 2e14 * r.nextDouble());
//                String joinMsg = "{\"roomid\":" + getRoomId() + ", \"uid\":" + tempuid + "}";
                String joinMsg = "{\"roomid\": " + getRoomId() + ", \"uid\": 0, \"protover\": 2, \"token\": \"" + token + "\", \"platform\": \"RaiixDM Server\"}";
                sendSocketData(7, joinMsg);


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (isWorking()) {
                                //System.out.println("[Raiix client] starting heartbeat...");
                                sendSocketData(2);
                                Thread.sleep(30000);
                            }
                        } catch (Exception e) {
                            System.out.println("[Raiix heartbeat loop] error:\n" + e.toString());
                        }
                    }
                }).start();
            } else {
                System.out.println("[Raiix] Connect to danmu server Fail!");
            }
            byte[] stableBuffer = new byte[clientSocket.getReceiveBufferSize()];
            byte[] msgBuffer = new byte[clientSocket.getReceiveBufferSize()];
            int error_cnt = 0;
            while (isWorking()) {


                //read dan mu
                InputStream in = clientSocket.getInputStream();
                int size = in.read(stableBuffer);

                if (size > 0) {
                    //System.out.println("[Raiix] got a package from server...");
                    //System.out.println("[Raiix] bytes: ");
                    //printBytes(stableBuffer, 0, size);

                    int bufferPos = 0;
                    int packetLength = bytesToInt(stableBuffer, bufferPos);
                    //System.out.println("[Raiix] Read a packet size: " + packetLength);
                    if (packetLength < 16 || packetLength > size) {
                        error_cnt += 1;
                        if(error_cnt >= 10)
                        {
                            sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(0)!");
                            System.out.println("[Raiix] Wrong packet size");
                            working = false;
                            break;
                        }
                        continue;
                    }else
                    {
                        error_cnt = 0;
                    }
                    bufferPos += 4;

                    //magic
                    bufferPos += 2;
                    //ver
                    bufferPos += 2;

                    //action
                    int action = bytesToInt(stableBuffer, bufferPos);
                    //System.out.println("[Raiix] Read action: " + action);
                    bufferPos += 4;

                    // params
                    int bodyLength = packetLength - 16;
                    if (bodyLength == 0) {
                        //System.out.println("[Raiix] this package does not have a msg body");
                        continue;
                    }
                    bufferPos += 4;

                    //body
                    int bodyStartPos = bufferPos;
                    switch (action) {
                        case 1:
                        case 2:
                        case 3: {
                            //System.out.println("[Raiix] get viewer: " + viewer);
                            theRoom.viewerNumber = bytesToInt(stableBuffer, bufferPos);
                            break;
                        }
                        case 4:
                        case 5: {
                            // Need to decompress the data
//                            printBytes(stableBuffer, bodyStartPos, bodyLength);
                            Inflater decompresser = new Inflater();
                            decompresser.setInput(stableBuffer, bodyStartPos, bodyLength);
                            int decompressed_size = 0;
                            try {
                                decompressed_size = decompresser.inflate(msgBuffer);
                                decompresser.end();
                            } catch (DataFormatException e){
//                                thePlugin.theLogger.error("Decompressing body data error \n" + e.toString());
                                break;
                            }

                            String msg = new String(msgBuffer, 0, decompressed_size, "utf-8");
                            ArrayList<String> msgs = divideAllJsonObjects(msg);
                            for(String m:msgs){
                                handleDMMessage(m);
                            }

                            break;
                        }
                        case 6: {
                            break;
                        }
                        case 8: {
                            System.out.println("DM server responded!");
                            sendChatMessageToTheExecutor("连接至" + getRoomId() + "房间成功！");
                            break;
                        }
                        case 17: {
                            break;

                        }
                        default: {
                            System.out.println("Unknown Action...");
                            break;
                        }
                    }
                }else if(size == -1){
                    error_cnt += 1;
                    if(error_cnt >= 10)
                    {
                        sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(1)!");
                        System.out.println("[Raiix] Wrong buffer size");
                        working = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[Raiix] Error happened!\n" + e.toString());
            //sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(1)!");
        } finally {
            thePlugin.disconnectDMServer(theRoom.roomID);
        }
    }
}
