package net.talkbubbles.util;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.talkbubbles.TalkBubbles;

@Environment(EnvType.CLIENT)
public class RenderBubble {

    private static final Identifier BACKGROUND = Identifier.of("talkbubbles", "textures/gui/background.png");
    private static final int TEXTURE_SIZE = 32;
    private static final float STACK_GAP = 4f;

    public static void renderBubbles(MatrixStack matrixStack, OrderedRenderCommandQueue queue, TextRenderer textRenderer,
            Quaternionf cameraOrientation, List<Bubble> bubbles, float playerHeight, int light) {
        if (bubbles.isEmpty()) {
            return;
        }
        matrixStack.push();

        // Use the tallest bubble in the stack as a baseline offset reference so all bubbles
        // share the same horizontal-billboard transform.
        int referenceHeight = 0;
        for (Bubble b : bubbles) {
            if (b.height > referenceHeight) {
                referenceHeight = b.height;
            }
        }

        matrixStack.translate(0.0D, playerHeight + 0.9F + (referenceHeight > 5 ? 0.1F : 0.0F) + TalkBubbles.CONFIG.chatHeight, 0.0D);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(toEulerXyzDegrees(cameraOrientation).y()));
        matrixStack.scale(0.025F * TalkBubbles.CONFIG.chatScale, -0.025F * TalkBubbles.CONFIG.chatScale, 0.025F);

        int bgColor = packColor(TalkBubbles.CONFIG.backgroundRed, TalkBubbles.CONFIG.backgroundGreen, TalkBubbles.CONFIG.backgroundBlue,
                TalkBubbles.CONFIG.backgroundOpacity);
        int textColor = TalkBubbles.CONFIG.chatColor | 0xFF000000;
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        // Newest bubble at index size-1 should sit closest to the head (lowest in world);
        // iterate from newest to oldest, moving each older bubble further "up" in local Y
        // (which is downward in screen since the Y axis is flipped by the negative scale,
        // hence we accumulate negative offsets to move them upward in world).
        float anchorY = 0f;
        for (int i = bubbles.size() - 1; i >= 0; i--) {
            Bubble b = bubbles.get(i);
            matrixStack.push();
            matrixStack.translate(0f, anchorY, 0f);
            drawSingleBubble(matrixStack, queue, textRenderer, immediate, b, light, bgColor, textColor);
            matrixStack.pop();

            if (i - 1 >= 0) {
                Bubble next = bubbles.get(i - 1);
                // Top of current bubble at local-Y: -8*b.height + 7
                // Bottom of next bubble at local-Y (relative to its anchor): next.height + 9
                // We want next bubble's bottom to sit just above current bubble's top with STACK_GAP.
                anchorY -= (8f * b.height + next.height + 2f + STACK_GAP);
            }
        }
        immediate.draw();

