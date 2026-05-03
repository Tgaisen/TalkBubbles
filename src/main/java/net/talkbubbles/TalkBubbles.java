package net.talkbubbles;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.talkbubbles.config.TalkBubblesConfig;
import net.talkbubbles.network.BubblePayload;
import net.talkbubbles.network.BubbleReceiver;

public class TalkBubbles implements ClientModInitializer {

    public static TalkBubblesConfig CONFIG = new TalkBubblesConfig();

    @Override
    public void onInitializeClient() {
        AutoConfig.register(TalkBubblesConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(TalkBubblesConfig.class).getConfig();

        PayloadTypeRegistry.playS2C().register(BubblePayload.ID, BubblePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BubblePayload.ID, (payload, context) ->
                BubbleReceiver.receive(context.client(), payload.sender(), payload.message()));
    }

}
