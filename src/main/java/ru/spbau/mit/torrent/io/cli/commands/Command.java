package ru.spbau.mit.torrent.io.cli.commands;

public abstract class Command {
    public enum CommandType {
        LIST,
        GET,
        UPLOAD,
        HELP,
        SOURCES,
        EXIT
    }

    public final CommandType commandType;

    public Command(CommandType commandType) {
        this.commandType = commandType;
    }

    public static Command readCommand(String line)
            throws UnknownCommandException, IllegalArgumentException {
        String[] words = line.split("\\s+");

        switch (words[0]) {
            case "list":
                return new ListCommand();
            case "sources":
                return new SourceCommand(Integer.parseInt(words[1]));
            case "get":
                if (words.length < 3) {
                    throw new IllegalArgumentException(
                            "Too few arguments for get");
                }
                return new GetCommand(Integer.parseInt(words[1]), words[2]);
            case "upload":
                if (words.length < 2) {
                    throw new IllegalArgumentException(
                            "Too few arguments for upload");
                }
                return new UploadCommand(words[1]);
            case "help":
                return new HelpCommand();
            case "exit":
                return new ExitCommand();
            default:
                throw new UnknownCommandException();
        }
    }
}
