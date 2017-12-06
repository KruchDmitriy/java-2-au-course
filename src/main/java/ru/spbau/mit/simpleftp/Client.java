package ru.spbau.mit.simpleftp;

import ru.spbau.mit.simpleftp.util.*;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class Client implements AutoCloseable {
    private static final FTPConfig CONFIG = new FTPConfig();
    private final Socket socket;
    private final ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;

    public Client() throws IOException {
        this(CONFIG.getAddress(), CONFIG.getPort());
    }

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public List<ListResponse.DirectoryItem> executeList(String pathToDirectory)
            throws Exception {
        Query query = new ListQuery(pathToDirectory);
        outputStream.writeObject(query);
        Response response = (Response) inputStream.readObject();

        handleError(response);

        return ((ListResponse) response).directoryItems;
    }

    public void executeGet(String pathToFile, String destinationPath)
            throws Exception {
        Query query = new GetQuery(pathToFile, destinationPath);
        outputStream.writeObject(query);
        Response response = (Response) inputStream.readObject();
        handleError(response);
    }

    public void executeClose() throws IOException {
        outputStream.writeObject(new CloseQuery());
    }

    @Override
    public void close() throws Exception {
        executeClose();

        if (!socket.isClosed()) {
            socket.close();
        }
    }

    private static void handleError(Response response) throws Exception {
        if (response instanceof ErrorResponse) {
            throw new Exception(((ErrorResponse) response).message);
        }
    }
}
