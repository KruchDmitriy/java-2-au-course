package ru.spbau.mit.torrent;

import org.apache.commons.io.FileUtils;
import ru.spbau.mit.torrent.io.response.ListResponse;
import ru.spbau.mit.torrent.util.Config;
import ru.spbau.mit.torrent.util.FileInfo;
import ru.spbau.mit.torrent.util.Source;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FileTracker {
    private final String stateFilePath;
    private final List<SourceTimeStorage> fileIdToListSources =
            Collections.synchronizedList(new ArrayList<>());
    private final List<FileInfo> fileInfos;
    private final AtomicInteger numFiles = new AtomicInteger();

    public FileTracker(String stateFilePath) {
        this.stateFilePath = stateFilePath;

        if (!new File(stateFilePath).exists()) {
            fileInfos = Collections.synchronizedList(new ArrayList<>());
            return;
        }

        List<FileInfo> loadedCollection =
                Collections.synchronizedList(new ArrayList<>());
        try {
            loadedCollection = Collections.synchronizedList(
                    Files.lines(Paths.get(stateFilePath))
                            .map(FileInfo::new)
                            .collect(Collectors.toList()));
        } catch (IOException e) {
            System.err.println("Failed to load tracker state");
            e.printStackTrace();
        }
        fileInfos = loadedCollection;
    }

    public boolean updateSource(Source source, List<Integer> fileIds) {
        fileIds.stream()
                .mapToInt(Integer::intValue)
                .forEach(fileId -> {
                    SourceTimeStorage lookup = fileIdToListSources.get(fileId);
                    lookup.update(source);
                });

        return true;
    }

    public ListResponse getListResponse() {
        return new ListResponse(fileInfos);
    }

    public int addFile(String fileName, long size) {
        final int fileId = numFiles.getAndIncrement();
        fileInfos.add(new FileInfo(fileId, fileName, size));
        fileIdToListSources.add(new SourceTimeStorage());
        return fileId;
    }

    public List<Source> getListSources(int fileId) {
        if (fileId >= fileIdToListSources.size()) {
            throw new RuntimeException(new IOException("Unknown file for tracker."));
        }

        final SourceTimeStorage lookup = fileIdToListSources.get(fileId);
        lookup.removeExpired();
        return lookup.toList();
    }

    public void saveState() {
        List<String> serializedFileInfos = fileInfos.stream()
                        .map(FileInfo::serialize)
                        .collect(Collectors.toList());
        try {
            FileUtils.writeLines(new File(stateFilePath), serializedFileInfos);
        } catch (IOException e) {
            System.err.println("Failed to save state of the tracker");
            e.printStackTrace();
        }
    }

    private static class SourceTimeStorage implements Serializable {
        private final Map<Source, LocalDateTime> sources = new TreeMap<>();

        public void update(Source source) {
            sources.put(source, LocalDateTime.now());
        }

        private static boolean isExpired(final LocalDateTime date) {
            return date.plusMinutes(Config.SOURCE_REFRESH_MINUTES)
                    .compareTo(LocalDateTime.now()) < 0;
        }

        public void removeExpired() {
            sources.forEach((source, date) -> {
                if (isExpired(date)) {
                    sources.remove(source);
                }
            });
        }

        public List<Source> toList() {
            return new ArrayList<>(sources.keySet());
        }
    }
}
