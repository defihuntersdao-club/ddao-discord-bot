package com.justfors.ddaodiscordbot.service;

import static java.lang.String.format;

import com.justfors.ddaodiscordbot.listener.EventListener;
import com.justfors.ddaodiscordbot.listener.MessageListener;
import com.justfors.ddaodiscordbot.model.DdaoUser;
import com.justfors.ddaodiscordbot.repository.DdaoUserRepository;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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
	@Value("${tg.bind.button}")
	private String tgBindButton;
	@Value("${bitbrain.verification.button}")
	private String bitbrainVerificationButton;
	@Value("${tg.bind.prefix}")
	private String tgBindPrefix;

	private String errorMsgCantSend = "Cannot send messages to this user";
	private String errorMsgCantSendResponse = "Hi, I cannot send you a message.\n"
			+ "Please enable direct messages sending in your settings.\n"
			+ "\n"
			+ "You can disable it after the verification is completed.";
	private String errorMsgSmthWntWrng = "Oops, something went wrong, contact the support.";

	public ButtonInteractionListener(final DdaoUserRepository ddaoUserRepository) {
		this.ddaoUserRepository = ddaoUserRepository;
	}

	@Override
	@Transactional
	public Mono<Void> execute(ButtonInteractionEvent event) {
		try {
			Message message = event.getMessage().get();
			var member = event.getInteraction().getMember().orElse(null);
			if (member != null) {
				log.info(format("button clicked by discordId %s", member.getId().asLong()));
				var ddaoUser = ddaoUserRepository.getByDiscordID(member.getId().asLong());
				if (ddaoUser == null) {
					ddaoUser = ddaoUserRepository.save(createDdaoUser(member));
					log.info(format("New user added %s", ddaoUser.getUserName()));
				}
				var guild = message.getGuild().block(Duration.ofSeconds(10));
				if (guild != null) {
					var privateChannel = member.getPrivateChannel().block(Duration.ofSeconds(10));
					if (event.getCustomId().equals(walletBindButton)) {
						privateChannel.createMessage(
								"Follow this link to bind your wallet to Discord:\n" + walletConfirmationLink + ddaoUser.getUuid())
								.block(Duration.ofSeconds(10));
						log.info(format("Link sent to user %s", ddaoUser.getUserName()));
						event.reply(InteractionApplicationCommandCallbackSpec.builder()
								.content("Hi, check your DM, I've sent you a link.	")
								.ephemeral(true)
								.build()).block(Duration.ofSeconds(10));
					} else if (event.getCustomId().equals(tgBindButton)) {
						var secretCode = tgBindPrefix + RandomStringUtils.randomAlphanumeric(10);
						privateChannel.createMessage(
								format("Steps to bind your TG and Discord:\n"
										+ "\n"
										+ "1. Сopy this ➡️ %s\n"
										+ "2. Paste it to https://t.me/ddao_info_bot", secretCode))
								.block(Duration.ofSeconds(10));
						log.info(format("Code sent to user %s", ddaoUser.getUserName()));
						event.reply(InteractionApplicationCommandCallbackSpec.builder()
								.content("I've sent you the secret code for telegram verification.")
								.ephemeral(true)
								.build()).block(Duration.ofSeconds(10));
						ddaoUserRepository.setTelegramCode(secretCode, ddaoUser.getId());
					} else if (event.getCustomId().equals(bitbrainVerificationButton)) {
						privateChannel.createMessage(
										"Please copy the code that you received in your email in the following format ➡️ bitbrain:xxxxx\n"
												+ "and paste it here")
								.block(Duration.ofSeconds(10));
						log.info(format("Sent bitbrain verification instruction to user %s", ddaoUser.getUserName()));
						event.reply(InteractionApplicationCommandCallbackSpec.builder()
								.content("Hi, check your DM, I've sent the instructions.")
								.ephemeral(true)
								.build()).block(Duration.ofSeconds(10));
					}
				}
			}
		} catch (Throwable e) {
			log.error("Unable to process " + getEventType().getSimpleName(), e);
			boolean isErrorMsgCantSend = e.getMessage().contains(errorMsgCantSend);
			event.reply(InteractionApplicationCommandCallbackSpec.builder()
					.addEmbed(EmbedCreateSpec.builder()
							.description(isErrorMsgCantSend ? errorMsgCantSendResponse : errorMsgSmthWntWrng)
							.image(isErrorMsgCantSend ? "https://i.imgur.com/xXzmVEs.png" : "https://t3.ftcdn.net/jpg/02/01/43/66/360_F_201436679_ZCLSEuwhRvmQEVofXHPpvLeV5sBLQ3vp.jpg")
							.build())
					.ephemeral(true)
					.build()).block(Duration.ofSeconds(10));
		}
		return Mono.empty();
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
