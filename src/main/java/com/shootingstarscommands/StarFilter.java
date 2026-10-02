package com.shootingstarscommands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.util.Text;

final class StarFilter
{
	private static final Pattern TIER_FILTER = Pattern.compile("(?i)^t?([1-9])(\\+)?(?:\\s+(.+))?$");
	private static final StarFilter ALL = new StarFilter(0, false, "");

	private final int tier;
	private final boolean minimumTier;
	private final String locationQuery;

	private StarFilter(int tier, boolean minimumTier, String locationQuery)
	{
		this.tier = tier;
		this.minimumTier = minimumTier;
		this.locationQuery = locationQuery;
	}

	static StarFilter fromCommand(String commandText)
	{
		if (commandText == null)
		{
			return ALL;
		}

		String plainText = Text.removeTags(commandText).trim();
		if (!plainText.regionMatches(true, 0, "!stars", 0, "!stars".length()))
		{
			return ALL;
		}

		String filterText = plainText.substring("!stars".length()).trim();
		if (filterText.isEmpty() || filterText.equalsIgnoreCase("all"))
		{
			return ALL;
		}

		Matcher tierMatcher = TIER_FILTER.matcher(filterText);
		if (tierMatcher.matches())
		{
			String location = normalizeLocation(tierMatcher.group(3));
			return new StarFilter(Integer.parseInt(tierMatcher.group(1)), tierMatcher.group(2) != null, location);
		}

		return new StarFilter(0, false, normalizeLocation(filterText));
	}

	List<Star> apply(List<Star> stars)
	{
		List<Star> matchingStars = new ArrayList<>();
		for (Star star : stars)
		{
			if (matches(star))
			{
				matchingStars.add(star);
			}
		}
		return matchingStars;
	}

	String description()
	{
		String tierDescription = tier == 0 ? "" : "T" + tier + (minimumTier ? "+" : "");
		if (locationQuery.isEmpty())
		{
			return tierDescription;
		}
		if (tierDescription.isEmpty())
		{
			return displayLocation();
		}
		return tierDescription + ", " + displayLocation();
	}

	boolean isAll()
	{
		return tier == 0 && locationQuery.isEmpty();
	}

	private boolean matches(Star star)
	{
		if (tier != 0 && (minimumTier ? star.getTier() < tier : star.getTier() != tier))
		{
			return false;
		}
		return locationQuery.isEmpty()
			|| star.getLocation().toLowerCase(Locale.ROOT).contains(locationQuery.toLowerCase(Locale.ROOT));
	}

	private static String normalizeLocation(String location)
	{
		return location == null ? "" : location.trim().replaceAll("\\s+", " ");
	}

	private String displayLocation()
	{
		return locationQuery.substring(0, 1).toUpperCase(Locale.ROOT) + locationQuery.substring(1);
	}
}
