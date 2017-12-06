package ru.spbau.mit.simpleftp.util;

public class ListQuery implements Query {
    public final String pathToDirectory;

    public ListQuery(String pathToDirectory) {
        this.pathToDirectory = pathToDirectory;
    }
}
