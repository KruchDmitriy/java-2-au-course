package ru.spbau.mit.torrent.io.query;

import java.io.*;

public abstract class Query implements Serializable {
    public final QueryId id;

    public Query(QueryId id) {
        this.id = id;
    }

    public enum QueryId implements Serializable {
        LIST((byte) 1),
        UPLOAD((byte) 2),
        SOURCES((byte) 3),
        UPDATE((byte) 4);

        private byte id;

        QueryId(byte id) {
            this.id = id;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeByte(id);
        }

        private void readObject(ObjectInputStream in)
                throws Exception {
            id = in.readByte();
        }

        private void readObjectNoData() throws ObjectStreamException {
        }
    }
}