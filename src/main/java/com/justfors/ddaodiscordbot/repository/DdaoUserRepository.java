package com.justfors.ddaodiscordbot.repository;

import com.justfors.ddaodiscordbot.model.DdaoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DdaoUserRepository extends JpaRepository<DdaoUser, Long> {

	@Query("select e from DdaoUser e where e.discordId = :discordId")
	DdaoUser getByDiscordID(Long discordId);

	@Query("select e from DdaoUser e where e.telegramId = :tgId")
	DdaoUser findByTGID(Long tgId);

	@Query("select e from DdaoUser e where e.userName = :userName and e.discriminator = :discriminator")
	DdaoUser findByUserNameAndDiscriminator(String userName, String discriminator);

	@Modifying
	@Query("update DdaoUser u set u.level1 = :level1, u.level2 = :level2, u.level3 = :level3, u.hamster = :hamster where u.id = :ddaoUserId")
	void updateRoles(boolean level1, boolean level2, boolean level3, boolean hamster, Long ddaoUserId);

	@Modifying
	@Query("update DdaoUser u set u.walletConfirm = true where u.id = :ddaoUserId")
	void setWalletConfirm(Long ddaoUserId);

	@Modifying
	@Query("update DdaoUser u set u.telegramCode = :telegramCode where u.id = :ddaoUserId")
	void setTelegramCode(String telegramCode, Long ddaoUserId);

	@Modifying
	@Query("update DdaoUser u set u.directLobsterAccess = true where u.id = :ddaoUserId")
	void setDirectLobsterAccessTrue(Long ddaoUserId);


	@Modifying
	@Query("update DdaoUser u set u.userName = :userName, u.discriminator = :discriminator where u.id = :ddaoUserId")
	void updateNickName(String userName, String discriminator, Long ddaoUserId);
}
