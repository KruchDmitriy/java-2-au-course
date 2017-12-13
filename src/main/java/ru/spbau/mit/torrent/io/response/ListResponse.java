package ru.spbau.mit.torrent.io.response;

import ru.spbau.mit.torrent.util.FileInfo;

import java.util.List;

public class ListResponse implements Response {
    public final List<FileInfo> filesInfo;

    public ListResponse(List<FileInfo> filesInfo) {
        this.filesInfo = filesInfo;
    }
}
