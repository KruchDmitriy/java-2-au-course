package ru.spbau.mit.torrent.util;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PartedFile implements Serializable {
    private static final String PART_EXT = ".part";
    private int id;
    private String path;
    private String name;
    private RandomAccessFile file;
    private long size;
    private RandomAccessFile availablePartsFile;
    private Set<Integer> availableParts = new HashSet<>();

    public PartedFile(int id, String path, String mode) throws IOException {
        this.id = id;
        this.path = path;
        processFileEnvironment(mode);
    }

    private void processFileEnvironment(String mode) throws IOException {
        name = Paths.get(path).getName(0).toString();
        file = new RandomAccessFile(path, mode);
        size = file.length();

        availablePartsFile = new RandomAccessFile(path + PART_EXT, mode);
        availablePartsFile.seek(0);
        if (mode.equals("r")) {
            for (int i = 0; i < getNumParts(); i++) {
                if (availablePartsFile.readBoolean()) {
                    availableParts.add(i);
                }
            }
        }
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

    public Part getPart(int id) throws IOException {
        return new Part(id);
    }

    public int getNumParts() {
        return (int) ((size + Part.STD_PART_SIZE - 1) / Part.STD_PART_SIZE);
    }

    public class Part implements Serializable {
        public static final int STD_PART_SIZE = 8192;
        private int id;
        private int size;
        private byte[] data = new byte[STD_PART_SIZE];

        public Part(int id) throws IOException {
            this.id = id;
        }

        public int getSize() {
            return size;
        }

        public boolean isAvailable() throws IOException {
            availablePartsFile.seek(id);
            return availablePartsFile.readBoolean();
        }

        private void readPart() throws IOException {
            if (!isAvailable()) {
                throw new UnavailablePartException("part " + id
                        + " failed to load, unavailable on client");
            }

            file.seek(getPartOffset());
            size = file.read(data, 0, STD_PART_SIZE);
        }

        private void writePart() throws IOException {
            file.seek(getPartOffset());
            file.write(data, 0, size);

            availablePartsFile.seek(getPartOffset());
            availablePartsFile.writeBoolean(true);
        }

        private long getPartOffset() {
            return STD_PART_SIZE * id;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            readPart();
            out.writeInt(size);
            out.write(data, 0, size);
        }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            size = in.readInt();
            in.read(data, 0, size);
            availableParts.add(id);
            writePart();
        }

        private void readObjectNoData()
                throws ObjectStreamException {
            size = 0;
            Arrays.fill(data, (byte) 0);
        }
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException {
        out.writeUTF(path);
        out.writeInt(id);
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        path = in.readUTF();
        id = in.readInt();
        processFileEnvironment("rw");
    }
}
