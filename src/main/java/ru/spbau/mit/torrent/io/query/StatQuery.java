package ru.spbau.mit.torrent.io.query;

public class StatQuery extends Query {
    public final int fileId;

    public StatQuery(int fileId) {
        super(QueryId.STAT);
        this.fileId = fileId;
    }
}
