package ru.spbau.mit.torrent.util;

import ru.spbau.mit.torrent.io.CloseQuery;
import ru.spbau.mit.torrent.io.query.Query;
import ru.spbau.mit.torrent.io.response.ErrorResponse;
import ru.spbau.mit.torrent.io.response.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.BiFunction;

public class ServerTask implements Runnable {
    private final Socket socket;
    private final Source source;
    private final BiFunction<Query, Source, Response> processQuery;

    public ServerTask(Socket socket, BiFunction<Query, Source, Response> processQuery) {
        this.socket = socket;
        this.processQuery = processQuery;
        source = new Source(
            new Source.IP(socket.getInetAddress().getAddress()),
            (short) socket.getPort()
        );
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (true) {
                try {
                    Query query = (Query) in.readObject();

                    if (query instanceof CloseQuery) {
                        return;
                    }

                    Response response = processQuery.apply(query, source);

                    out.writeObject(response);
                } catch (IOException e) {
                    out.writeObject(new ErrorResponse("Error while processing query", e));
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }
}
