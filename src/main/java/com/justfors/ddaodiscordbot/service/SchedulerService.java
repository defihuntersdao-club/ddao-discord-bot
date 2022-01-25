package com.justfors.ddaodiscordbot.service;

import static java.lang.String.format;

import com.justfors.ddaodiscordbot.model.DdaoUser;
import com.justfors.ddaodiscordbot.repository.DdaoUserRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
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
	@Value("${role.hamster}")
	private String roleHamster;

	private static final CircularFifoQueue<String> NODE_URLS = new CircularFifoQueue();

	private static final Credentials CREDENTIALS = Credentials.create("1", "1");

	@Value("${contractLevel1Address}")
	private String contractLevel1Address;
	@Value("${contractLevel2Address}")
	private String contractLevel2Address;
	@Value("${contractLevel3Address}")
	private String contractLevel3Address;
	@Value("${contractHamsterAddress}")
	private String contractHamsterAddress;

	private ERC20 contractLevel1 = null;
	private ERC20 contractLevel2 = null;
	private ERC20 contractLevel3 = null;
	private ERC20 contractHamster = null;

	@SneakyThrows
	@PostConstruct
	public void init() {
		NODE_URLS.add("https://rpc-mainnet.matic.network");
		NODE_URLS.add("https://matic-mainnet.chainstacklabs.com");
		NODE_URLS.add("https://rpc-mainnet.maticvigil.com");
		NODE_URLS.add("https://rpc-mainnet.matic.quiknode.pro");
		NODE_URLS.add("https://matic-mainnet-full-rpc.bwarelabs.com");

		Web3j web3j = Web3j.build(new HttpService(getNode()));

		contractLevel1 = ERC20.load(contractLevel1Address, web3j, CREDENTIALS, new DefaultGasProvider());
		contractLevel2 = ERC20.load(contractLevel2Address, web3j, CREDENTIALS, new DefaultGasProvider());
		contractLevel3 = ERC20.load(contractLevel3Address, web3j, CREDENTIALS, new DefaultGasProvider());
		contractHamster = ERC20.load(contractHamsterAddress, web3j, CREDENTIALS, new DefaultGasProvider());
	}

	private void reinit(){
		Web3j web3j = Web3j.build(new HttpService(getNode()));

		contractLevel1 = ERC20.load(contractLevel1Address, web3j, CREDENTIALS, new DefaultGasProvider());
		contractLevel2 = ERC20.load(contractLevel2Address, web3j, CREDENTIALS, new DefaultGasProvider());
		contractLevel3 = ERC20.load(contractLevel3Address, web3j, CREDENTIALS, new DefaultGasProvider());
		contractHamster = ERC20.load(contractHamsterAddress, web3j, CREDENTIALS, new DefaultGasProvider());
	}

	private String getNode() {
		String currentNodeUrl = NODE_URLS.poll();
		NODE_URLS.add(currentNodeUrl);
		log.info(format("Current Node is %s", currentNodeUrl));
		return currentNodeUrl;
	}

	private final DdaoUserRepository ddaoUserRepository;
	private final GatewayDiscordClient discordClient;

	public SchedulerService(
			final DdaoUserRepository ddaoUserRepository,
			final GatewayDiscordClient discordClient) {
		this.ddaoUserRepository = ddaoUserRepository;
		this.discordClient = discordClient;
	}

	private static Map<Long, Pair<Member, List<Role>>> USER_ROLES = new ConcurrentHashMap();
	private static List<Role> ROLES = new ArrayList<>();

	@Scheduled(cron = "0 * * * * *")
	public void refreshDiscrodMembers() {
		var guildId = getGuildId();
		if (guildId != null) {
			if (ROLES.isEmpty()) {
				var actualRoles = discordClient.getGuildRoles(guildId).collectList().block(Duration.ofSeconds(10));
				ROLES = actualRoles != null ? actualRoles : ROLES;
			}
			List<Member> members = discordClient.requestMembers(guildId).collectList().block(Duration.ofSeconds(10));
			refreshUserList(members);
			checkWalletAssign(members);
			refreshUserRoles();
		}
	}

