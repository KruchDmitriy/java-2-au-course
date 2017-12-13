package ru.spbau.mit.torrent.io.cli.commands;

public class SourceCommand extends Command {
    public final int fileId;

    public SourceCommand(int fileId) {
        super(CommandType.SOURCES);
        this.fileId = fileId;
    }
}
