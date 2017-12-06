package ru.spbau.mit.simpleftp;

import ru.spbau.mit.simpleftp.util.*;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.lang.Thread.activeCount;

public class Server {
    private static final FTPConfig CONFIG = new FTPConfig();
    private final ServerSocket serverSocket;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(activeCount());
    private Thread mainThread;
    private final AtomicBoolean stopServer = new AtomicBoolean();

    public Server() throws IOException {
        this(CONFIG.getPort());
    }

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void start() throws ServerAlreadyStarted {
        if (mainThread != null && mainThread.isAlive()) {
            throw new ServerAlreadyStarted();
        }

        mainThread = new Thread(() -> {
            while (!stopServer.get()) {
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

    public void stop() throws ServerAlreadyStopped {
        if (!mainThread.isAlive()) {
            throw new ServerAlreadyStopped();
        }

        if (!serverSocket.isClosed()) {
            try {
                stopServer.set(true);
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Response processQuery(Query query) throws IOException {
        if (query instanceof ListQuery) {
            return list((ListQuery) query);
        }

        if (query instanceof GetQuery) {
            return get((GetQuery) query);
        }

        return new ErrorResponse("Server error: Unknown query type");
    }

    private static GetResponse get(GetQuery query) throws IOException {
        return new GetResponse(new File(query.pathToFile), query.destinationPath);
    }

    private static ListResponse list(ListQuery query) throws IOException {
        List<ListResponse.DirectoryItem> directoryItems =
                Files.walk(Paths.get(query.pathToDirectory), 1)
                .map(path -> new ListResponse.DirectoryItem(
                        path.toAbsolutePath().normalize().toString(),
                        Files.isDirectory(path)
                ))
                .collect(Collectors.toList());
        return new ListResponse(directoryItems);
    }

    public boolean isStopped() {
        return mainThread == null || !mainThread.isAlive();
    }

    private class Task implements Runnable {
        private final Socket socket;

        Task(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                while (true) {
                    try {
                        Query query = (Query) in.readObject();

                        if (query instanceof CloseQuery) {
                            return;
                        }

                        Response response = processQuery(query);
                        out.writeObject(response);
                    } catch (IOException e) {
                        out.writeObject(new ErrorResponse("Error while processing query"));
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