//	TODO: create separated listener on users events, to reduce load on @Schedule
//	MemberJoinEvent: a user has joined a guild
//	MemberLeaveEvent: a user has left or was kicked from a guild
//	MemberUpdateEvent: a user had their nickname and/or roles change
	private void refreshUserList(List<Member> members) {
		log.info("started refreshUserList");
		if (members != null) {
			members.forEach(m -> {
				if (USER_ROLES.get(m.getId().asLong()) == null) {
					USER_ROLES.put(m.getId().asLong(), Pair.of(m, m.getRoles().collectList().block(Duration.ofSeconds(10))));
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
	public void checkWalletAssign(List<Member> members) {
		log.info("started checkWalletAssign");
		if (members != null) {
			members.forEach(m -> {
				var ddaoUser = ddaoUserRepository.getByDiscordID(m.getId().asLong());
				if (ddaoUser != null) {
					if (StringUtils.isNotEmpty(ddaoUser.getWalletAddress()) && !ddaoUser.isWalletConfirm()){
						ddaoUser.setWalletConfirm(true);
						ddaoUserRepository.save(ddaoUser);
						var channel = m.getPrivateChannel().block(Duration.ofSeconds(10));
						if (channel != null) {
							channel.createMessage("Your wallet successfully added.").block(Duration.ofSeconds(10));
						} else {
							log.info("couldn't get a channel to send the link message.");
						}
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
						boolean isLvl1 = contractLevel1.balanceOf(ddaoUser.getWalletAddress()).sendAsync().get(10, TimeUnit.SECONDS).intValue() > 0;
						boolean isLvl2 = contractLevel2.balanceOf(ddaoUser.getWalletAddress()).sendAsync().get(10, TimeUnit.SECONDS).intValue() > 0;
						boolean isLvl3 = contractLevel3.balanceOf(ddaoUser.getWalletAddress()).sendAsync().get(10, TimeUnit.SECONDS).intValue() > 0;
						boolean isHamster = ddaoUser.isTelegramConfirm() ?
								ddaoUser.isTelegramConfirm() :
								contractHamster.balanceOf(ddaoUser.getWalletAddress()).sendAsync().get(10, TimeUnit.SECONDS).intValue() > 0;
						if (isLvl1) {addRole(v, roleShrimp);} else { if (ddaoUser.isRemovable()) {removeRole(v, roleShrimp);}}
						if (isLvl2) {addRole(v, roleShark);} else { if (ddaoUser.isRemovable()) {removeRole(v, roleShark);}}
						if (isLvl3) {addRole(v, roleWhale);} else {if (ddaoUser.isRemovable()) {removeRole(v, roleWhale);}}
						if (isHamster) {addRole(v, roleHamster);} else {if (ddaoUser.isRemovable()) {removeRole(v, roleHamster);}}
						ddaoUser.setLevel1(isLvl1);
						ddaoUser.setLevel2(isLvl2);
						ddaoUser.setLevel3(isLvl3);
						ddaoUser.setHamster(isHamster);
						ddaoUserRepository.save(ddaoUser);
					} catch (TimeoutException e) {
						log.error("Got an timeout exception, trying to change node for contract check.");
						reinit();
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
			}
		});
		log.info("finished refreshUserRoles");
	}

	private void addRole(Pair<Member, List<Role>> pair, String role){
		if (!isRoleExists(pair.getValue(), role)) {
			log.info(format("Trying to add role %s to user %s", role, pair.getKey().getUsername()));
			pair.getKey().addRole(Snowflake.of(getRoleId(role))).block(Duration.ofSeconds(10));
			pair.getValue().add(getRoleByName(ROLES, role));
			log.info(format("Role %s added to user %s", role, pair.getKey().getUsername()));
		}
	}

	private void removeRole(Pair<Member, List<Role>> pair, String role){
		if (isRoleExists(pair.getValue(), role)) {
			log.info(format("Trying to remove role %s to user %s", role, pair.getKey().getUsername()));
			pair.getKey().removeRole(Snowflake.of(getRoleId(role))).block(Duration.ofSeconds(10));
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
		if (DiscordDataCache.getGuildId() == null) {
			discordClient.getGuilds().collectList().doOnNext(e -> {
				e.forEach(g -> {
					DiscordDataCache.setGuildId(g.getId());
				});
			}).block(Duration.ofSeconds(10));
		}
		return DiscordDataCache.getGuildId();
	}

}
