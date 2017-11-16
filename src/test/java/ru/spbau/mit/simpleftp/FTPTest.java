package ru.spbau.mit.simpleftp;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.spbau.mit.simpleftp.Client.DirectoryItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FTPTest {
    private static final String TEST_FOLDER = "./src/test/resources/ftpTest/";
    private Server server;

    @Before
    public void setUp() throws IOException, ServerAlreadyStarted {
        server = new Server();
        server.start();
    }

    @After
    public void tearDown() throws ServerAlreadyStopped, InterruptedException {
        if (!server.isStopped()) {
            server.stop();
            Thread.sleep(1000); // waiting for finishing accept thread
        }
    }

    @Test
    public void connectionTest() throws Exception {
        try (Client client = new Client()) {
            client.executeList(".");
        }
    }

    @Test
    public void listTest() throws Exception {
        try (Client client = new Client()) {
            List<DirectoryItem> directoryItems = Arrays.asList(client.executeList(TEST_FOLDER));
            assertEquals(3, directoryItems.size());
            final String folderA = new File(TEST_FOLDER + "a").getCanonicalPath();
            final String folderB = new File(TEST_FOLDER + "b").getCanonicalPath();
            final String textC = new File(TEST_FOLDER + "c.txt").getCanonicalPath();
            final String textAInFolderA = new File(TEST_FOLDER + "a/a.txt").getCanonicalPath();
            final String folderB1InFolderB = new File(TEST_FOLDER + "b/b1").getCanonicalPath();


            assertEquals(3, directoryItems.size());
            assertTrue(directoryItems.contains(new DirectoryItem(folderA, true)));
            assertTrue(directoryItems.contains(new DirectoryItem(folderB, true)));
            assertTrue(directoryItems.contains(new DirectoryItem(textC, false)));

            directoryItems = Arrays.asList(client.executeList(folderA));
            assertEquals(1, directoryItems.size());
            assertTrue(directoryItems.contains(new DirectoryItem(textAInFolderA, false)));

            directoryItems = Arrays.asList(client.executeList(folderB));
            assertEquals(1, directoryItems.size());
            assertTrue(directoryItems.contains(new DirectoryItem(folderB1InFolderB, true)));
        }
    }

    @Test
    public void getTest() throws Exception {
        try (Client client = new Client()) {
            final String textAInFolderA = new File(TEST_FOLDER + "a/a.txt").getCanonicalPath();
            final String textC = new File(TEST_FOLDER + "c.txt").getCanonicalPath();

            String fileContentA = client.executeGet(textAInFolderA);
            assertEquals(getFileContent(textAInFolderA), fileContentA);

            String fileContentC = client.executeGet(textC);
            assertEquals(getFileContent(textC), fileContentC);
        }
    }

    private String getFileContent(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void doubleStartTest() throws ServerAlreadyStarted,
            IOException, ServerAlreadyStopped {
        exception.expect(ServerAlreadyStarted.class);
        server.start();
    }

    @Test(expected = ServerAlreadyStopped.class)
    public void doubleStopTest() throws ServerAlreadyStarted,
            IOException, ServerAlreadyStopped, InterruptedException {
        server.stop();
        Thread.sleep(1000);
        server.stop();
    }
}
