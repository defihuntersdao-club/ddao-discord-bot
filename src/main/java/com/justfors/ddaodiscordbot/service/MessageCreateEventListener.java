package com.justfors.ddaodiscordbot.service;

import static java.lang.String.format;

import com.justfors.ddaodiscordbot.listener.EventListener;
import com.justfors.ddaodiscordbot.listener.MessageListener;
import com.justfors.ddaodiscordbot.model.Bitbrain;
import com.justfors.ddaodiscordbot.repository.BitbrainRepository;
import com.justfors.ddaodiscordbot.repository.DdaoUserRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class MessageCreateEventListener extends MessageListener implements EventListener<MessageCreateEvent> {

	@Override
	public Class<MessageCreateEvent> getEventType() {
		return MessageCreateEvent.class;
	}

	private final DdaoUserRepository ddaoUserRepository;
	private final BitbrainRepository bitbrainRepository;

	private String successMsg = "Success! Verification is completed.";
	private String errMsg = "Sorry, something went wrong. Please make sure you copied the code correctly";

	public MessageCreateEventListener(
			final DdaoUserRepository ddaoUserRepository,
			final BitbrainRepository bitbrainRepository) {
		this.ddaoUserRepository = ddaoUserRepository;
		this.bitbrainRepository = bitbrainRepository;
	}

	@Override
	@Transactional
	public Mono<Void> execute(MessageCreateEvent event) {
		Message message = event.getMessage();
		var guild = message.getGuild().block(Duration.ofSeconds(10));
		var author = message.getAuthor().orElse(null);
		if (guild == null && author != null) {
			var ddaoUser = ddaoUserRepository.getByDiscordID(author.getId().asLong());
			if (ddaoUser != null) {
				var privateChannel = author.getPrivateChannel().block(Duration.ofSeconds(10));
				if (message.getContent() != null && message.getContent().contains("bitbrain:")) {
					Bitbrain code = bitbrainRepository.findByCode(message.getContent());
					if (code != null && code.getStatus() == 0) {
						privateChannel.createMessage(successMsg).block(Duration.ofSeconds(10));
						ddaoUserRepository.setDirectLobsterAccessTrue(ddaoUser.getId());
						bitbrainRepository.exprireStatus(code.getId());
						log.info(format("Bitbrain validation for user %s successfully completed", ddaoUser.getUserName()));
					} else {
						privateChannel.createMessage(errMsg).block(Duration.ofSeconds(10));
						log.info(format("Code %s not found", message.getContent()));
					}
				}
			} else {

			}
		}
		return Mono.empty();
	}
}
