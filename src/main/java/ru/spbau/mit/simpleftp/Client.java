package ru.spbau.mit.simpleftp;

import java.io.*;
import java.net.Socket;

public class Client implements AutoCloseable {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = Server.DEFAULT_PORT;
    private final Socket socket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

    public Client() throws IOException {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
    }

    public DirectoryItem[] executeList(String fileName) throws IOException, InterruptedException {
        outputStream.writeInt(Command.List.id);
        outputStream.writeUTF(fileName);

        final int length = inputStream.readInt();
        DirectoryItem[] files = new DirectoryItem[length];

        for (int i = 0; i < length; i++) {
            String file = inputStream.readUTF();
            boolean isDirectory = inputStream.readBoolean();
            files[i] = new DirectoryItem(file, isDirectory);
        }

        return files;
    }

    public String executeGet(String fileName) throws IOException {
        outputStream.writeInt(Command.Get.id);
        outputStream.writeUTF(fileName);

        StringBuilder builder = new StringBuilder();
        long length = inputStream.readLong();
        byte[] bytes = new byte[256];
        for (int counter = 0, read; counter < length; counter += read) {
            read = inputStream.read(bytes);
            builder.append(new String(bytes, 0, read));
        }

        return builder.toString();
    }

    @Override
    public void close() throws Exception {
        outputStream.writeInt(Command.Shutdown.id);

        if (!socket.isClosed()) {
            socket.close();
        }
    }

    public static class DirectoryItem {
        public final String fileName;
        public final boolean isDirectory;

        public DirectoryItem(String fileName, boolean isDirectory) {
            this.fileName = fileName;
            this.isDirectory = isDirectory;
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof DirectoryItem)) {
                return false;
            }
            DirectoryItem item = (DirectoryItem) obj;

            return fileName.equals(item.fileName) && isDirectory == item.isDirectory;
        }
    }
}
