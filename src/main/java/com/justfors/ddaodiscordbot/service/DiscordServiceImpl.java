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
	private final GatewayDiscordClient client;
	private static Snowflake guildId;

	public DiscordServiceImpl(final DdaoUserRepository ddaoUserRepository, final GatewayDiscordClient client) {
		this.ddaoUserRepository = ddaoUserRepository;
		this.client = client;
	}

	@Override
	public void sendTGBindMessage(final Long tgId) {
		var ddaoUser = ddaoUserRepository.findByTGID(tgId);
		if (ddaoUser != null) {
			Member member = client.getMemberById(getGuildId(), Snowflake.of(ddaoUser.getDiscordId())).block(Duration.ofSeconds(10));
			if (member != null) {
				var channel = member.getPrivateChannel().block(Duration.ofSeconds(10));
				if (channel != null) {
					channel.createMessage("Your telegram successfully added.").block(Duration.ofSeconds(10));
					log.info(format("TG bind notification sent to %s", ddaoUser.getUserName()));
				} else {
					log.info("couldn't get a channel to send the message.");
				}
			}
		}
	}

	private Snowflake getGuildId() {
		if (guildId == null) {
			client.getGuilds().collectList().doOnNext(e -> {
				e.forEach(g -> {
					setGuildId(g.getId());
				});
			}).block(Duration.ofSeconds(10));
		}
		return guildId;
	}

	private void setGuildId(final Snowflake guildId) {
		this.guildId = guildId;
	}

}
