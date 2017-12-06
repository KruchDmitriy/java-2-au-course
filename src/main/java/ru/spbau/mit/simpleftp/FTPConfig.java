package ru.spbau.mit.simpleftp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FTPConfig {
    private static final String DEFAULT_CONFIG_PATH = "src/main/resources/simpleftp/config.txt";
    private String address;
    private int port;

    public FTPConfig() {
        String address = null;
        int port = 0;
        try {
            List<String> lines = Files.readAllLines(Paths.get(DEFAULT_CONFIG_PATH));
            for (String line: lines) {
                String[] params = line.split(" ");
                if (params[0].equals("address")) {
                    address = params[1];
                } else if (params[0].equals("port")) {
                    port = Integer.parseInt(params[1]);
                }
            }

            if (address == null) {
                throw new RuntimeException("Config file doesn't contains minimal information");
            }

            this.address = address;
            this.port = port;
        } catch (FileNotFoundException e) {
            System.err.println("Config file not found");
        } catch (IOException e) {
            System.err.println("Couldn't read config file");
        }
    }

    public FTPConfig(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
