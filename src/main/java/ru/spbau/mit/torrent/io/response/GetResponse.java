package ru.spbau.mit.torrent.io.response;

public class GetResponse implements Response {
    final public int size;
    final public byte[] data;

    public GetResponse(int size, byte[] data) {
        this.size = size;
        this.data = data;
    }
}
