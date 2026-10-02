package com.shootingstarscommands;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class ShootingStarsPluginFormattingTest
{
	@Test
	public void formatsCompactStarListWithLocations()
	{
		String formatted = ShootingStarsPlugin.formatStars(Arrays.asList(
			new Star(533, 9, "West Lumbridge Swamp", 1L),
			new Star(330, 8, "Al Kharid", 1L)));

		assertEquals("Stars: T9 W533 (West Lumbridge Swamp) | T8 W330 (Al Kharid)", formatted);
	}

	@Test
	public void formatsEmptyStarList()
	{
		assertEquals("No active stars were found.", ShootingStarsPlugin.formatStars(Collections.emptyList()));
	}

	@Test
	public void identifiesAnEmptyFilteredList()
	{
		StarFilter filter = StarFilter.fromCommand("!stars t7 varrock");

		assertEquals("No active stars matched T7, Varrock.",
			ShootingStarsPlugin.formatStars(Collections.emptyList(), filter));
	}

	@Test
	public void recognizesHelpCommands()
	{
		assertEquals(true, ShootingStarsPlugin.isHelpCommand("!stars help"));
		assertEquals(true, ShootingStarsPlugin.isHelpCommand("!stars commands"));
	}
}
