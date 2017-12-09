package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.io.response.ListResponse;
import ru.spbau.mit.torrent.util.Config;
import ru.spbau.mit.torrent.util.FileInfo;
import ru.spbau.mit.torrent.util.Source;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileTracker {
    private final List<SourceLookup> fileIdToListSources =
            Collections.synchronizedList(new ArrayList<>());
    private final List<FileInfo> fileInfos =
            Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger numFiles = new AtomicInteger();

    public boolean updateSource(Source source, List<Integer> fileIds) {
        fileIds.stream()
                .mapToInt(Integer::intValue)
                .forEach(fileId -> {
                    SourceLookup lookup = fileIdToListSources.get(fileId);
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
        return fileId;
    }

    public List<Source> getListSources(int fileId) {
        final SourceLookup lookup = fileIdToListSources.get(fileId);
        lookup.removeExpired();
        return lookup.toList();
    }

    private static class SourceLookup implements Serializable {
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
