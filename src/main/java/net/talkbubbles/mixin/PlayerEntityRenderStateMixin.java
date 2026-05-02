package net.talkbubbles.mixin;

import java.util.Collections;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.talkbubbles.accessor.PlayerEntityRenderStateAccessor;
import net.talkbubbles.util.Bubble;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements PlayerEntityRenderStateAccessor {

    @Unique
    private List<Bubble> talkbubbles$bubbles = Collections.emptyList();
    @Unique
    private float talkbubbles$entityHeight;

    @Override
    public void talkbubbles$setBubbles(List<Bubble> bubbles, float entityHeight) {
        this.talkbubbles$bubbles = bubbles == null ? Collections.emptyList() : bubbles;
        this.talkbubbles$entityHeight = entityHeight;
    }

    @Override
    public List<Bubble> talkbubbles$getBubbles() {
        return this.talkbubbles$bubbles;
    }

    @Override
    public float talkbubbles$getBubbleEntityHeight() {
        return this.talkbubbles$entityHeight;
    }
}
