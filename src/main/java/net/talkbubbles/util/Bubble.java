package net.talkbubbles.util;

import java.util.List;

public final class Bubble {

    public final List<String> lines;
    public final int addedAge;
    public final int width;
    public final int height;

    public Bubble(List<String> lines, int addedAge, int width, int height) {
        this.lines = lines;
        this.addedAge = addedAge;
        this.width = width;
        this.height = height;
    }
}
