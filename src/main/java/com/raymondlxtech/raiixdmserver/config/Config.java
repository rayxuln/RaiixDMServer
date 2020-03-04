package com.raymondlxtech.raiixdmserver.config;

import java.util.HashMap;

public class Config{
    public String black_dm = "";
    public String white_dm = "";
    public String mode = "black";
    public String platform = "bilibili";

    public String chat_dm_style = "%GREEN%[弹幕]%RED%[{{roomOwner}}]%GOLD%[UL{{uLevel}}]%WHITE%<{{danmuAuthur}}>:{{danmuMsg}}";
    public String gift_dm_style = "%BLUE%[礼物]%RED%[{{roomOwner}}]%WHITE%{{danmuAuthur}} {{actionName}}了%GOLD%{{num}}%WHITE%个%LIGHT_PURPLE%{{giftName}}";

    public HashMap<String, Config> roomConfigs = new HashMap<>();

    public HashMap<String, String> customKeys = new HashMap<>();

    public String getStringNotNull(String s)
    {
        return s != null ? s : "";
    }

    public void copyFrom(Config c)
    {
        copyFrom(c, true);
    }
    public void copyFrom(Config c, boolean all)
    {
        black_dm = getStringNotNull(c.black_dm);
        white_dm = getStringNotNull(c.white_dm);
        mode = getStringNotNull(c.mode);
        platform = getStringNotNull(c.platform);

        chat_dm_style = getStringNotNull(c.chat_dm_style);
        gift_dm_style = getStringNotNull(c.gift_dm_style);

        if(all)
        {
            roomConfigs = (HashMap<String, Config>) c.roomConfigs.clone();
            customKeys = (HashMap<String, String>) c.customKeys.clone();
        }
    }

    public void set(String key, String value)
    {
        if(key.equals("black_dm"))
        {
            black_dm = value;
        }else if(key.equals("white_dm"))
        {
            white_dm = value;
        }else if(key.equals("mode"))
        {
            mode = value;
        }else if(key.equals("platform"))
        {
            platform = value;
        }else if(key.equals("chat_dm_style"))
        {
            chat_dm_style = value;
        }else if(key.equals("gift_dm_style"))
        {
            gift_dm_style = value;
        }else
        {
            customKeys.put(key, value);
        }
    }

    public String get(String key)
    {
        if(key.equals("black_dm"))
        {
            return getStringNotNull(black_dm);
        }else if(key.equals("white_dm"))
        {
            return getStringNotNull(white_dm);
        }else if(key.equals("mode"))
        {
            return getStringNotNull(mode);
        }else if(key.equals("platform"))
        {
            return  getStringNotNull(platform);
        }else if(key.equals("chat_dm_style"))
        {
            return  getStringNotNull(chat_dm_style);
        }else if(key.equals("gift_dm_style"))
        {
            return  getStringNotNull(gift_dm_style);
        }else
        {
            return  getStringNotNull(customKeys.get(key));
        }
    }
}
