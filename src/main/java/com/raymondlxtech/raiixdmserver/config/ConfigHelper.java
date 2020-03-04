package com.raymondlxtech.raiixdmserver.config;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.raymondlxtech.raiixdmserver.RaiixDMServer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class ConfigHelper  {
    public static final String MODID = "raiixdmserver";

    private Config theConfig;
    private RaiixDMServer theMod;

    public ConfigHelper(RaiixDMServer m)
    {
        theMod = m;
        theConfig = new Config();
    }

    public Config getConfig(){
        return theConfig;
    }

    public void saveConfig(){
        theMod.theLogger.info("Saving config file...");
        File configFile = new File(FabricLoader.getInstance().getConfigDirectory(), MODID + ".json");
        try {
            if(!configFile.exists()) {
                configFile.getParentFile().mkdir();
            }
            Files.write(configFile.toPath(), new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(theConfig).getBytes("utf-8"), StandardOpenOption.CREATE);
        } catch (Exception e)
        {
            theMod.theLogger.error("error happened while saving config.");
            e.printStackTrace();
        }
    }

    public void loadConfig(){
        theMod.theLogger.info("Loading config file...");
        File configFile = new File(FabricLoader.getInstance().getConfigDirectory(), MODID + ".json");
        try {
            if(configFile.exists())
            {
                JsonReader jr = new JsonReader(new InputStreamReader(new FileInputStream(configFile), "utf-8"));
                jr.setLenient(true);
                Config c = new Gson().fromJson(jr, Config.class);
                theConfig.copyFrom(c);
            } else {
                configFile.getParentFile().mkdir();
                Files.write(configFile.toPath(), new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(theConfig).getBytes("utf-8"), StandardOpenOption.CREATE_NEW);
            }
        }catch (Exception e)
        {
            theMod.theLogger.error(" error happened while loading config");
            e.printStackTrace();
        }
    }

}

