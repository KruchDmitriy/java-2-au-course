package ru.spbau.mit.torrent.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

public class Source implements Comparable<Source>, Serializable {
    public final IP ip;
    public final int port;

    public Source(IP ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public Socket createSocket() throws IOException {
        return new Socket(ip.toString(), port);
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

    @Override
    public String toString() {
        return ip.toString() + ":" + port;
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

        public IP(String ip) {
            String[] bytes = ip.split("\\.");
            data[0] = Byte.parseByte(bytes[0]);
            data[1] = Byte.parseByte(bytes[1]);
            data[2] = Byte.parseByte(bytes[2]);
            data[3] = Byte.parseByte(bytes[3]);
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
