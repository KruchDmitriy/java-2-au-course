package ru.spbau.mit.torrent;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class ClientTest {
    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @AfterClass
    public static void tearDown() {
        TEMPORARY_FOLDER.delete();
    }

    @Test(expected = FileNotFoundException.class)
    public void uploadNonExistingFile() throws IOException, ClassNotFoundException {
        String temporaryFolder = TEMPORARY_FOLDER.getRoot().toString();
        new Server(temporaryFolder + "/tracker.state");

        Client client = new Client(temporaryFolder);
        client.upload(Paths.get(temporaryFolder + "/a.txt"), 20);
        assertEquals(0, Client.list().size());
    }
}
