package ru.spbau.mit.torrent.io.query;

public class SourcesQuery extends Query {
    public final int fileId;

    public SourcesQuery(int fileId) {
        super(QueryId.SOURCES);
        this.fileId = fileId;
    }
}
