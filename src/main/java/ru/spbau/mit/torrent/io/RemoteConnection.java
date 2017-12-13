package ru.spbau.mit.torrent.io;

import ru.spbau.mit.torrent.io.query.Query;
import ru.spbau.mit.torrent.io.response.ErrorResponse;
import ru.spbau.mit.torrent.io.response.Response;
import ru.spbau.mit.torrent.util.Source;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class RemoteConnection implements AutoCloseable {
    private final Socket socket;
    private ObjectInputStream in;
    private final ObjectOutputStream out;

    public RemoteConnection(Source source) throws IOException {
        socket = source.createSocket();
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void send(Query query) throws IOException {
        out.writeObject(query);
        out.flush();
    }

    public <T extends Response> T receive(Class<T> cls)
            throws IOException, ClassNotFoundException {
        Response response = (Response) in.readObject();
        handleErrors(response);
        return cls.cast(response);
    }

    private void handleErrors(Response response) throws IOException {
        if (response instanceof ErrorResponse) {
            ErrorResponse errorResponse = (ErrorResponse) response;
            throw new IOException(errorResponse.message, errorResponse.cause);
        }
    }

    @Override
    public void close() throws IOException {
        send(new CloseQuery());
        socket.close();
    }
}
