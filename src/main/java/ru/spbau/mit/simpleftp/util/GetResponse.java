package ru.spbau.mit.simpleftp.util;

import java.io.*;

public class GetResponse implements Response {
    private static final int TRANSFER_CHUNK_SIZE = 256;
    private File file;
    private String destinationPath;

    public GetResponse(File file, String destinationPath) {
        this.file = file;
        this.destinationPath = destinationPath;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            final long size = file.length();
            out.writeLong(size);
            out.writeUTF(destinationPath);

            long count = 0;
            byte[] buffer = new byte[TRANSFER_CHUNK_SIZE];
            while (count < size) {
                int read = inputStream.read(buffer);
                count += read;
                out.write(buffer, 0, read);
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        long size = in.readLong();
        destinationPath = in.readUTF();
        file = new File(destinationPath);

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            long count = 0;
            byte[] buffer = new byte[TRANSFER_CHUNK_SIZE];
            while (count < size) {
                int read = in.read(buffer);
                count += read;
                outputStream.write(buffer, 0, read);
            }
        }
    }

    private void readObjectNoData() throws ObjectStreamException {
        file = new File(destinationPath);
    }
}
