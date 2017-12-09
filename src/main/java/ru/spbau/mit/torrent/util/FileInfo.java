package ru.spbau.mit.torrent.util;

public class FileInfo {
    private int id;
    private String name;
    private long size;

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
}