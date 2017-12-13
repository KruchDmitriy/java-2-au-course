package ru.spbau.mit.torrent.io.query;

import java.io.*;

public abstract class Query implements Serializable {
    public final QueryId id;

    public Query(QueryId id) {
        this.id = id;
    }

    public enum QueryId implements Serializable {
        LIST,
        UPLOAD,
        SOURCES,
        UPDATE,
        STAT,
        GET,
        CLOSE,
        TEST
    }
}