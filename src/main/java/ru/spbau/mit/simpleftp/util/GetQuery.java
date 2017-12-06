package ru.spbau.mit.simpleftp.util;

public class GetQuery implements Query {
    public final String pathToFile;
    public final String destinationPath;

    public GetQuery(String pathToFile, String destinationPath) {
        this.pathToFile = pathToFile;
        this.destinationPath = destinationPath;
    }
}
