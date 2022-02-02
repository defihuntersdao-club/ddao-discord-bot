package com.justfors.ddaodiscordbot.service;

import static java.lang.String.format;

import com.justfors.ddaodiscordbot.repository.DdaoUserRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DiscordServiceImpl implements DiscordService {

	private final DdaoUserRepository ddaoUserRepository;
	private final GatewayDiscordClient discordClient;

	public DiscordServiceImpl(final DdaoUserRepository ddaoUserRepository, final GatewayDiscordClient discordClient) {
		this.ddaoUserRepository = ddaoUserRepository;
		this.discordClient = discordClient;
	}

	@Override
	public void sendTGBindMessage(final Long tgId) {
		var ddaoUser = ddaoUserRepository.findByTGID(tgId);
		if (ddaoUser != null) {
			Member member = discordClient.getMemberById(getGuildId(), Snowflake.of(ddaoUser.getDiscordId())).block(Duration.ofSeconds(10));
			if (member != null) {
				var channel = member.getPrivateChannel().block(Duration.ofSeconds(10));
				if (channel != null) {
					channel.createMessage("Your Telegram and Discord were successfully connected!").block(Duration.ofSeconds(10));
					log.info(format("TG bind notification sent to %s", ddaoUser.getUserName()));
				} else {
					log.info("couldn't get a channel to send the message.");
				}
			}
		}
	}

	private Snowflake getGuildId() {
		if (DiscordDataCache.getGuildId() == null) {
			discordClient.getGuilds().collectList().doOnNext(e -> {
				e.forEach(g -> {
					DiscordDataCache.setGuildId(g.getId());
				});
			}).block(Duration.ofSeconds(10));
		}
		return DiscordDataCache.getGuildId();
	}

}
