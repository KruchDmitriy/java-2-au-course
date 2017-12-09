package ru.spbau.mit.torrent.util;

import java.io.Serializable;

public class Source implements Comparable<Source>, Serializable {
    public final IP ip;
    public final short port;

    public Source(IP ip, short port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Source) {
            return compareTo((Source) obj) == 0;
        }

        return false;
    }

    @Override
    public int compareTo(Source o) {
        int compareIp = ip.compareTo(o.ip);
        if (compareIp != 0) {
            return compareIp;
        }

        return port - o.port;
    }

    public static class IP implements Comparable<IP>, Serializable {
        private byte[] data = new byte[4];
        public IP(byte b0, byte b1, byte b2, byte b3) {
            data[0] = b0;
            data[1] = b1;
            data[2] = b2;
            data[3] = b3;
        }

        public IP(byte[] data) {
            System.arraycopy(data, 0, this.data, 0, 4);
        }

        @Override
        public String toString() {
            return data[0] + "." + data[1] + "."
                    + data[2] + "." + data[3];
        }

        @Override
        public int compareTo(IP o) {
            int result = 0;
            for (int i = 0; i < 4; i++) {
                result = result * 255 + (data[i] - o.data[i]);
            }

            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IP) {
                return compareTo((IP) obj) == 0;
            }

            return false;
        }
    }
}
