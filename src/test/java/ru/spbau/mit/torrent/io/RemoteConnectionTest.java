package ru.spbau.mit.torrent.io;

import org.junit.Test;
import ru.spbau.mit.torrent.io.query.Query;
import ru.spbau.mit.torrent.io.response.Response;
import ru.spbau.mit.torrent.util.ServerTask;
import ru.spbau.mit.torrent.util.Source;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertEquals;

public class RemoteConnectionTest {
    private static final short TEST_SERVER_PORT = 8099;
    private static final Source TEST_SOURCE = new Source(
            new Source.IP("127.0.0.1"), TEST_SERVER_PORT);
    private static final String TEST_TEXT = "message";

    @Test
    public void simpleTest() throws IOException,
            ClassNotFoundException, InterruptedException {
        new Thread(() -> {
            try (Socket socket = new ServerSocket(TEST_SERVER_PORT).accept();
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
            {
                while (true) {
                    Query query = (Query) in.readObject();

                    if (query instanceof CloseQuery) {
                        return;
                    }

                    TestQuery testQuery = (TestQuery) query;
                    assertEquals(TEST_TEXT, testQuery.someText);
                    out.writeObject(new TestResponse(testQuery.someText));
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();

        // waiting for starting server thread
        Thread.sleep(1000);

        try (RemoteConnection connection = new RemoteConnection(TEST_SOURCE)) {
            for (int i = 0; i < 10; i++) {
                connection.send(new TestQuery(TEST_TEXT));
                TestResponse response = connection.receive(TestResponse.class);
                assertEquals(TEST_TEXT, response.someText);
            }
        }
    }

    private static class TestQuery extends Query {
        final String someText;

        TestQuery(String someText) {
            super(QueryId.TEST);
            this.someText = someText;
        }
    }

    private static class TestResponse implements Response {
        final String someText;

        private TestResponse(String someText) {
            this.someText = someText;
        }
    }
}
