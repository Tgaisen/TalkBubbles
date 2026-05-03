package net.talkbubbles.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.talkbubbles.TalkBubbles;
import net.talkbubbles.accessor.AbstractClientPlayerEntityAccessor;

/**
 * Client-side dispatcher invoked when the server sends a {@link BubblePayload}.
 * Mirrors the word-wrap + addBubble logic of {@code ChatHudMixin} so server-driven bubbles
 * have the same visual layout as fallback (chat-text-parsed) bubbles.
 */
@Environment(EnvType.CLIENT)
public final class BubbleReceiver {

    private BubbleReceiver() {
    }

    public static void receive(MinecraftClient client, UUID senderUUID, String message) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        if (senderUUID == null || message == null || message.isEmpty()) {
            return;
        }

        List<AbstractClientPlayerEntity> nearby = client.world.getEntitiesByClass(
                AbstractClientPlayerEntity.class,
                client.player.getBoundingBox().expand(TalkBubbles.CONFIG.chatRange),
                EntityPredicates.EXCEPT_SPECTATOR);
        if (!TalkBubbles.CONFIG.showOwnBubble) {
            nearby.remove(client.player);
        }

        for (AbstractClientPlayerEntity p : nearby) {
            if (p.getUuid().equals(senderUUID)) {
                addBubble(client, p, message);
                return;
            }
        }
    }

    private static void addBubble(MinecraftClient client, AbstractClientPlayerEntity player, String message) {
        String[] words = message.split(" ");
        List<String> lines = new ArrayList<>();
        String collector = "";
        int width = 0;
        int height = 0;
        int max = TalkBubbles.CONFIG.maxChatWidth;

        for (int u = 0; u < words.length; u++) {
            int curW = client.textRenderer.getWidth(collector);
            int wordW = client.textRenderer.getWidth(words[u]);
            if (curW < max && curW + wordW <= max) {
                collector = collector + " " + words[u];
                if (u == words.length - 1) {
                    lines.add(collector);
                    height++;
                    int w = client.textRenderer.getWidth(collector);
                    if (width < w) width = w;
                }
            } else {
                lines.add(collector);
                height++;
                int w = client.textRenderer.getWidth(collector);
                if (width < w) width = w;

                collector = words[u];
                if (u == words.length - 1) {
                    lines.add(collector);
                    height++;
                    int w2 = client.textRenderer.getWidth(collector);
                    if (width < w2) width = w2;
                }
            }
        }

        if (width % 2 != 0) {
            width++;
        }

        ((AbstractClientPlayerEntityAccessor) player).talkbubbles$addBubble(lines, player.age, width, height);
    }
}
