package ru.spbau.mit.simpleftp.util;

import java.io.Serializable;
import java.util.List;

public class ListResponse implements Response {
    public final List<DirectoryItem> directoryItems;

    public ListResponse(List<DirectoryItem> directoryItems) {
        this.directoryItems = directoryItems;
    }

    public static class DirectoryItem implements Serializable {
        public final String name;
        public final boolean isDirectory;

        public DirectoryItem(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof DirectoryItem)) {
                return false;
            }
            DirectoryItem item = (DirectoryItem) obj;

            return name.equals(item.name) && isDirectory == item.isDirectory;
        }
    }
}
