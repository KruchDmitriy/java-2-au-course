package ru.spbau.mit.torrent;

import org.junit.Test;

import java.io.IOException;
import java.net.BindException;

public class ServerTest {
    @Test(expected = BindException.class)
    public void runTwiceTest() throws IOException, InterruptedException {
        new Server();
        new Server();
    }
}
