package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.io.cli.commands.Command;
import ru.spbau.mit.torrent.io.query.UpdateQuery;
import ru.spbau.mit.torrent.util.Config;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.activeCount;
import static java.lang.Thread.sleep;

public class Client {
    private final ServerSocket seedSocket;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(activeCount());
    private final Thread updateThread;
    private final short port;
    private volatile boolean stopUpdate = false;
    private final FileManager fileManager;

    public Client(String pathToListFiles) throws IOException, ClassNotFoundException {
        this(Config.DEFAULT_CLIENT_PORT, pathToListFiles);
    }

    public Client(short port, String pathToListFiles) throws IOException, ClassNotFoundException {
        this.port = port;
        fileManager = new FileManager(pathToListFiles);
        seedSocket = new ServerSocket(port);
        updateThread = new Thread(this::update);
    }

    private void update() {
        while (!stopUpdate) {
            try (Socket socket = new Socket(Config.TRACKER_IP, Config.TRACKER_PORT);
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()))
            {
                out.writeObject(new UpdateQuery(port, fileManager.getUploaded()));

                if (!in.readBoolean()) {
                    // TODO: Exception?
                    System.err.println("Failed to update information on server");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                sleep(Config.CLIENT_UPDATE_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void mainCycle() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            Command command = Command.readCommand(scanner.nextLine());

            switch (command.)
        }
    }
}