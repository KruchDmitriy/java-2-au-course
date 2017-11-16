package ru.spbau.mit.simpleftp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.activeCount;

public class Server {
    public static final int DEFAULT_PORT = 78905;
    private final ServerSocket serverSocket;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(activeCount());
    private Thread mainThread;

    public Server() throws IOException {
        this(DEFAULT_PORT);
    }

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void start() throws ServerAlreadyStarted {
        if (mainThread.isAlive()) {
            throw new ServerAlreadyStarted();
        }

        mainThread = new Thread(() -> {
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    executorService.submit(new Task(clientSocket));
                } catch (IOException ignored) {
                    return;
                }
            }
        });
        mainThread.start();
    }

    public void stop() throws ServerAlreadyStopped {
        if (!mainThread.isAlive()) {
            throw new ServerAlreadyStopped();
        }

        try {
            serverSocket.close();
        } catch (IOException ignored) { }
    }

    private static void get(DataOutputStream outputStream, String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(fileName));
        outputStream.writeUTF(Arrays.toString(bytes));
    }

    private static void list(DataOutputStream outputStream, String fileName) throws IOException {
        final IOException[] wasThrown = { null };
        String[] fileNames = new File(fileName).list();

        if (fileNames == null) {
            outputStream.writeInt(0);
            return;
        }

        outputStream.writeInt(fileNames.length);
        Arrays.stream(fileNames)
            .forEach(file -> {
                try {
                    outputStream.writeUTF(file);
                    outputStream.writeBoolean(new File(file).isDirectory());
                } catch (IOException e) {
                    wasThrown[0] = e;
                }
            });

        if (wasThrown[0] != null) {
            throw wasThrown[0];
        }
    }

    private class Task implements Runnable {
        private final Socket socket;

        Task(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream inputStream = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));
                 DataOutputStream outputStream = new DataOutputStream(
                         new BufferedOutputStream(socket.getOutputStream())))
            {
                while (true) {
                    switch (Command.createCommand(inputStream.readInt())) {
                        case Get:
                            get(outputStream, inputStream.readUTF());
                            break;
                        case List:
                            list(outputStream, inputStream.readUTF());
                            break;
                        case Shutdown:
                            return;
                    }
                }
            } catch (IOException ignored) { }
        }
    }
}
