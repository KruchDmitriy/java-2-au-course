package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.io.RemoteConnection;
import ru.spbau.mit.torrent.io.query.UpdateQuery;
import ru.spbau.mit.torrent.io.response.UpdateResponse;
import ru.spbau.mit.torrent.util.Config;

import java.io.IOException;

import static java.lang.Thread.sleep;
import static ru.spbau.mit.torrent.util.Config.TRACKER_SOURCE;

public class Updater implements Runnable {
    private final int port;
    private final FileManager fileManager;

    public Updater(int port, FileManager fileManager) throws IOException {
        this.port = port;
        this.fileManager = fileManager;
    }

    public void updateNow() throws IOException, ClassNotFoundException {
        try (RemoteConnection connection = new RemoteConnection(TRACKER_SOURCE)) {
            connection.send(new UpdateQuery(port, fileManager.getUploaded()));
            UpdateResponse response = connection.receive(UpdateResponse.class);
            if (!response.status) {
                System.err.println("Updater: Failed to update information on server");
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try (RemoteConnection connection = new RemoteConnection(TRACKER_SOURCE)) {
                connection.send(new UpdateQuery(port, fileManager.getUploaded()));
                UpdateResponse response = connection.receive(UpdateResponse.class);
                if (!response.status) {
                    System.err.println("Updater: Failed to update information on server");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            try {
                sleep(Config.CLIENT_UPDATE_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
