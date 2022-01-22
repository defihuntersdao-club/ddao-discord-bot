package com.justfors.ddaodiscordbot.config;

import com.justfors.ddaodiscordbot.listener.EventListener;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields.Author;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields.FileSpoiler;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class BotConfiguration {

	@Value("${discrod.bot.token}")
	private String token;

	@Value("${wallet.bind.button}")
	private String walletBindButton;
	@Value("${bot.channel.name}")
	private String botChannelName;

	@Bean
	public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<EventListener<T>> eventListeners) {
		GatewayDiscordClient client = DiscordClientBuilder.create(token)
				.build()
				.gateway()
				.setEnabledIntents(IntentSet.of(Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES, Intent.DIRECT_MESSAGES))
				.login()
				.blockOptional().orElseThrow();

//		//Button creation, need to perform only once
//		AtomicLong channelId = new AtomicLong(0);
//
//		client.getGuilds().collectList().block().forEach(g -> {
//			g.getChannels().collectList().block().forEach(e -> {
//				if (e.getName().contains(botChannelName)) {
//					channelId.set(e.getId().asLong());
//				}
//			});
//		});
//
//		Button button = Button.primary(walletBindButton, "Let's go!");
//		var channel = client.getChannelById(Snowflake.of(channelId.get()))
//				.ofType(GuildMessageChannel.class)
//				.block();
//
//		channel.createMessage(
//				MessageCreateSpec.builder()
//						// Buttons must be in action rows
//						.addEmbed(EmbedCreateSpec.builder()
//								.description("DDAO.bot")
//								.thumbnail("https://i.imgur.com/dmd3Yoo.png")
//								.addField("Verify your assets",
//										"This is a read-only connection. Do not share your private keys."
//												+ " We will never ask for your seed phrase. We will never DM you.", true)
//								.build())
//						.addComponent(ActionRow.of(button))
//						.build()
//		).block();

		for (EventListener<T> listener : eventListeners) {
			client.on(listener.getEventType(), event -> {
						return Mono.fromRunnable(() -> listener.execute(event));
					})
					.subscribe();
		}

		return client;
	}

}
