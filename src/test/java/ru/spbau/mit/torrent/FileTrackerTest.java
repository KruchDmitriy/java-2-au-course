package ru.spbau.mit.torrent;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.spbau.mit.torrent.util.Source;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class FileTrackerTest {
    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @AfterClass
    public static void tearDown() {
        TEMPORARY_FOLDER.delete();
    }

    private long getTrackerListSize(FileTracker fileTracker) {
        return fileTracker.getListResponse().filesInfo.size();
    }

    @Test
    public void saveLoadTest() throws IOException {
        File trackerStateFile = TEMPORARY_FOLDER.newFile("tracker.state");
        String trackerStateFilePath = trackerStateFile.getCanonicalPath();

        FileTracker fileTracker = new FileTracker(trackerStateFilePath);
        assertEquals(0, getTrackerListSize(fileTracker));
        fileTracker.saveState();

        fileTracker = new FileTracker(trackerStateFilePath);
        assertEquals(0, getTrackerListSize(fileTracker));

        fileTracker.addFile("kuki", 1);
        fileTracker.addFile("kuki", 1);
        fileTracker.addFile("kuki", 1);
        fileTracker.saveState();

        fileTracker = new FileTracker(trackerStateFilePath);
        assertEquals(3, getTrackerListSize(fileTracker));

        fileTracker.addFile("baki", 3);
        fileTracker.addFile("baki", 2);
        fileTracker.saveState();

        fileTracker = new FileTracker(trackerStateFilePath);
        assertEquals(5, getTrackerListSize(fileTracker));
    }
}
