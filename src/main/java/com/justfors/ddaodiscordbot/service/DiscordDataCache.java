package com.justfors.ddaodiscordbot.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.tuple.Pair;

public final class DiscordDataCache {

	private DiscordDataCache() {}

	private static Snowflake guildId = null;
	public static final Map<Long, Pair<Member, List<Role>>> USER_ROLES = new ConcurrentHashMap();
	public static final List<Role> ROLES = new ArrayList<>();

	public static Snowflake getGuildId() {
		return guildId;
	}

	public static void setGuildId(final Snowflake guildId) {
		DiscordDataCache.guildId = guildId;
	}
}
