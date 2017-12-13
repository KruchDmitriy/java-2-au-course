package ru.spbau.mit.torrent.io;

import ru.spbau.mit.torrent.io.query.Query;

public class CloseQuery extends Query {
    public CloseQuery() {
        super(QueryId.CLOSE);
    }
}
