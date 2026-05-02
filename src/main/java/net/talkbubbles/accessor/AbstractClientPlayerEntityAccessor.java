package net.talkbubbles.accessor;

import java.util.Deque;
import java.util.List;

import net.talkbubbles.util.Bubble;

public interface AbstractClientPlayerEntityAccessor {

    void talkbubbles$addBubble(List<String> lines, int currentAge, int width, int height);

    Deque<Bubble> talkbubbles$getBubbles();
}
