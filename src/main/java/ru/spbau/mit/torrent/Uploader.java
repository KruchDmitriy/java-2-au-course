package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.io.query.GetQuery;
import ru.spbau.mit.torrent.io.query.Query;
import ru.spbau.mit.torrent.io.query.StatQuery;
import ru.spbau.mit.torrent.io.response.ErrorResponse;
import ru.spbau.mit.torrent.io.response.Response;
import ru.spbau.mit.torrent.io.response.StatResponse;
import ru.spbau.mit.torrent.util.PartedFile;
import ru.spbau.mit.torrent.util.ServerTask;
import ru.spbau.mit.torrent.util.Source;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

public class Uploader implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final FileManager fileManager;

    public Uploader(int port, ExecutorService executorService,
                    FileManager fileManager)
            throws IOException {
        serverSocket = new ServerSocket(port);
        this.executorService = executorService;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(
                        new ServerTask(clientSocket, this::processQuery)
                );
            } catch (IOException e) {
                if (e instanceof SocketException
                        && e.getMessage().equals("Socket closed")) {
                    return;
                }
                e.printStackTrace();
            }
        }
    }

    private Response processQuery(Query query, Source sender) {
        final PartedFile file;
        switch (query.id) {
            case STAT:
                StatQuery statQuery = (StatQuery) query;
                file = fileManager.getFile(statQuery.fileId);
                return new StatResponse(file.getAvailableParts());
            case GET:
                GetQuery getQuery = (GetQuery) query;
                file = fileManager.getFile(getQuery.fileId);
                return file.sendPart(getQuery.partId);
            default:
                return new ErrorResponse("Unknown query");
        }
    }
}
