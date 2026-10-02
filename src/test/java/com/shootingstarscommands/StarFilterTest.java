package com.shootingstarscommands;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class StarFilterTest
{
	private static final List<Star> STARS = Arrays.asList(
		new Star(330, 9, "Varrock Mine", 1L),
		new Star(531, 7, "Varrock Palace", 1L),
		new Star(533, 7, "West Lumbridge Swamp", 1L),
		new Star(434, 6, "Rimmington", 1L));

	@Test
	public void filtersAnExactTier()
	{
		StarFilter filter = StarFilter.fromCommand("!stars t7");

		assertEquals("T7", filter.description());
		assertEquals(2, filter.apply(STARS).size());
	}

	@Test
	public void combinesMinimumTierAndLocation()
	{
		StarFilter filter = StarFilter.fromCommand("!stars t7+ varrock");

		assertEquals("T7+, Varrock", filter.description());
		assertEquals(2, filter.apply(STARS).size());
	}

	@Test
	public void filtersLocationCaseInsensitively()
	{
		StarFilter filter = StarFilter.fromCommand("!stars LUMBRIDGE");

		assertEquals("LUMBRIDGE", filter.description());
		assertEquals(1, filter.apply(STARS).size());
	}
}
