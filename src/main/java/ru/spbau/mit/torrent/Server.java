package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.io.query.Query;
import ru.spbau.mit.torrent.io.query.SourcesQuery;
import ru.spbau.mit.torrent.io.query.UpdateQuery;
import ru.spbau.mit.torrent.io.query.UploadQuery;
import ru.spbau.mit.torrent.io.response.*;
import ru.spbau.mit.torrent.util.Config;
import ru.spbau.mit.torrent.util.ServerTask;
import ru.spbau.mit.torrent.util.Source;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.activeCount;

public class Server {
    private static String DEFAULT_STATE_FILE_PATH = "tracker.state";
    private final ServerSocket serverSocket =
            new ServerSocket(Config.TRACKER_PORT);
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(activeCount());
    private final FileTracker fileTracker;

    public Server() throws IOException {
        this(DEFAULT_STATE_FILE_PATH);
    }

    public Server(String pathToState) throws IOException {
        fileTracker = new FileTracker(pathToState);
        Thread mainThread = new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(
                            new ServerTask(clientSocket, this::processQuery)
                    );
                } catch (IOException e) {
                    if (e instanceof SocketException
                            && e.getMessage().equals("Socket closed")) {
                        executorService.shutdown();
                        return;
                    }
                    e.printStackTrace();
                } finally {
                    fileTracker.saveState();
                }
            }
        });
        mainThread.start();
    }

    private Response processQuery(Query query, Source sender) {
        switch (query.id) {
            case LIST:
                return fileTracker.getListResponse();
            case UPLOAD:
                final UploadQuery uploadQuery = (UploadQuery) query;
                final int fileId = fileTracker.addFile(uploadQuery.name, uploadQuery.size);
                return new UploadResponse(fileId);
            case SOURCES:
                final SourcesQuery sourcesQuery = (SourcesQuery) query;
                final List<Source> listSources = fileTracker.getListSources(sourcesQuery.fileId);
                return new SourcesResponse(listSources);
            case UPDATE:
                final UpdateQuery updateQuery = (UpdateQuery) query;
                boolean status = fileTracker.updateSource(
                        new Source(sender.ip, updateQuery.port),
                        updateQuery.fileIds);
                return new UpdateResponse(status);
            default:
                return new ErrorResponse("Unknown query");
        }
    }
}