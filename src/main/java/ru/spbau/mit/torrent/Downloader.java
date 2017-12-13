package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.io.RemoteConnection;
import ru.spbau.mit.torrent.io.query.GetQuery;
import ru.spbau.mit.torrent.io.query.StatQuery;
import ru.spbau.mit.torrent.io.response.GetResponse;
import ru.spbau.mit.torrent.io.response.StatResponse;
import ru.spbau.mit.torrent.util.Config;
import ru.spbau.mit.torrent.util.PartedFile;
import ru.spbau.mit.torrent.util.Source;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Downloader {
    private final Map<PartedFile, SourceLookup> downloadingFiles =
            Collections.synchronizedMap(new LinkedHashMap<>());
    private final ExecutorService executorService;
    private final FileManager fileManager;

    public Downloader(ExecutorService executorService, FileManager fileManager) {
        this.executorService = executorService;
        this.fileManager = fileManager;
    }

    public void downloadFile(int id, String destinationPath, long size)
            throws IOException, ClassNotFoundException {
        PartedFile file = new PartedFile(id, destinationPath, size);
        fileManager.addFile(file);
        downloadingFiles.put(file, new SourceLookup(Client.sources(id)));
        executorService.submit(new FileDownloader(file));
    }

    private static class SourceLookup {
        private final Queue<Source> sources;

        SourceLookup(List<Source> sources) {
            this.sources = new ArrayDeque<>(sources);
        }

        Source nextSource() {
             return sources.poll();
        }

        boolean isEmpty() {
            return sources.isEmpty();
        }
    }

    private class FileDownloader implements Runnable {
        private final PartedFile file;

        FileDownloader(PartedFile file) {
            this.file = file;
        }

        @Override
        public void run() {
            try {
                while (!file.containAllParts()) {
                    List<Future<?>> futures = new ArrayList<>(Config.NUM_SOURCES_PER_DOWNLOAD);
                    SourceLookup lookup = downloadingFiles.get(file);
                    if (lookup.isEmpty()) {
                        lookup = new SourceLookup(Client.sources(file.getId()));
                    }

                    for (int i = 0; i < Config.NUM_SOURCES_PER_DOWNLOAD; i++) {
                        if (lookup.isEmpty()) {
                            break;
                        }
                        futures.add(executorService.submit(
                                new PartDownloader(file, lookup.nextSource())
                        ));
                    }

                    for (Future<?> future: futures) {
                        future.get();
                    }
                }
            } catch (InterruptedException | ExecutionException
                    | IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private class PartDownloader implements Runnable {
        private final PartedFile file;
        private final Source source;
        private final Set<Integer> sourceParts = new HashSet<>();

        PartDownloader(PartedFile file, Source source) {
            this.file = file;
            this.source = source;
        }

        @Override
        public void run() {
            try (RemoteConnection connection = new RemoteConnection(source)) {
                getFileParts(connection);

                for (int partId: sourceParts) {
                    if (!file.containPart(partId)) {
                        downloadPart(partId, connection);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void getFileParts(RemoteConnection connection)
                throws IOException, ClassNotFoundException {
            connection.send(new StatQuery(file.getId()));
            StatResponse response = connection.receive(StatResponse.class);
            sourceParts.addAll(response.availableParts);
        }

        private void downloadPart(int partId, RemoteConnection connection)
                throws IOException, ClassNotFoundException {
            connection.send(new GetQuery(file.getId(), partId));
            GetResponse response = connection.receive(GetResponse.class);
            file.receivePart(partId, response);
        }
    }
}
