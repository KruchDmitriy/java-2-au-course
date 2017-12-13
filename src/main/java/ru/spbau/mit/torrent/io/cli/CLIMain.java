package ru.spbau.mit.torrent.io.cli;

import ru.spbau.mit.torrent.Client;
import ru.spbau.mit.torrent.Server;

import java.io.IOException;

public class CLIMain {
    private static void printHelp() {
        System.out.println("This is help message.\n" +
                "If you want to start server run: " +
                "java -jar <name-jar> server pathToTrackerState\n" +
                "If you want to start client run: " +
                "java -jar <name-jar> client port pathToSourceFolder\n");
    }

    public static void main(String[] args)
            throws IOException, ClassNotFoundException {
        if (args.length == 0) {
            printHelp();
            return;
        }

        if (args[0].equals("server")) {
            new Server(args[1]);
            return;
        }

        if (args[0].equals("client")) {
            if (args.length < 3) {
                printHelp();
                return;
            }

            short port = Short.parseShort(args[1]);
            String pathToSourceFolder = args[2];

            Client client = new Client(port, pathToSourceFolder);
            client.CLICycle();
        }
    }
}
