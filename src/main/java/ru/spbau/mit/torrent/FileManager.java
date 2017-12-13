package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.util.PartedFile;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileManager implements AutoCloseable {
    private final static String MANAGER_STATE_FILE = "manager.state";
    private final Map<Integer, PartedFile> files =
            Collections.synchronizedMap(new HashMap<>());
    private final File managerStateFile;

    public FileManager(String pathToListFiles)
            throws IOException, ClassNotFoundException {
        managerStateFile = new File(pathToListFiles + MANAGER_STATE_FILE);
        if (!managerStateFile.exists()) {
            dumpState();
            return;
        }

        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(managerStateFile))) {
            files.putAll((Map<Integer, PartedFile>) in.readObject());
        }
    }

    public void addFile(PartedFile file) {
        files.put(file.getId(), file);
    }

    public List<Integer> getUploaded() {
        return files.values().stream()
                .filter(file -> file.getNumAvailableParts() != 0)
                .map(PartedFile::getId)
                .collect(Collectors.toList());
    }

    public PartedFile getFile(int fileId) {
        return files.get(fileId);
    }

    private void dumpState() throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(managerStateFile))) {
            out.writeObject(files);
        }
    }

    @Override
    public void close() throws IOException {
        dumpState();
    }
}
