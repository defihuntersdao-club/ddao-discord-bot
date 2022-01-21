package com.justfors.ddaodiscordbot.service;

import com.justfors.ddaodiscordbot.listener.EventListener;
import com.justfors.ddaodiscordbot.listener.MessageListener;
import com.justfors.ddaodiscordbot.model.DdaoUser;
import com.justfors.ddaodiscordbot.repository.DdaoUserRepository;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
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
public class ButtonInteractionListener extends MessageListener implements EventListener<ButtonInteractionEvent> {

	@Override
	public Class<ButtonInteractionEvent> getEventType() {
		return ButtonInteractionEvent.class;
	}

	private final DdaoUserRepository ddaoUserRepository;

	@Value("${wallet.confirmation.link}")
	private String walletConfirmationLink;
	@Value("${wallet.bind.button}")
	private String walletBindButton;

	public ButtonInteractionListener(final DdaoUserRepository ddaoUserRepository) {
		this.ddaoUserRepository = ddaoUserRepository;
	}

	@Override
	@Transactional
	public Mono<Void> execute(ButtonInteractionEvent event) {
		Message message = event.getMessage().get();
		var member = event.getInteraction().getMember().orElse(null);
		if (member != null) {
			var ddaoUser = ddaoUserRepository.getByDiscrodID(member.getId().asLong());
			if (ddaoUser == null) {
				ddaoUser = ddaoUserRepository.save(createDdaoUser(member));
			}
			var guild = message.getGuild().block();
			if (guild != null && event.getCustomId().equals(walletBindButton)) {
				member.getPrivateChannel().block().createMessage(walletConfirmationLink + ddaoUser.getUuid()).block();
			}
		}
		return processCommand(message);
	}

	private DdaoUser createDdaoUser(Member member) {
		DdaoUser user = new DdaoUser();

		user.setUserName(member.getUsername());
		user.setDiscriminator(member.getDiscriminator());
		user.setDiscordId(member.getId().asLong());

		user.setUuid(UUID.randomUUID().toString());

		return user;
	}
}
