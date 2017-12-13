package ru.spbau.mit.torrent.io.response;

public class UpdateResponse implements Response {
    public final boolean status;

    public UpdateResponse(boolean status) {
        this.status = status;
    }
}
