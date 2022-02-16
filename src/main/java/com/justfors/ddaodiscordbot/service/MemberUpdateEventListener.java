package com.justfors.ddaodiscordbot.service;

import static java.lang.String.format;

import com.justfors.ddaodiscordbot.listener.EventListener;
import com.justfors.ddaodiscordbot.listener.MessageListener;
import com.justfors.ddaodiscordbot.repository.DdaoUserRepository;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * @author Alexander Sosnovsky
 */
@Slf4j
@Service
public class MemberUpdateEventListener extends MessageListener implements EventListener<MemberUpdateEvent> {

	@Override
	public Class<MemberUpdateEvent> getEventType() {
		return MemberUpdateEvent.class;
	}

	private final DdaoUserRepository ddaoUserRepository;

	public MemberUpdateEventListener(
			final DdaoUserRepository ddaoUserRepository) {
		this.ddaoUserRepository = ddaoUserRepository;
	}

	@Override
	@Transactional
	public Mono<Void> execute(MemberUpdateEvent event) {
		var member = event.getMember().block(Duration.ofSeconds(10));
		if (member != null) {
			var ddaoUser = ddaoUserRepository.getByDiscordID(member.getId().asLong());
			if (ddaoUser != null) {
				ddaoUserRepository.updateNickName(member.getUsername(), member.getDiscriminator(), ddaoUser.getId());
				log.info(format("Users %s changed", ddaoUser.getDiscordId()));
			} else {

			}
		}
		return Mono.empty();
	}
}