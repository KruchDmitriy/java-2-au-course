package ru.spbau.mit.torrent.io.response;

import ru.spbau.mit.torrent.util.Source;

import java.util.List;

public class SourcesResponse implements Response {
    public final List<Source> listSoucres;

    public SourcesResponse(List<Source> listSources) {
        this.listSoucres = listSources;
    }
}
