package ru.spbau.mit.simpleftp;

import org.junit.Test;

public class FTPTest {
    @Test
    public void connectionTest() throws Exception {
        Server server = new Server();
        server.start();

        try (Client client = new Client()) {
            client.executeList(".");
        }

        server.stop();
    }

    public void listTest() {

    }
}
