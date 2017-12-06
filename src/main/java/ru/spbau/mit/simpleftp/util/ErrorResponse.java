package ru.spbau.mit.simpleftp.util;

public class ErrorResponse implements Response {
    public final String message;
    public final Throwable cause;

    public ErrorResponse(String message) {
        this.message = message;
        this.cause = null;
    }

    public ErrorResponse(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }
}
