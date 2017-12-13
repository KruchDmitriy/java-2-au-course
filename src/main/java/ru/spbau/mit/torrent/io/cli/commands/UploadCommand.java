package ru.spbau.mit.torrent.io.cli.commands;

public class UploadCommand extends Command {
    public final String filePath;

    public UploadCommand(String filePath) {
        super(CommandType.UPLOAD);
        this.filePath = filePath;
    }
}