        matrixStack.pop();
    }

    private static void drawSingleBubble(MatrixStack matrixStack, OrderedRenderCommandQueue queue, TextRenderer textRenderer,
            VertexConsumerProvider.Immediate immediate, Bubble bubble, int light, int bgColor, int textColor) {
        final int bw = bubble.width;
        final int bh = bubble.height;
        final List<String> lines = bubble.lines;

        queue.submitCustom(matrixStack, RenderLayer.getEntityTranslucent(BACKGROUND), (entry, vc) -> {
            // Top left
            emitSlice(entry, vc, -bw / 2f - 2f, -bh - (bh - 1f) * 7f, 5f, 5f, 0f, 0f, 5f, 5f, light, bgColor);
            // Mid left
            emitSlice(entry, vc, -bw / 2f - 2f, -bh - (bh - 1f) * 7f + 5f, 5f, bh + (bh - 1f) * 8f, 0f, 6f, 5f, 1f, light, bgColor);
            // Bottom left
            emitSlice(entry, vc, -bw / 2f - 2f, 5f + (bh - 1f), 5f, 5f, 0f, 8f, 5f, 5f, light, bgColor);

            // Top mid
            emitSlice(entry, vc, -bw / 2f + 3f, -bh - (bh - 1f) * 7f, bw - 4f, 5f, 6f, 0f, 5f, 5f, light, bgColor);
            // Mid mid
            emitSlice(entry, vc, -bw / 2f + 3f, -bh - (bh - 1f) * 7f + 5f, bw - 4f, bh + (bh - 1f) * 8f, 6f, 6f, 5f, 1f, light, bgColor);
            // Bottom mid
            emitSlice(entry, vc, -bw / 2f + 3f, 5f + (bh - 1f), bw - 4f, 5f, 6f, 8f, 5f, 5f, light, bgColor);

            // Top right
            emitSlice(entry, vc, bw / 2f - 1f, -bh - (bh - 1f) * 7f, 5f, 5f, 12f, 0f, 5f, 5f, light, bgColor);
            // Mid right
            emitSlice(entry, vc, bw / 2f - 1f, -bh - (bh - 1f) * 7f + 5f, 5f, bh + (bh - 1f) * 8f, 12f, 6f, 5f, 1f, light, bgColor);
            // Bottom right
            emitSlice(entry, vc, bw / 2f - 1f, 5f + (bh - 1f), 5f, 5f, 12f, 8f, 5f, 5f, light, bgColor);
        });

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        for (int u = lines.size(); u > 0; u--) {
            String line = lines.get(u - 1);
            float x = -textRenderer.getWidth(line) / 2.0F;
            float y = (float) lines.size() + (u - lines.size()) * 9f;
            textRenderer.draw(line, x, y, textColor, false, matrix4f, immediate, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
        }
    }

    private static void emitSlice(MatrixStack.Entry entry, VertexConsumer vc, float dx, float dy, float dw, float dh,
            float srcU, float srcV, float srcW, float srcH, int light, int color) {
        float u0 = srcU / TEXTURE_SIZE;
        float v0 = srcV / TEXTURE_SIZE;
        float u1 = (srcU + srcW) / TEXTURE_SIZE;
        float v1 = (srcV + srcH) / TEXTURE_SIZE;
        float x0 = dx;
        float y0 = dy;
        float x1 = dx + dw;
        float y1 = dy + dh;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        Matrix4f m = entry.getPositionMatrix();
        vc.vertex(m, x0, y1, 0f).color(r, g, b, a).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
        vc.vertex(m, x1, y1, 0f).color(r, g, b, a).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
        vc.vertex(m, x1, y0, 0f).color(r, g, b, a).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
        vc.vertex(m, x0, y0, 0f).color(r, g, b, a).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
    }

    private static int packColor(float red, float green, float blue, float alpha) {
        int r = clamp((int) (red * 255f));
        int g = clamp((int) (green * 255f));
        int b = clamp((int) (blue * 255f));
        int a = clamp((int) (alpha * 255f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static Vector3f toEulerXyz(Quaternionf quaternionf) {
        float f = quaternionf.w() * quaternionf.w();
        float g = quaternionf.x() * quaternionf.x();
        float h = quaternionf.y() * quaternionf.y();
        float i = quaternionf.z() * quaternionf.z();
        float j = f + g + h + i;
        float k = 2.0f * quaternionf.w() * quaternionf.x() - 2.0f * quaternionf.y() * quaternionf.z();
        float l = (float) Math.asin(k / j);
        if (Math.abs(k) > 0.999f * j) {
            return new Vector3f(l, 2.0f * (float) Math.atan2(quaternionf.y(), quaternionf.w()), 0.0f);
        }
        return new Vector3f(l, (float) Math.atan2(2.0f * quaternionf.x() * quaternionf.z() + 2.0f * quaternionf.y() * quaternionf.w(), f - g - h + i),
                (float) Math.atan2(2.0f * quaternionf.x() * quaternionf.y() + 2.0f * quaternionf.w() * quaternionf.z(), f - g + h - i));
    }

    private static Vector3f toEulerXyzDegrees(Quaternionf quaternionf) {
        Vector3f vec3f = RenderBubble.toEulerXyz(quaternionf);
        return new Vector3f((float) Math.toDegrees(vec3f.x()), (float) Math.toDegrees(vec3f.y()), (float) Math.toDegrees(vec3f.z()));
    }
}
