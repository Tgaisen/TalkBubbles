package net.talkbubbles.accessor;

import java.util.List;

import net.talkbubbles.util.Bubble;

public interface PlayerEntityRenderStateAccessor {

    void talkbubbles$setBubbles(List<Bubble> bubbles, float entityHeight);

    List<Bubble> talkbubbles$getBubbles();

    float talkbubbles$getBubbleEntityHeight();
}
