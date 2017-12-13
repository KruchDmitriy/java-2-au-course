package ru.spbau.mit.torrent.io.response;

import java.util.List;

public class StatResponse implements Response {
    public final List<Integer> availableParts;

    public StatResponse(List<Integer> availableParts) {
        this.availableParts = availableParts;
    }
}
