package ru.spbau.mit.torrent.io.cli.commands;

public class GetCommand extends Command {
    public final int id;
    public final String destinationPath;

    public GetCommand(int id, String destinationPath) {
        super(CommandType.GET);
        this.id = id;
        this.destinationPath = destinationPath;
    }
}
