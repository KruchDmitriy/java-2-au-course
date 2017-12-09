package ru.spbau.mit.torrent.io.response;

public class UploadResponse implements Response {
    public final int fileId;

    public UploadResponse(int fileId) {
        this.fileId = fileId;
    }
}
