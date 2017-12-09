package ru.spbau.mit.torrent.util;

import java.io.IOException;

public class UnavailablePartException extends IOException {
    public UnavailablePartException(String message) {
        super(message);
    }
}
