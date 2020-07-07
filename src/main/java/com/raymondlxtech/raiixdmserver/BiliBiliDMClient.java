package com.raymondlxtech.raiixdmserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class BiliBiliDMClient implements Runnable {
    final String confURL = "https://api.live.bilibili.com/room/v1/Danmu/getConf?room_id=";
    final String roomInfoURL = "https://api.live.bilibili.com/xlive/web-room/v1/index/getInfoByRoom?room_id=";

    String token;
    String host;
    int port;

    Socket clientSocket;
    BiliBiliDMPlugin thePlugin;

    //room info
    RaiixDMServerRoom theRoom;

    //danmu
    final short protocol = 2;

    boolean working;

    public BiliBiliDMClient(RaiixDMServerRoom r, RaiixDMServer plugin){
        theRoom = r;

        clientSocket = null;

        thePlugin = plugin;

        working = true;
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

    //big
    public int bytesToInt(byte[] bs, int start) {
        int res = 0;
        ByteBuffer bb = ByteBuffer.wrap(bs, start, 4);
        res = bb.getInt();
        return res;
    }

    public void sendSocketData(int action) {
        sendSocketData(action, "");
    }

    public void sendSocketData(int action, String body) {
        sendSocketData(0, (short) 16, (short) protocol, action, 1, body);
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
                        thePlugin.getTheLogger().error("[Raiix client] Error: \n" + e.toString());
                        thePlugin.sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(2)!", theRoom.theClient);
                        working = false;
                    }
                }
            });
            t.start();

        } catch (Exception e) {
            thePlugin.getTheLogger().error("[Raiix] Error:\n" + e.toString());
            thePlugin.sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(3)!", theRoom.theClient);
            working = false;
        }

    }

    public int getRoomViewer(){
        return theRoom.viewerNumber;
    }

    public String getRoomId(){
        return theRoom.roomID;
    }

    public JsonObject getJsonFromURL(String url) throws IOException {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = con.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
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

    public String connect(){
        String roomID = theRoom.roomID;

        try {
            JsonObject resData = getJsonFromURL(confURL + roomID);
            if(resData == null)
            {
                thePlugin.getTheLogger().warning("Try to get info of " + roomID + " fail!");
                return "未找到" + roomID + "房间，" + "请确认房间号是否正确！";
            }

            token = resData.get("data").getAsJsonObject().get("token").getAsString();
            host = resData.get("data").getAsJsonObject().get("host").getAsString();
            port = Integer.parseInt(resData.get("data").getAsJsonObject().get("port").getAsString());

            // Now connect to the actual danmu server
            new Thread((Runnable) this, "BiliBiliDMClientThread").start();

            // Last, get some info of the room
            resData = getJsonFromURL(roomInfoURL + roomID);
            if(resData != null && resData.isJsonObject())
            {
                //theLogger.info("[room" + roomID + "]:" + resData.toString());
                theRoom.ownerName = resData.get("data").getAsJsonObject().get("anchor_info").getAsJsonObject().get("base_info").getAsJsonObject().get("uname").getAsString();
                theRoom.roomTitle = resData.get("data").getAsJsonObject().get("room_info").getAsJsonObject().get("title").getAsString();
            }else
            {
                thePlugin.getTheLogger().warning("Get info of room " + roomID + " fail!");
            }

        } catch (Exception e) {
            thePlugin.getTheLogger().error("request fail!");
//            e.printStackTrace();
//            thePlugin.getRooms().put(roomID, null);
            thePlugin.onClientNeedToBeDisconnect(theRoom.roomID);
            return "连接弹幕服务器失败，请重新尝试！";
        }
        return "";
    }

    public void disconnect() throws IOException {
        if (clientSocket == null || !clientSocket.isConnected() || clientSocket.isClosed()) return;
        clientSocket.close();
        theRoom.state = RaiixDMServerRoom.State.Disconnected;
        thePlugin.getTheLogger().info("Disconnected with dm server.");
        thePlugin.broadcastMessage("已经与直播房间("+theRoom.roomID+")断开连接!");
    }

    public void printBytes(byte[] bs, int start, int size) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = start; i < start + size; ++i) {
            sb.append(Integer.toHexString(bs[i] & 0xff));
            if (i < start + size - 1) sb.append(',');
        }
        sb.append("]");
        thePlugin.getTheLogger().info(sb.toString());
    }

    public boolean isWorking(){
        return working && clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed();
    }



    public int getMatchBlanket(String s, int i){
        int cnt=1;
        for (;i<s.length();++i)
        {
            if(s.charAt(i) == '{') cnt += 1;
            if(s.charAt(i) == '}') cnt -= 1;
            if(cnt == 0) return i;
        }
        return -1;
    }
    public ArrayList<String> divideAllJsonObjects(String rawStr){
        ArrayList<String> res = new ArrayList<>();

        int pos = 0;
        while(pos < rawStr.length()){
            if(rawStr.charAt(pos) == '{')
            {
                int mPos = getMatchBlanket(rawStr, pos+1);
                if(mPos == -1) break;
                if(pos >= mPos) break;
                String msg = rawStr.substring(pos, mPos+1);
                res.add(msg);
//                thePlugin.theLogger.info("pos("+pos+"): " + msg);
                pos = mPos;
            }
            pos += 1;
        }
        return res;
    }

    @Override
    public void run() {
        try {
            clientSocket = new Socket(host, port);
            if (clientSocket.isConnected() && !clientSocket.isInputShutdown()) {
                thePlugin.getTheLogger().info("[Raiix] Connect to danmu server successfully!");
                thePlugin.getTheLogger().info("[Raiix] Sending join msg...");

                Random r = new Random();
                long tempuid = (long) (1e14 + 2e14 * r.nextDouble());
//                String joinMsg = "{\"roomid\":" + getRoomId() + ", \"uid\":" + tempuid + "}";
                String joinMsg = "{\"roomid\": " + getRoomId() + ", \"uid\": 0, \"protover\": 2, \"token\": \"" + token + "\", \"platform\": \"RaiixDM Server\"}";
//                thePlugin.theLogger.info("join msg: " + joinMsg);
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
                            thePlugin.getTheLogger().error("[Raiix heartbeat loop] error:\n" + e.toString());
                        }
                    }
                }).start();
            } else {
                thePlugin.getTheLogger().error("[Raiix] Connect to danmu server Fail!");
            }
            byte[] stableBuffer = new byte[clientSocket.getReceiveBufferSize()];
            byte[] msgBuffer = new byte[clientSocket.getReceiveBufferSize()];
            int error_cnt = 0;
            while (isWorking()) {


                //read dan mu
                InputStream in = clientSocket.getInputStream();
                int size = in.read(stableBuffer);
//                thePlugin.theLogger.info("Receive a data, size: " + size);

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
                            thePlugin.sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(0)!", theRoom.theClient);
                            thePlugin.getTheLogger().error("[Raiix] Wrong packet size");
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
//                    System.out.println("[Raiix] Read action: " + action);
                    bufferPos += 4;

                    // params
                    int bodyLength = packetLength - 16;
                    if (bodyLength == 0) {
//                        System.out.println("[Raiix] this package does not have a msg body");
                        continue;
                    }
                    bufferPos += 4;

                    //body
                    int bodyStartPos = bufferPos;
                    switch (action) {
                        case 1:
                        case 2:
                        case 3: {
                            theRoom.viewerNumber = bytesToInt(stableBuffer, bufferPos);
//                            System.out.println("[Raiix] get viewer: " + theRoom.viewerNumber);
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
//                            System.out.println("[Raiix] get a dan mu: \n" + msg);
                            ArrayList<String> msgs = divideAllJsonObjects(msg);
                            for(String m:msgs){
                                thePlugin.handleDMMessage(m, this);
                            }
                            break;
                        }
                        case 6: {
                            break;
                        }
                        case 8: {
                            thePlugin.getTheLogger().info("DM server responded!");
                            theRoom.state = RaiixDMServerRoom.State.Connected;
                            thePlugin.sendChatMessageToTheExecutor("连接至" + getRoomId() + "房间成功！", theRoom.theClient);
                            break;
                        }
                        case 17: {
                            break;

                        }
                        default: {
                            thePlugin.getTheLogger().info("Unknown Action...");
                            break;
                        }
                    }
                }else if(size == -1){
                    error_cnt += 1;
                    if(error_cnt >= 10)
                    {
                        thePlugin.sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(1)!", theRoom.theClient);
                        thePlugin.getTheLogger().error("[Raiix] Wrong buffer size");
                        working = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            thePlugin.getTheLogger().error("[Raiix] Error happened!\n" + e.toString());
            //sendChatMessageToTheExecutor("[RaiixDM] Connect to dm server fail(1)!");
        } finally {
            thePlugin.onClientNeedToBeDisconnect(theRoom.roomID);
        }
    }


}
