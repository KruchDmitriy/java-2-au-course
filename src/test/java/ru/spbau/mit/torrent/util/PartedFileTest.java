package ru.spbau.mit.torrent.util;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.spbau.mit.torrent.io.response.GetResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PartedFileTest {
    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @AfterClass
    public static void tearDown() {
        TEMPORARY_FOLDER.delete();
    }

    @Test
    public void createNewFileTest() throws IOException {
        PartedFile partedFile = new PartedFile(0, "a.txt", 27);
        assertEquals(0, partedFile.getNumAvailableParts());
        Files.deleteIfExists(Paths.get("a.txt"));
        Files.deleteIfExists(Paths.get("a.txt.part"));
    }

    @Test
    public void readExistingFileTest() throws IOException {
        File file = TEMPORARY_FOLDER.newFile("a.txt");
        byte[] data = new byte[200];
        Arrays.fill(data, (byte) 1);
        FileUtils.writeByteArrayToFile(file, data);

        PartedFile partedFile = new PartedFile(0, file.getCanonicalPath());
        assertTrue(partedFile.containAllParts());
        assertEquals(1, partedFile.getNumAvailableParts());
        assertEquals(partedFile.getNumAvailableParts(), partedFile.getNumParts());

        file.deleteOnExit();
    }

    @Test
    public void readWritePartTest() throws IOException, ClassNotFoundException {
        File sourceFile = TEMPORARY_FOLDER.newFile("src.txt");
        File destFile = TEMPORARY_FOLDER.newFile("dst.txt");

        Random rng = new Random();
        byte[] data = new byte[35 * 1024 * 1024];
        rng.nextBytes(data);
        FileUtils.writeByteArrayToFile(sourceFile, data);

        PartedFile sourcePartedFile = new PartedFile(0, sourceFile.getCanonicalPath());
        PartedFile destPartedFile = new PartedFile(0,
                destFile.getCanonicalPath(), sourcePartedFile.getSize());

        List<Integer> parts = IntStream
                .range(0, sourcePartedFile.getNumParts())
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(parts);

        for (int partId: parts) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(Config.STD_PART_SIZE);
            ObjectOutputStream out = new ObjectOutputStream(byteStream);

            out.writeObject(sourcePartedFile.sendPart(partId));

            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(byteStream.toByteArray()));

            GetResponse response = (GetResponse) in.readObject();
            destPartedFile.receivePart(partId, response);
            assertTrue(destPartedFile.containPart(partId));
        }

        assertTrue(destPartedFile.containAllParts());
        assertTrue(FileUtils.contentEquals(sourceFile, destFile));

        sourceFile.deleteOnExit();
        destFile.deleteOnExit();
    }
}
