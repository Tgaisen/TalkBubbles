# TalkBubbles Plugin Message Protocol

This document describes the wire protocol used by the TalkBubbles Fabric mod, so a Bukkit / Paper plugin can drive bubbles directly instead of relying on the client guessing senders from chat text.

The mod exposes a single S2C plugin message channel:

| Channel             | Direction          | Purpose                                        |
| ------------------- | ------------------ | ---------------------------------------------- |
| `talkbubbles:bubble`| server → client    | Render one bubble above the named player.      |

If the client mod is installed, it announces this channel during the standard vanilla `minecraft:register` handshake. If the server-side plugin is present, it sends the packet whenever it wants a bubble shown. Either side missing is fine — both pieces are optional.

## Wire format

After the standard plugin-message channel header that vanilla Minecraft adds, the payload bytes are:

| Offset | Bytes | Field          | Encoding                                                                 |
| ------ | ----- | -------------- | ------------------------------------------------------------------------ |
| 0      | 8     | sender UUID hi | big-endian `long` (= `UUID.getMostSignificantBits()`)                    |
| 8      | 8     | sender UUID lo | big-endian `long` (= `UUID.getLeastSignificantBits()`)                   |
| 16     | 1‑5   | message length | Minecraft `VarInt` — UTF-8 byte length of the message                    |
| ...    | N     | message text   | UTF-8 bytes, no terminator. Max 32767 chars.                             |

This is exactly what `PacketByteBuf.writeUuid()` followed by `writeString(s, 32767)` produce on the Fabric side, so the plugin just needs to mirror it byte-for-byte.

`VarInt` is the standard Minecraft variable-length integer:

```java
static void writeVarInt(java.io.DataOutput out, int value) throws java.io.IOException {
    while ((value & ~0x7F) != 0) {
        out.writeByte((value & 0x7F) | 0x80);
        value >>>= 7;
    }
    out.writeByte(value);
}
```

## Paper / Bukkit reference implementation

### `plugin.yml`

```yaml
name: TalkBubbles
version: 1.0.0
main: com.example.talkbubbles.TalkBubblesPlugin
api-version: '1.21'
```

### Plugin main

```java
package com.example.talkbubbles;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class TalkBubblesPlugin extends JavaPlugin implements Listener {

    public static final String CHANNEL = "talkbubbles:bubble";
    public static final int MAX_MESSAGE_BYTES = 32767;

    /** Players whose UUIDs (over 30 blocks of the sender, in the same world) get the packet. */
    private static final double DEFAULT_RADIUS = 30.0D;

    @Override
    public void onEnable() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        broadcastBubble(sender.getUniqueId(), text, sender, DEFAULT_RADIUS);
    }

    /**
     * Send a bubble for {@code senderUuid} carrying {@code message} to every nearby player
     * (in the same world, within {@code radius} blocks of {@code anchor}) whose client has
     * the talkbubbles mod installed.
     */
    public void broadcastBubble(UUID senderUuid, String message, Player anchor, double radius) {
        byte[] payload = encode(senderUuid, message);
        if (payload == null) return;
        double r2 = radius * radius;
        for (Player target : anchor.getWorld().getPlayers()) {
            if (target.getLocation().distanceSquared(anchor.getLocation()) > r2) continue;
            if (!target.getListeningPluginChannels().contains(CHANNEL)) continue; // client doesn't have the mod
            target.sendPluginMessage(this, CHANNEL, payload);
        }
    }

    private static byte[] encode(UUID sender, String message) {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        if (msgBytes.length > MAX_MESSAGE_BYTES) {
            // Truncate at a UTF-8 boundary to avoid splitting a code point mid-byte.
            int safe = MAX_MESSAGE_BYTES;
            while (safe > 0 && (msgBytes[safe] & 0xC0) == 0x80) safe--;
            byte[] cut = new byte[safe];
            System.arraycopy(msgBytes, 0, cut, 0, safe);
            msgBytes = cut;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeLong(sender.getMostSignificantBits());
        out.writeLong(sender.getLeastSignificantBits());
        try {
            writeVarInt(out, msgBytes.length);
        } catch (IOException impossible) {
            return null;
        }
        out.write(msgBytes);
        return out.toByteArray();
    }

    private static void writeVarInt(java.io.DataOutput out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }
}
```

## Behavioural notes

1. **Client capability check.** `Player.getListeningPluginChannels()` reflects what the client registered during the `minecraft:register` handshake. Vanilla clients won't be in that set, so the plugin naturally skips them — no need to know who has the mod ahead of time.
2. **Sender doesn't need to be the speaking player.** The UUID in the packet just selects which player entity gets the bubble on the receiving client. You can drive bubbles for NPCs/citizens by sending the NPC's UUID — as long as the client sees an `AbstractClientPlayerEntity` with that UUID, the bubble will sit on them.
3. **Range filtering belongs on the server.** The mod's client-side `chatRange` is a final defense, but the server is the right place to decide who a bubble should reach. Don't broadcast cross-world.
4. **System / staff messages.** If you want a global server announcement to show on, say, the staff member who issued it, send a bubble for that staff member's UUID containing the announcement text. The client doesn't care whether the chat itself was a `/say` or a plugin broadcast — it just renders what the protocol tells it.
5. **No delivery confirmation.** Plugin messages are fire-and-forget. If a packet exceeds 32767 bytes the client will simply drop it; the encoder above truncates to stay safe.
6. **Falling back gracefully.** When the plugin is absent, the mod's client falls back to its legacy `MessageHandler.onChatMessage` UUID-matching path, which works on vanilla `chat.type.text` but is unreliable on servers that rewrite chat into system messages. Installing this plugin makes that whole code path moot — the client only renders what the server sends.
