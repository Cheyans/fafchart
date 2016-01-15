/**
 * @author Aaron Elligsen
 */

package com.faforever.fachart;

/**
 * This point class is a point made of a long and an int. The java library
 * does not have a matching point class for these types. It does have classes for
 * floats and doubles, but I'd rather not have to cast back down from those types.
 */

public class Point {
    public long x;
    public int y;

    public Point(long a, int b) {
        x = a;
        y = b;
    }

    public String toString() {
        return "series.add(" + x + "," + y + ");";
    }
}
