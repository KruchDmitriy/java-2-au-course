package ru.spbau.mit.simpleftp.util;

public class ErrorResponse implements Response {
    public final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }
}
