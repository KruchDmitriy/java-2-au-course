package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.io.query.Query;
import ru.spbau.mit.torrent.io.query.SourcesQuery;
import ru.spbau.mit.torrent.io.query.UpdateQuery;
import ru.spbau.mit.torrent.io.query.UploadQuery;
import ru.spbau.mit.torrent.io.response.*;
import ru.spbau.mit.torrent.util.Config;
import ru.spbau.mit.torrent.util.Source;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.activeCount;

public class Server {
    private final ServerSocket serverSocket =
            new ServerSocket(Config.TRACKER_PORT);
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(activeCount());
    private final FileTracker fileTracker = new FileTracker();

    public Server() throws IOException {
        Thread mainThread = new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(new Task(clientSocket));
                } catch (IOException e) {
                    if (e instanceof SocketException
                            && e.getMessage().equals("Socket closed")) {
                        return;
                    }
                    e.printStackTrace();
                }
            }
        });
        mainThread.start();
    }

    private class Task implements Runnable {
        private final Socket socket;

        Task(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                try {
                    Query query = (Query) in.readObject();

                    Response response = processQuery(query);
                    out.writeObject(response);
                } catch (IOException e) {
                    out.writeObject(new ErrorResponse("Error while processing query", e));
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }

        private Response processQuery(Query query) {
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
                    final Source source = new Source(
                            new Source.IP(socket.getInetAddress().getAddress()),
                            updateQuery.port
                    );
                    boolean status = fileTracker.updateSource(source, updateQuery.fileIds);
                    return new UpdateResponse(status);
                default:
                    return new ErrorResponse("Unknown query");
            }
        }
    }
}