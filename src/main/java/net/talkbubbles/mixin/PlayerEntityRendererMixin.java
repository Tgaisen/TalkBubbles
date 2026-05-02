package net.talkbubbles.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.talkbubbles.TalkBubbles;
import net.talkbubbles.accessor.AbstractClientPlayerEntityAccessor;
import net.talkbubbles.accessor.PlayerEntityRenderStateAccessor;
import net.talkbubbles.util.Bubble;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void talkbubbles$copyBubblesToState(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) {
            return;
        }
        AbstractClientPlayerEntityAccessor data = (AbstractClientPlayerEntityAccessor) player;
        int chatTime = TalkBubbles.CONFIG.chatTime;
        int currentAge = entity.age;
        List<Bubble> live = new ArrayList<>();
        for (Bubble b : data.talkbubbles$getBubbles()) {
            if (currentAge - b.addedAge <= chatTime) {
                live.add(b);
            }
        }
        ((PlayerEntityRenderStateAccessor) state).talkbubbles$setBubbles(live, entity.getHeight());
    }
}
