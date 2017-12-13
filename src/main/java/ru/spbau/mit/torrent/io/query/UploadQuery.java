package ru.spbau.mit.torrent.io.query;

public class UploadQuery extends Query {
    public final String name;
    public final long size;

    public UploadQuery(String name, long size) {
        super(QueryId.UPLOAD);
        this.name = name;
        this.size = size;
    }
}
