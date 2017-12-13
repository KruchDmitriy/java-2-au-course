package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.io.RemoteConnection;
import ru.spbau.mit.torrent.io.cli.commands.*;
import ru.spbau.mit.torrent.io.query.ListQuery;
import ru.spbau.mit.torrent.io.query.SourcesQuery;
import ru.spbau.mit.torrent.io.query.UploadQuery;
import ru.spbau.mit.torrent.io.response.ListResponse;
import ru.spbau.mit.torrent.io.response.SourcesResponse;
import ru.spbau.mit.torrent.io.response.UploadResponse;
import ru.spbau.mit.torrent.util.Config;
import ru.spbau.mit.torrent.util.FileInfo;
import ru.spbau.mit.torrent.util.PartedFile;
import ru.spbau.mit.torrent.util.Source;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.spbau.mit.torrent.util.Config.TRACKER_SOURCE;

public class Client {
    private final FileManager fileManager;
    private final Downloader downloader;
    private final Updater updater;
    private final int port;

    public Client(String pathToListFiles)
            throws IOException, ClassNotFoundException {
        this(Config.DEFAULT_CLIENT_PORT, pathToListFiles);
    }

    public Client(int port, String pathToListFiles)
            throws IOException, ClassNotFoundException {
        this.port = port;
        fileManager = new FileManager(pathToListFiles);

        updater = new Updater(port, fileManager);
        Thread updaterThread = new Thread(updater);
        updaterThread.setDaemon(true);
        updaterThread.start();

        ExecutorService executorService =
                Executors.newFixedThreadPool(Config.CLIENT_NUM_THREADS);
        downloader = new Downloader(executorService, fileManager);

        Thread uploader = new Thread(
            new Uploader(port, executorService, fileManager)
        );
        uploader.setDaemon(true);
        uploader.start();
    }

    public int getPort() {
        return port;
    }

    public static List<FileInfo> list()
            throws ClassNotFoundException, IOException {
        try (RemoteConnection connection = new RemoteConnection(TRACKER_SOURCE)) {
            connection.send(new ListQuery());
            ListResponse response = connection.receive(ListResponse.class);
            return response.filesInfo;
        }
    }

    public int upload(Path filePath, long size)
            throws IOException, ClassNotFoundException {
        int fileId;
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("file "
                    + filePath.getFileName().toString()
                    + " not found");
        }

        try (RemoteConnection connection = new RemoteConnection(TRACKER_SOURCE)) {
            String path = filePath.toAbsolutePath().normalize().toString();
            String fileName = filePath.getFileName().toString();

            connection.send(new UploadQuery(fileName, size));
            UploadResponse response = connection.receive(UploadResponse.class);

            fileManager.addFile(new PartedFile(response.fileId, path));
            fileId = response.fileId;
        }

        updater.updateNow();

        return fileId;
    }

    public static List<Source> sources(int fileId)
            throws IOException, ClassNotFoundException {
        try (RemoteConnection connection = new RemoteConnection(TRACKER_SOURCE)) {
            connection.send(new SourcesQuery(fileId));
            SourcesResponse response = connection.receive(SourcesResponse.class);
            return response.listSources;
        }
    }

    public void get(int fileId, String destinationPath, long size)
            throws IOException, ClassNotFoundException {
        downloader.downloadFile(fileId, destinationPath, size);
    }

    public void CLICycle() throws IOException {
        Scanner scanner = new Scanner(System.in);
        final HashMap<Integer, FileInfo> fileInfoList = new HashMap<>();
        while (true) {
            try {
                Command command = Command.readCommand(scanner.nextLine());

                switch (command.commandType) {
                    case LIST:
                        List<FileInfo> list = list();
                        System.out.println(list);

                        list.forEach(fileInfo -> fileInfoList.put(fileInfo.getId(), fileInfo));
                        break;
                    case SOURCES:
                        SourceCommand sourceCommand = (SourceCommand) command;
                        System.out.println(sources(sourceCommand.fileId));
                        break;
                    case GET:
                        GetCommand getCommand = (GetCommand) command;
                        get(getCommand.id,
                                getCommand.destinationPath,
                                fileInfoList.get(getCommand.id).getSize());
                        break;
                    case UPLOAD:
                        UploadCommand uploadCommand = (UploadCommand) command;
                        Path path = Paths.get(uploadCommand.filePath);
                        int fileId = upload(path, path.toFile().length());
                        System.out.println("Upload successful, file fileId = " + fileId);
                        break;
                    case HELP:
                        printHelp();
                        break;
                    case EXIT:
                        System.out.println("Bye!");
                        return;
                }
            } catch (UnknownCommandException e) {
                System.out.println("Unknown command");
                printHelp();
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                printHelp();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                fileManager.close();
            }
        }
    }

    private void printHelp() {
        System.out.println("This is help message. Usage:\n" +
                "1. list -> get list files on tracker\n" +
                "2. get fileId destinationPath -> download file with id\n" +
                "\t fileId to path destinationPath\n" +
                "3. sources fileId -> get peers of file with id fileId\n" +
                "4. upload filePath -> upload file from filePath\n" +
                "\t with size fileSize on server\n" +
                "5. exit\n");
    }
}