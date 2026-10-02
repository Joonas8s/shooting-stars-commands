package com.shootingstarscommands;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ShootingStarsConfig.GROUP)
public interface ShootingStarsConfig extends Config
{
	String GROUP = "shooting-stars-commands";

	@ConfigSection(
		name = "Star Miners feed",
		description = "Settings for the optional third-party Shooting Stars feed.",
		position = 0
	)
	String feedSection = "feedSection";

	@ConfigItem(
		keyName = "useStarMinersFeed",
		name = "Use Star Miners feed",
		description = "Retrieve active stars from the Star Miners crowdsourcing service.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 1,
		section = feedSection
	)
	default boolean useStarMinersFeed()
	{
		return false;
	}

	@ConfigItem(
		keyName = "authorizationKey",
		name = "Star Miners authorization key",
		description = "The Authorization header value supplied by Star Miners. No key is bundled with the plugin.",
		secret = true,
		position = 2,
		section = feedSection
	)
	default String authorizationKey()
	{
		return "";
	}

}
