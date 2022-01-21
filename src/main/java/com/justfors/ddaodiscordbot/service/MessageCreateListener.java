package com.justfors.ddaodiscordbot.service;

import com.justfors.ddaodiscordbot.listener.EventListener;
import com.justfors.ddaodiscordbot.listener.MessageListener;
import com.justfors.ddaodiscordbot.model.DdaoUser;
import com.justfors.ddaodiscordbot.repository.DdaoUserRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MessageCreateListener extends MessageListener implements EventListener<MessageCreateEvent> {

	@Override
	public Class<MessageCreateEvent> getEventType() {
		return MessageCreateEvent.class;
	}

	private final DdaoUserRepository ddaoUserRepository;

	@Value("${wallet.confirmation.link}")
	private String walletConfirmationLink;
	@Value("${bot.channel.name}")
	private String botChannelName;

	public MessageCreateListener(final DdaoUserRepository ddaoUserRepository) {
		this.ddaoUserRepository = ddaoUserRepository;
	}

	@Override
	@Transactional
	public Mono<Void> execute(MessageCreateEvent event) {
		Message message = event.getMessage();
		var member = message.getAuthorAsMember().block();
		if (member != null) {
			var ddaoUser = ddaoUserRepository.getByDiscrodID(member.getId().asLong());
			if (ddaoUser == null) {
				ddaoUser = ddaoUserRepository.save(createDdaoUser(member));
			}
			var guild = event.getGuild().block();
			if (guild != null && "/wallet-connect".equalsIgnoreCase(message.getContent())) {
				if (message.getChannel().block().getRestChannel().getData().block().name().get().contains(botChannelName)) {
					message.delete().block();
					member.getPrivateChannel().block().createMessage(walletConfirmationLink + ddaoUser.getUuid()).block();
				}
			}
		}
		return processCommand(message);
	}

	private DdaoUser createDdaoUser(Member member) {
		DdaoUser user = new DdaoUser();

		user.setUserName(member.getUsername());
		user.setDiscriminator(member.getDiscriminator());
		user.setDiscordId(member.getId().asLong());

		user.setUuid(UUID.randomUUID());

		return user;
	}
}
