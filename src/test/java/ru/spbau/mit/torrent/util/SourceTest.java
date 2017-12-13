package ru.spbau.mit.torrent.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SourceTest {
    @Test
    public void testIPComparator() {
        Source.IP ip1 = new Source.IP(new byte[]{0, 0, -128, 2});
        Source.IP ip2 = new Source.IP(new byte[]{0, 0, 2, 2});
        Source.IP ip3 = new Source.IP(new byte[]{0, 0, 127, 2});

        assertTrue(ip1.compareTo(ip2) < 0);
        assertTrue(ip2.compareTo(ip1) > 0);
        assertTrue(ip3.compareTo(ip2) > 0);
        assertTrue(ip3.compareTo(ip1) > 0);
    }
}
