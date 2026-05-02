package net.talkbubbles.mixin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.talkbubbles.TalkBubbles;
import net.talkbubbles.accessor.AbstractClientPlayerEntityAccessor;
import net.talkbubbles.util.Bubble;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin implements AbstractClientPlayerEntityAccessor {

    @Unique
    private final Deque<Bubble> talkbubbles$bubbles = new ArrayDeque<>();

    @Override
    public void talkbubbles$addBubble(List<String> lines, int currentAge, int width, int height) {
        this.talkbubbles$bubbles.addLast(new Bubble(lines, currentAge, width, height));
        int max = Math.max(1, TalkBubbles.CONFIG.maxBubbles);
        while (this.talkbubbles$bubbles.size() > max) {
            this.talkbubbles$bubbles.pollFirst();
        }
    }

    @Override
    public Deque<Bubble> talkbubbles$getBubbles() {
        return this.talkbubbles$bubbles;
    }
}
