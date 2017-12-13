package ru.spbau.mit.torrent.util;

import java.io.Serializable;

public class FileInfo implements Serializable {
    private final int id;
    private final String name;
    private final long size;

    public FileInfo(String serializedFileInfo) {
        String[] args = serializedFileInfo.split(" ");
        this.id = Integer.parseInt(args[0]);
        this.name = args[1];
        this.size = Long.parseLong(args[2]);
    }

    public FileInfo(int id, String name, long size) {
        this.id = id;
        this.name = name;
        this.size = size;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String serialize() {
        return id + " " + name + " " + size;
    }

    @Override
    public String toString() {
        return String.format("[%4d] %20s %10d", id, name, size);
    }
}