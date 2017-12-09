package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.util.PartedFile;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class FileManager {
    private final List<PartedFile> files;

    public FileManager(String pathToListFiles)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(new File(pathToListFiles))))
        {
            files = (List<PartedFile>) in.readObject();
        }
    }

    public List<Integer> getUploaded() {
        return files.stream()
                .filter(file -> file.getNumAvailableParts() != 0)
                .map(PartedFile::getId)
                .collect(Collectors.toList());
    }
}
