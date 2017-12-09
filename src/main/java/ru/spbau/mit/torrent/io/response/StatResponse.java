package ru.spbau.mit.torrent.io.response;

import java.util.Set;

public class StatResponse implements Response {
    private Set<Integer> availableParts;

    public StatResponse(Set<Integer> availableParts) {
        this.availableParts = availableParts;
    }
}
