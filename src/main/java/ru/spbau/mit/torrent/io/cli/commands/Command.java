package ru.spbau.mit.torrent.io.cli.commands;

public abstract class Command {
    public final CommandType commandType;

    public Command(CommandType commandType) {
        this.commandType = commandType;
    }

    public static Command readCommand(String line) throws UnknownCommandException {
        String[] words = line.split("\\s+");

        switch (words[0]) {
            case "list":
                return new ListCommand();
            case "get":
                return new GetCommand();
            case "upload":
                break;
            default:
                throw new UnknownCommandException();
        }

        return null;
    }

    public enum CommandType {
        LIST,
        GET,
        UPLOAD
    }
}
