package ru.spbau.mit.torrent.io.query;

import java.util.List;

public class UpdateQuery extends Query {
    public final int port;
    public final List<Integer> fileIds;

    public UpdateQuery(int port, List<Integer> fileIds) {
        super(QueryId.UPDATE);
        this.port = port;
        this.fileIds = fileIds;
    }
}
