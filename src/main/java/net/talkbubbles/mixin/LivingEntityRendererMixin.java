package net.talkbubbles.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.talkbubbles.accessor.PlayerEntityRenderStateAccessor;
import net.talkbubbles.util.Bubble;
import net.talkbubbles.util.RenderBubble;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
    private void talkbubbles$renderBubbles(LivingEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }
        if (state.invisible) {
            return;
        }
        PlayerEntityRenderStateAccessor accessor = (PlayerEntityRenderStateAccessor) playerState;
        List<Bubble> bubbles = accessor.talkbubbles$getBubbles();
        if (bubbles == null || bubbles.isEmpty()) {
            return;
        }
        RenderBubble.renderBubbles(matrixStack, queue, MinecraftClient.getInstance().textRenderer,
                cameraState.orientation, bubbles, accessor.talkbubbles$getBubbleEntityHeight(), state.light);
    }
}
