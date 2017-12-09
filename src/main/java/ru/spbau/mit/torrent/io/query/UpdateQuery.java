package ru.spbau.mit.torrent.io.query;

import java.util.List;

public class UpdateQuery extends Query {
    public final short port;
    public final List<Integer> fileIds;

    public UpdateQuery(short port, List<Integer> fileIds) {
        super(QueryId.UPDATE);
        this.port = port;
        this.fileIds = fileIds;
    }
}
