package net.talkbubbles.network;

import java.util.UUID;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C plugin message carrying a single bubble to display above a player.
 *
 * Wire format (raw bytes after the standard plugin-message channel prefix):
 *   - 16 bytes: sender UUID, written as two big-endian longs (mostSignificantBits, leastSignificantBits)
 *   - VarInt:   UTF-8 byte length of {@code message}
 *   - N bytes:  message UTF-8 bytes (max {@link #MAX_MESSAGE_LENGTH} chars)
 *
 * This is exactly what {@code PacketByteBuf.writeUuid} + {@code writeString} produce, so a
 * Bukkit/Paper plugin can replicate it with raw {@code DataOutput} + an MC varint helper.
 */
public record BubblePayload(UUID sender, String message) implements CustomPayload {

    public static final Identifier CHANNEL = Identifier.of("talkbubbles", "bubble");
    public static final CustomPayload.Id<BubblePayload> ID = new CustomPayload.Id<>(CHANNEL);

    public static final int MAX_MESSAGE_LENGTH = 32767;

    public static final PacketCodec<RegistryByteBuf, BubblePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeUuid(payload.sender);
                buf.writeString(payload.message, MAX_MESSAGE_LENGTH);
            },
            buf -> new BubblePayload(buf.readUuid(), buf.readString(MAX_MESSAGE_LENGTH))
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
