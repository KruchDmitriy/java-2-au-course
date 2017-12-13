package ru.spbau.mit.torrent.util;

import ru.spbau.mit.torrent.io.response.ErrorResponse;
import ru.spbau.mit.torrent.io.response.GetResponse;
import ru.spbau.mit.torrent.io.response.Response;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import static ru.spbau.mit.torrent.util.Config.STD_PART_SIZE;

public class PartedFile implements Serializable {
    private static final String PART_EXT = ".part";
    private int id;
    private String path;
    private String name;
    private RandomAccessFile file;
    private long size;
    private RandomAccessFile availablePartsFile;
    private Map<Integer, Part> availableParts =
            Collections.synchronizedMap(new HashMap<>());

    public PartedFile(int id, String path) throws IOException {
        this.id = id;
        this.path = path;
        createFileEnvironment("r");
    }

    public PartedFile(int id, String path, long size) throws IOException {
        this.id = id;
        this.path = path;
        this.size = size;
        createFileEnvironment("rw");
    }

    private void createFileEnvironment(String mode) throws IOException {
        name = Paths.get(path).getName(0).toString();
        file = new RandomAccessFile(path, mode);
        if (mode.equals("r")) {
            size = file.length();
        }

        final File file = new File(path + PART_EXT);
        if (!file.exists() && mode.equals("r")) {
            createPartFile(path);
        }

        availablePartsFile = new RandomAccessFile(path + PART_EXT, mode);

        if (mode.equals("r")) {
            for (int i = 0; i < getNumParts(); i++) {
                if (availablePartsFile.readBoolean()) {
                    availableParts.put(i, new Part(i));
                }
            }
        }
    }

    private void createPartFile(String fileName) {
        try (DataOutputStream out = new DataOutputStream(
                new FileOutputStream(fileName + PART_EXT))) {
            for (int i = 0; i < getNumParts(); i++) {
                out.writeBoolean(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PartedFile) {
            return this.id == ((PartedFile) obj).getId();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getId();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public int getNumAvailableParts() {
        return availableParts.size();
    }

    public Response sendPart(int partId) {
        if (!availableParts.containsKey(partId)) {
            return new ErrorResponse("Trying to send unavailable part.");
        }

        return availableParts.get(partId).createResponse();
    }

    public void receivePart(int partId, GetResponse response)
            throws IOException, ClassNotFoundException {
        if (availableParts.containsKey(partId)) {
            throw new PartAlreadyReceivedException();
        }

        final Part part = new Part(partId, response);
        availableParts.put(partId, part);
    }

    public int getNumParts() {
        return (int) ((size + STD_PART_SIZE - 1) / STD_PART_SIZE);
    }

    public boolean containPart(int partId) {
        return availableParts.containsKey(partId);
    }

    public boolean containAllParts() {
        return availableParts.size() == getNumParts();
    }

    public List<Integer> getAvailableParts() {
        return new ArrayList<>(availableParts.keySet());
    }

    private class Part {
        private int id;
        private int size;
        private byte[] data = new byte[STD_PART_SIZE];

        private Part(int id) {
            this.id = id;
        }

        private Part(int id, GetResponse response) throws IOException {
            this.id = id;
            size = response.size;
            data = response.data;
            writeToDisk();
        }

        public int getSize() {
            return size;
        }

        public boolean isAvailable() throws IOException {
            synchronized (PartedFile.this) {
                availablePartsFile.seek(id);
                return availablePartsFile.readBoolean();
            }
        }

        private void readFromDisk() throws IOException {
            synchronized (PartedFile.this) {
                if (!isAvailable()) {
                    throw new UnavailablePartException("Part " + id
                            + " failed to load, unavailable on client");
                }

                file.seek(getPartOffset());
                size = file.read(data, 0, STD_PART_SIZE);
            }
        }

        private void writeToDisk() throws IOException {
            synchronized (PartedFile.this) {
                file.seek(getPartOffset());
                file.write(data, 0, size);

                availablePartsFile.seek(getPartOffset());
                availablePartsFile.writeBoolean(true);
            }
        }

        private Response createResponse() {
            try {
                readFromDisk();
            } catch (IOException e) {
                return new ErrorResponse("Error while reading file", e);
            }
            return new GetResponse(size, data);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Part) {
                return id == ((Part) obj).id;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return id;
        }

        private long getPartOffset() {
            return STD_PART_SIZE * id;
        }
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException {
        out.writeUTF(path);
        out.writeInt(id);
        out.flush();
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        path = in.readUTF();
        id = in.readInt();
        createFileEnvironment("rw");
    }
}
