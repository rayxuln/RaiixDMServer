package com.raymondlxtech.raiixdmserver;

import com.raymondlxtech.raiixdmserver.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class DMClientThreadRun {
    String host;
    int port;

    Socket clientSocket;
    RaiixDMServer thePlugin;

    //room info
    RaiixDMServerRoom theRoom;

    Entity theExecutor;

    boolean working;

    public int getRoomViewer(){
        return theRoom.viewerNumber;
    }

    public String getRoomId(){
        return theRoom.roomID;
    }

    public DMClientThreadRun(String h, int p, RaiixDMServerRoom r, RaiixDMServer plugin, Entity e) {
        host = h;
        port = p;
        theRoom = r;

        clientSocket = null;

        thePlugin = plugin;
        theExecutor = e;

        working = true;
    }

    public void disconnect()throws IOException {}

    public void sendChatMessageToTheExecutor(String msg) {
        sendChatMessageToTheExecutor(new TranslatableText(msg));
    }

    public void sendChatMessageToTheExecutor(Text msg) {
        if(theExecutor == null)
        {
            thePlugin.theLogger.info(msg.getString());
            return;
        }
//        theExecutor.sendMessage(msg);
        theExecutor.sendSystemMessage(msg, Util.NIL_UUID);
    }

    public static void printBytes(byte[] bs, int start, int size) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = start; i < start + size; ++i) {
            sb.append(Integer.toHexString(bs[i] & 0xff));
            if (i < start + size - 1) sb.append(',');
        }
        sb.append("]");
        System.out.println(sb.toString());
    }

    public boolean isWorking(){
        return working && clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed();
    }

    public boolean validateDanMu(String dm)
    {
        Config theConfig = thePlugin.theConfigHelper.getConfig();
        if(theConfig.roomConfigs.get(theRoom.roomID) != null) theConfig = theConfig.roomConfigs.get(theRoom.roomID);

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

    public static Text mapStringToStyledText(String style, HashMap<String, String> mapStr)
    {
        return new StyleParser().parse(style, mapStr);
    }

    private static class StyleParser{
        /*
         *     _styleString := _colorPattern | _keyPattern | _styleString _colorPattern | _styleString _keyPattern
         *     _colorPattern := % _letters %
         *     _keyPattern := {{ _letters }}
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

        private void colorPattern()  throws WrongPatternException{
            match('%');
            letters();
            match('%');
        }

        private void keyPattern()  throws WrongPatternException {
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
                        }catch (WrongPatternException e)
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
                        }catch (WrongPatternException e)
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

        private void match(char c) throws WrongPatternException
        {
            if(style.charAt(next) == c)
            {
                next += 1;
                return;
            }
            throw new WrongPatternException("missmatch with " + c);
        }
    }
}