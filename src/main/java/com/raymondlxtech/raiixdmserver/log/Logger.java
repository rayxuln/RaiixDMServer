package com.raymondlxtech.raiixdmserver.log;

public class Logger {

    String name;

    public Logger(String n)
    {
        name = n;
    }

    public void info(String msg)
    {
        System.out.printf("[%s]%s\n", name, msg);
    }
    public void warning(String msg)
    {
        System.out.printf("[%s *warning*]%s\n", name, msg);
    }
    public void error(String msg)
    {
        System.out.printf("[%s *error*]%s\n", name, msg);
    }

}
