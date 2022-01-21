package com.justfors.ddaodiscordbot.config;

import com.justfors.ddaodiscordbot.listener.EventListener;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfiguration {

	@Value("${discrod.bot.token}")
	private String token;

	@Bean
	public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<EventListener<T>> eventListeners) {
		GatewayDiscordClient client = DiscordClientBuilder.create(token)
				.build()
				.gateway()
				.setEnabledIntents(IntentSet.of(Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES, Intent.DIRECT_MESSAGES))
				.login()
				.blockOptional().orElseThrow();

		for(EventListener<T> listener : eventListeners) {
			client.on(listener.getEventType())
					.flatMap(listener::execute)
					.onErrorResume(listener::handleError)
					.subscribe();
		}

		return client;
	}

}
