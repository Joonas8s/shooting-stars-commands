package com.shootingstarscommands;

import java.util.Objects;

public final class Star
{
	private final int world;
	private final int tier;
	private final String location;
	private final long lastUpdated;

	public Star(int world, int tier, String location, long lastUpdated)
	{
		this.world = world;
		this.tier = tier;
		this.location = Objects.requireNonNull(location);
		this.lastUpdated = lastUpdated;
	}

	public int getWorld()
	{
		return world;
	}

	public int getTier()
	{
		return tier;
	}

	public String getLocation()
	{
		return location;
	}

	public long getLastUpdated()
	{
		return lastUpdated;
	}
}
