package com.justfors.ddaodiscordbot.service;

import static java.lang.String.format;

import com.justfors.ddaodiscordbot.model.DdaoUser;
import com.justfors.ddaodiscordbot.repository.DdaoUserRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

@Slf4j
@Service
public class SchedulerService {

	@Value("${role.shrimp}")
	private String roleShrimp;
	@Value("${role.shark}")
	private String roleShark;
	@Value("${role.whale}")
	private String roleWhale;

	private static final Credentials CREDS = Credentials.create("1", "1");
	private static final Web3j CLIENT = Web3j.build(new HttpService("https://matic-mainnet.chainstacklabs.com"));

	@Value("${contractLevel1Address}")
	private String contractLevel1Address;
	@Value("${contractLevel2Address}")
	private String contractLevel2Address;
	@Value("${contractLevel3Address}")
	private String contractLevel3Address;

	private ERC20 contractLevel1 = null;
	private ERC20 contractLevel2 = null;
	private ERC20 contractLevel3 = null;

	@SneakyThrows
	@PostConstruct
	public void init() {
		contractLevel1 = ERC20.load(contractLevel1Address, CLIENT, CREDS, new DefaultGasProvider());
		contractLevel2 = ERC20.load(contractLevel2Address, CLIENT, CREDS, new DefaultGasProvider());
		contractLevel3 = ERC20.load(contractLevel3Address, CLIENT, CREDS, new DefaultGasProvider());
	}

	private final DdaoUserRepository ddaoUserRepository;
	private final GatewayDiscordClient client;

	public SchedulerService(
			final DdaoUserRepository ddaoUserRepository,
			final GatewayDiscordClient client) {
		this.ddaoUserRepository = ddaoUserRepository;
		this.client = client;
	}

	private static Snowflake guildId;
	private static Map<Long, Pair<Member, List<Role>>> USER_ROLES = new ConcurrentHashMap();
	private static List<Role> ROLES = new ArrayList<>();

	@Scheduled(cron = "0 * * * * *")
	public void refreshDiscrodMembers() {
		if (getGuildId() == null) {
			client.getGuilds().collectList().doOnNext(e -> {
				e.forEach(g -> {
					setGuildId(g.getId());
				});
			}).subscribe();
		}
		if (guildId != null) {
			if (ROLES.isEmpty()) {
				ROLES = client.getGuildRoles(guildId).collectList().block();
			}
			refreshUserList();
			checkWalletAssign();
			refreshUserRoles();
		}
	}

	private void refreshUserList() {
		log.info("started refreshUserList");
		List<Member> members = client.requestMembers(guildId).collectList().block();
		if (members != null) {
			members.forEach(m -> {
				if (USER_ROLES.get(m.getId().asLong()) == null) {
					USER_ROLES.put(m.getId().asLong(), Pair.of(m, m.getRoles().collectList().block()));
				}
			});
			USER_ROLES.forEach((k,v) -> {
				var exists = new AtomicBoolean(false);
				for (Member member : members) {
					if (k.equals(member.getId().asLong())) {
						exists.set(true);
						break;
					}
				}
				if (!exists.get()) {
					USER_ROLES.remove(k);
				}
			});
		}
		log.info("finished refreshUserList");
	}

	@Transactional
	public void checkWalletAssign() {
		log.info("started checkWalletAssign");
		List<Member> members = client.requestMembers(guildId).collectList().block();
		if (members != null) {
			members.forEach(m -> {
				var ddaoUser = ddaoUserRepository.getByDiscrodID(m.getId().asLong());
				if (ddaoUser != null) {
					if (StringUtils.isNotEmpty(ddaoUser.getWalletAddress()) && !ddaoUser.isWalletConfirm()){
						ddaoUser.setWalletConfirm(true);
						ddaoUserRepository.save(ddaoUser);
						m.getPrivateChannel().block().createMessage("Your wallet successfully added.").block();
					}
				}
			});
		}
		log.info("finished checkWalletAssign");
	}

	private void refreshUserRoles() {
		log.info("started refreshUserRoles");
		Map<Long, DdaoUser> userBalances = ddaoUserRepository.findAll().stream().collect(Collectors.toMap(DdaoUser::getDiscordId, Function.identity(), (v1, v2) -> v1));
		USER_ROLES.forEach((k,v) -> {
			var ddaoUser = userBalances.get(k);
			if (ddaoUser != null) {
				if (StringUtils.isNotEmpty(ddaoUser.getWalletAddress())) {
					try {
						boolean isLvl1 = contractLevel1.balanceOf(ddaoUser.getWalletAddress()).sendAsync().get().intValue() > 0;
						boolean isLvl2 = contractLevel2.balanceOf(ddaoUser.getWalletAddress()).sendAsync().get().intValue() > 0;
						boolean isLvl3 = contractLevel3.balanceOf(ddaoUser.getWalletAddress()).sendAsync().get().intValue() > 0;
						if (isLvl1) {addRole(v, roleShrimp);} else { if (ddaoUser.isRemovable()) {removeRole(v, roleShrimp);}}
						if (isLvl2) {addRole(v, roleShark);} else { if (ddaoUser.isRemovable()) {removeRole(v, roleShark);}}
						if (isLvl3) {addRole(v, roleWhale);} else {if (ddaoUser.isRemovable()) {removeRole(v, roleWhale);}}
						ddaoUser.setLevel1(isLvl1);
						ddaoUser.setLevel2(isLvl2);
						ddaoUser.setLevel3(isLvl3);
						ddaoUserRepository.save(ddaoUser);
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
			}
		});
		log.info("started refreshUserRoles");
	}

	private void addRole(Pair<Member, List<Role>> pair, String role){
		if (!isRoleExists(pair.getValue(), role)) {
			pair.getKey().addRole(Snowflake.of(getRoleId(role))).block();
			pair.getValue().add(getRoleByName(ROLES, role));
			log.info(format("Roles %s added to user %s", role, pair.getKey().getUsername()));
		}
	}

	private void removeRole(Pair<Member, List<Role>> pair, String role){
		if (isRoleExists(pair.getValue(), role)) {
			pair.getKey().removeRole(Snowflake.of(getRoleId(role))).block();
			pair.getValue().remove(getRoleByName(pair.getValue(), role));
			log.info(format("Role %s removed from user %s", role, pair.getKey().getUsername()));
		}
	}

	private boolean isRoleExists(List<Role> roles, String roleName) {
		return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName.toLowerCase()));
	}

	private Role getRoleByName(List<Role> roles, String roleName) {
		Role result = null;
		for (Role role : roles) {
			if (role.getName().equalsIgnoreCase(roleName.toLowerCase())) {
				result = role;
				break;
			}
		}
		return result;
	}

	private long getRoleId(String roleName) {
		var roleId = new AtomicLong(0);
		for (Role role : ROLES) {
			if (role.getName().equalsIgnoreCase(roleName.toLowerCase())) {
				roleId.set(role.getId().asLong());
				break;
			}
		}
		return roleId.get();
	}

	private Snowflake getGuildId() {
		return guildId;
	}

	private void setGuildId(final Snowflake guildId) {
		this.guildId = guildId;
	}

}
