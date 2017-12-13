package ru.spbau.mit.torrent;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.spbau.mit.torrent.util.Source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class CommonTests {
    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @AfterClass
    public static void tearDown() {
        TEMPORARY_FOLDER.delete();
    }

    private Client createClient(String folder, int port)
            throws IOException, ClassNotFoundException {
        File folder1 = TEMPORARY_FOLDER.newFolder(folder);
        String path = folder1.getCanonicalPath();

        return new Client(port, path);
    }

    private File createFileWithRandomContent(String name) throws IOException {
        final File file = TEMPORARY_FOLDER.newFile(name);
        byte[] fileContent = new byte[35 * 1024 * 1024];
        new Random().nextBytes(fileContent);
        FileUtils.writeByteArrayToFile(file, fileContent);

        return file;
    }

    @Test
    public void fileTransferTest() throws IOException, ClassNotFoundException {
        new Server(TEMPORARY_FOLDER.getRoot().getCanonicalPath() + "/tracker.state");

        Client client1 = createClient("client1", 8890);
        Client client2 = createClient("client2", 8891);
        final Source sourceClient1 = new Source(new Source.IP("127.0.0.1"), client1.getPort());
        final Source sourceClient2 = new Source(new Source.IP("127.0.0.1"), client2.getPort());

        final File file1 = createFileWithRandomContent("client1/test.txt");
        final File file2 = createFileWithRandomContent("client2/test2.txt");
        int file1Id = client1.upload(Paths.get(file1.getCanonicalPath()), file1.length());
        int file2Id = client2.upload(Paths.get(file2.getCanonicalPath()), file2.length());

        assertTrue(!Client.list().isEmpty());
        assertTrue(Client.sources(file1Id).contains(sourceClient1));
        assertTrue(Client.sources(file2Id).contains(sourceClient2));

        String destinationPath1 = TEMPORARY_FOLDER.getRoot().getCanonicalPath()
                + "/client2/test.txt";
        String destinationPath2 = TEMPORARY_FOLDER.getRoot().getCanonicalPath()
                + "/client1/test2.txt";

        client2.get(file1Id, destinationPath1, file1.length());
        client1.get(file2Id, destinationPath2, file2.length());

        File file1Transfer = new File(destinationPath1);
        File file2Transfer = new File(destinationPath2);

        FileUtils.contentEquals(file1, file1Transfer);
        FileUtils.contentEquals(file2, file2Transfer);
    }
}
