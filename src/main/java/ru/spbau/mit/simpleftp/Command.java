package ru.spbau.mit.simpleftp;

public enum Command {
    List(0),
    Get(1),
    Shutdown(2),
    Undefined(3);

    int id;

    Command(int id) {
        this.id = id;
    }

    public static Command createCommand(int id) {
        if (id < 0 || id > 2) {
            return Undefined;
        }

        return Command.values()[id];
    }
}
