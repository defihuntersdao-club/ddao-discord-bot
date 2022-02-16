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
import java.util.Arrays;
import java.util.List;
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

	private List<Long> adminIds = Arrays.asList(
			820699473503846471L, //max
			779257124487561236L, //slava
			839890090191749142L, //hamster
			465421098918477833L  //me
	);

	@Override
	@Transactional
	public Mono<Void> execute(MessageCreateEvent event) {
		Message message = event.getMessage();
		var guild = message.getGuild().block(Duration.ofSeconds(10));
		var author = message.getAuthor().orElse(null);
		if (guild == null && author != null) {
			var ddaoUser = ddaoUserRepository.getByDiscordID(author.getId().asLong());
			if (ddaoUser != null) {
				if (message.getContent() != null) {
					if (message.getContent().contains("bitbrain:")) {
						var privateChannel = author.getPrivateChannel().block(Duration.ofSeconds(10));
						Bitbrain code = bitbrainRepository.findByCode(message.getContent());
						if (code != null && code.getStatus() == 0) {
							privateChannel.createMessage(successMsg).block(Duration.ofSeconds(10));
							ddaoUserRepository.setDirectLobsterAccessTrue(ddaoUser.getId());
							bitbrainRepository.exprireStatus(code.getId());
							log.info(format("Bitbrain validation for user %s successfully completed with code %s",
									ddaoUser.getUserName(), code.getCode()));
						} else {
							privateChannel.createMessage(errMsg).block(Duration.ofSeconds(10));
							log.info(format("Code %s not found", message.getContent()));
						}
					}
				}
			} else {

			}
			if (message.getContent().contains("check:") && adminIds.contains(author.getId().asLong())) {
				var privateChannel = author.getPrivateChannel().block(Duration.ofSeconds(10));
				log.info("CHECK USER from: " + author.getId().asLong());
				var userNickNameWithDiscriminator = message.getContent().replace("check:", "");
				var userInfos = userNickNameWithDiscriminator.split("#");
				var ddaoUserInfo = ddaoUserRepository.findByUserNameAndDiscriminator(userInfos[0], userInfos[1]);
				log.info("Trying to check user details");
				if (ddaoUserInfo != null) {
					var resultMessage = format("""
												User %s have
																				
												wallet: %s
												telegram_confirm: %s
												is_direct_lobster_access: %s
												""",
							userNickNameWithDiscriminator,
							ddaoUserInfo.getWalletAddress(),
							ddaoUserInfo.isTelegramConfirm(),
							ddaoUserInfo.isDirectLobsterAccess());
					privateChannel.createMessage(resultMessage).block(Duration.ofSeconds(10));
					log.info(format("requested data for user is %s", resultMessage));
				} else {
					var resultMessage = format("User %s is not found in database.", userNickNameWithDiscriminator);
					privateChannel.createMessage(resultMessage).block(Duration.ofSeconds(10));
					log.info(format("requested data for user is %s", resultMessage));
				}
				log.info("User details processed");
			}
		}
		return Mono.empty();
	}
}
