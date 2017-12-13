package ru.spbau.mit.torrent.io.query;

public class GetQuery extends Query {
    public final int fileId;
    public final int partId;

    public GetQuery(int fileId, int partId) {
        super(QueryId.GET);
        this.fileId = fileId;
        this.partId = partId;
    }
}
