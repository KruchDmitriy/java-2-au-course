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
    public static final int DEFAULT_PORT = 57890;
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
        if (mainThread != null && mainThread.isAlive()) {
            throw new ServerAlreadyStarted();
        }

        mainThread = new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
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
        final File file = new File(fileName);
        outputStream.writeLong(file.length());

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[256];
            int readed;
            while ((readed = fileInputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, readed);
            }
        }
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
            .forEach(curFileName -> {
                try {
                    File file = new File(fileName + File.separator + curFileName);
                    outputStream.writeUTF(file.getCanonicalPath());
                    outputStream.writeBoolean(file.isDirectory());
                } catch (IOException e) {
                    wasThrown[0] = e;
                }
            });

        if (wasThrown[0] != null) {
            throw wasThrown[0];
        }
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
            try (DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                 DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream()))
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
