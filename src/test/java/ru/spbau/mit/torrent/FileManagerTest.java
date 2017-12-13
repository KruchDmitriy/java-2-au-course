package ru.spbau.mit.torrent;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.spbau.mit.torrent.util.PartedFile;

import java.io.IOException;

public class FileManagerTest {
    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @AfterClass
    public static void tearDown() {
        TEMPORARY_FOLDER.delete();
    }

    @Test
    public void saveLoadTest() throws IOException, ClassNotFoundException {
        final String folderPath = TEMPORARY_FOLDER.getRoot().getCanonicalPath();
        TEMPORARY_FOLDER.newFile("a.txt");
        TEMPORARY_FOLDER.newFile("b.txt");

        FileManager fileManager = new FileManager(folderPath);
        PartedFile file1 = new PartedFile(0, folderPath + "/a.txt");
        PartedFile file2 = new PartedFile(1, folderPath + "/b.txt");

        fileManager.addFile(file1);
        fileManager.addFile(file2);
        fileManager.close();

        fileManager = new FileManager(folderPath);
        fileManager.getFile(0);
        fileManager.getFile(1);
        fileManager.close();
    }
}
