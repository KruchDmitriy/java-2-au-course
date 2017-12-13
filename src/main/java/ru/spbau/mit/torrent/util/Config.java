package ru.spbau.mit.torrent.util;

public class Config {
    public static final String TRACKER_IP = "127.0.0.1";
    public static final int TRACKER_PORT = 8081;
    public static final Source TRACKER_SOURCE
            = new Source(new Source.IP(Config.TRACKER_IP), Config.TRACKER_PORT);
    public static final int DEFAULT_CLIENT_PORT = 8082;
    public static final int SOURCE_REFRESH_MINUTES = 5;
    public static final int CLIENT_UPDATE_MILLIS = 4 * 60 * 1000;
    public static final int STD_PART_SIZE = 8 * 1024 * 1024;
    public static final int CLIENT_NUM_THREADS = 20;
    public static final int NUM_SOURCES_PER_DOWNLOAD = 4;
}
