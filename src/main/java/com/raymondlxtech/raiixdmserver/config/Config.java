package com.raymondlxtech.raiixdmserver.config;

import java.lang.reflect.Field;
import java.util.*;

public class Config{
    //========== Config properties==========
    @OptionValues(value = {"true", "false"})
    public boolean auto_reconnect = false;
    @OptionValues(value = {"10000", "30000"})
    public long auto_reconnect_delay = 10000;

    @OptionValues(value = {""})
    public String black_dm = "";
    @OptionValues(value = {""})
    public String white_dm = "";
    @OptionValues(value = {"black", "white"})
    public String mode = "black";
    @OptionValues(value = {"bilibili"})
    public String platform = "bilibili";

    @OptionValues(value = {"\"%GREEN%[弹幕]%RED%[{{roomOwner}}]%GOLD%{#[舰长#{fanGuard=3}#]#}%dark_purple%{#[#{fanName}#{{fanLevel}}]#}%GOLD%[UL{{uLevel}}]%WHITE%<{{danmuAuthur}}>:{{danmuMsg}}\""})
    public String chat_dm_style = "%GREEN%[弹幕]%RED%[{{roomOwner}}]%GOLD%{#[舰长#{fanGuard=3}#]#}%dark_purple%{#[#{fanName}#{{fanLevel}}]#}%GOLD%[UL{{uLevel}}]%WHITE%<{{danmuAuthur}}>:{{danmuMsg}}";
    @OptionValues(value = {"\"%BLUE%[礼物]%RED%[{{roomOwner}}]%GOLD%{#[舰长#{fanGuard=3}#]#}%dark_purple%{#[#{fanName}#{{fanLevel}}]#}%WHITE%{{danmuAuthur}} {{actionName}}了%GOLD%{{num}}%WHITE%个%LIGHT_PURPLE%{{giftName}}\""})
    public String gift_dm_style = "%BLUE%[礼物]%RED%[{{roomOwner}}]%GOLD%{#[舰长#{fanGuard=3}#]#}%dark_purple%{#[#{fanName}#{{fanLevel}}]#}%WHITE%{{danmuAuthur}} {{actionName}}了%GOLD%{{num}}%WHITE%个%LIGHT_PURPLE%{{giftName}}";
    @OptionValues(value = {"\"%GREEN%[提示]%RED%[{{roomOwner}}]%GOLD%{#[舰长#{fanGuard=3}#]#}%dark_purple%{#[#{fanName}#{{fanLevel}}]#}%WHITE%{{danmuAuthur}} 加入了房间\""})
    public String welcome_dm_style = "%GREEN%[提示]%RED%[{{roomOwner}}]%GOLD%{#[舰长#{fanGuard=3}#]#}%dark_purple%{#[#{fanName}#{{fanLevel}}]#}%WHITE%{{danmuAuthur}} 加入了房间";
    @OptionValues(value = {"\"%GREEN%[提示]%RED%[{{roomOwner}}]%GOLD%{#[舰长#{fanGuard=3}#]#}%dark_purple%{#[#{fanName}#{{fanLevel}}]#}%WHITE%{{danmuAuthur}} 关注了你\""})
    public String subscribe_dm_style = "%GREEN%[提示]%RED%[{{roomOwner}}]%GOLD%{#[舰长#{fanGuard=3}#]#}%dark_purple%{#[#{fanName}#{{fanLevel}}]#}%WHITE%{{danmuAuthur}} 关注了你";

    @OptionValues(value = {"true", "false"})
    public boolean enable_chat_dm = true;
    @OptionValues(value = {"true", "false"})
    public boolean enable_gift_dm = true;
    @OptionValues(value = {"true", "false"})
    public boolean enable_welcome_dm = false;
    @OptionValues(value = {"true", "false"})
    public boolean enable_subscribe_dm = false;

    @HidedConfigProperty
    public ArrayList<String> addedRooms = new ArrayList<>();

    @HidedConfigProperty
    public HashMap<String, Config> roomConfigs = new HashMap<>();

    @HidedConfigProperty
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
        auto_reconnect = c.auto_reconnect;
        auto_reconnect_delay = c.auto_reconnect_delay;

        black_dm = getStringNotNull(c.black_dm);
        white_dm = getStringNotNull(c.white_dm);
        mode = getStringNotNull(c.mode);
        platform = getStringNotNull(c.platform);

        chat_dm_style = getStringNotNull(c.chat_dm_style);
        gift_dm_style = getStringNotNull(c.gift_dm_style);
        welcome_dm_style = getStringNotNull(c.welcome_dm_style);
        subscribe_dm_style = getStringNotNull(c.subscribe_dm_style);

        if(all)
        {
            addedRooms = (ArrayList<String>) c.addedRooms.clone();
            roomConfigs = (HashMap<String, Config>) c.roomConfigs.clone();
            customKeys = (HashMap<String, String>) c.customKeys.clone();
        }
    }

    public List<String> getProperties(){
        List<String> res = new LinkedList<>();
        for(Field f:getClass().getFields()){
            if(!f.isAnnotationPresent(HidedConfigProperty.class)){
                res.add(f.getName());
            }
        }
        res.addAll(customKeys.keySet());
        return res;
    }

    public List<String> getPropertyOptionValues(String fieldName){
        List<String> res = new LinkedList<>();
        res.add("<No Option>");
        try {
            Field field = getClass().getField(fieldName);
            if(field.isAnnotationPresent(OptionValues.class)){
                String[] values = field.getAnnotation(OptionValues.class).value();
                return Arrays.asList(values);
            }
        }catch (NoSuchFieldException | SecurityException e){
            return res;
        }
        return res;
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
        }else if(key.equals("welcome_dm_style"))
        {
            welcome_dm_style = value;
        }else if(key.equals("subscribe_dm_style"))
        {
            subscribe_dm_style = value;
        }else if(key.equals("auto_reconnect"))
        {
            auto_reconnect = Boolean.parseBoolean(value);
        }else if(key.equals("auto_reconnect_delay"))
        {
            auto_reconnect_delay = Long.parseLong(value);
        }else if(key.equals("enable_chat_dm"))
        {
            enable_chat_dm = Boolean.parseBoolean(value);
        }else if(key.equals("enable_gift_dm"))
        {
            enable_gift_dm = Boolean.parseBoolean(value);
        }else if(key.equals("enable_welcome_dm"))
        {
            enable_welcome_dm = Boolean.parseBoolean(value);
        }else if(key.equals("enable_subscribe_dm"))
        {
            enable_subscribe_dm = Boolean.parseBoolean(value);
        }
        else
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
        }else if(key.equals("welcome_dm_style"))
        {
            return getStringNotNull(welcome_dm_style);
        }else if(key.equals("subscribe_dm_style"))
        {
            return getStringNotNull(subscribe_dm_style);
        }else if(key.equals("auto_reconnect"))
        {
            return "" + auto_reconnect;
        }else if(key.equals("auto_reconnect_delay"))
        {
            return "" + auto_reconnect_delay;
        }else if(key.equals("enable_chat_dm"))
        {
            return "" + enable_chat_dm;
        }else if(key.equals("enable_gift_dm"))
        {
            return "" + enable_gift_dm;
        }else if(key.equals("enable_welcome_dm"))
        {
            return "" + enable_welcome_dm;
        }else if(key.equals("enable_subscribe_dm"))
        {
            return "" + enable_subscribe_dm;
        }
        else
        {
            return  getStringNotNull(customKeys.get(key));
        }
    }
}
