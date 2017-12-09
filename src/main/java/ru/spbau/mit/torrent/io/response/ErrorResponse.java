package ru.spbau.mit.torrent.io.response;

import java.io.IOException;

public class ErrorResponse implements Response {
    public final String message;
    public IOException cause;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public ErrorResponse(String message, IOException cause) {
        this.message = message;
        this.cause = cause;
    }
}
