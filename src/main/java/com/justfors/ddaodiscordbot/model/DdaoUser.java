package com.justfors.ddaodiscordbot.model;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ddao_users")
@Getter
@Setter
public class DdaoUser {
	@Id
	@GeneratedValue
	private long id;

	private String userName;
	private String discriminator;
	private long discordId;

	private boolean removable = true;

	private String uuid;

	private boolean level1 = false;
	private boolean level2 = false;
	private boolean level3 = false;
	private boolean hamster = false;

	private String walletAddress;
	private boolean walletConfirm = false;
}
