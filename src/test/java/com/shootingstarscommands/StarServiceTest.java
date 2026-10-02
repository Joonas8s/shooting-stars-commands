package com.shootingstarscommands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import org.junit.Test;

public class StarServiceTest
{
	private static final Gson GSON = new Gson();
	private static final long NOW = 2_000_000L;

	@Test
	public void parsesAndSortsStarsByTierThenWorld()
	{
		String response = "["
			+ "{\"world\":533,\"tier\":8,\"calledLocation\":\"West Lumbridge Swamp\",\"calledAt\":1999999},"
			+ "{\"world\":330,\"tier\":9,\"calledLocation\":\"Al Kharid\",\"calledAt\":1999999},"
			+ "{\"world\":434,\"tier\":8,\"calledLocation\":\"Rimmington\",\"calledAt\":1999999}"
			+ "]";

		List<Star> stars = StarService.parseStars(GSON, response, NOW);

		assertEquals(3, stars.size());
		assertEquals(330, stars.get(0).getWorld());
		assertEquals(434, stars.get(1).getWorld());
		assertEquals(533, stars.get(2).getWorld());
		assertEquals("Al Kharid", stars.get(0).getLocation());
	}

	@Test
	public void dropsInvalidAndStaleEntries()
	{
		String response = "["
			+ "{\"world\":330,\"tier\":9,\"calledLocation\":\"Al Kharid\",\"calledAt\":1999999},"
			+ "{\"world\":533,\"tier\":0,\"calledAt\":1999999},"
			+ "{\"world\":434,\"tier\":7,\"calledAt\":1000000},"
			+ "{\"world\":445,\"tier\":7,\"calledAt\":1999999}"
			+ "]";

		List<Star> stars = StarService.parseStars(GSON, response, NOW);

		assertEquals(2, stars.size());
		assertTrue(stars.get(1).getLocation().isEmpty());
	}

	@Test
	public void appliesCooldownAfterAnyRequest()
	{
		assertTrue(StarService.isRequestCooldownActive(1_000L, 1_000L + StarService.REQUEST_COOLDOWN_MILLIS - 1));
		assertEquals(false, StarService.isRequestCooldownActive(1_000L, 1_000L + StarService.REQUEST_COOLDOWN_MILLIS));
	}

	@Test
	public void rejectsOversizedResponses()
	{
		ResponseBody body = new ResponseBody()
		{
			@Override
			public MediaType contentType()
			{
				return null;
			}

			@Override
			public long contentLength()
			{
				return -1L;
			}

			@Override
			public BufferedSource source()
			{
				return new Buffer().write(new byte[(int) StarService.MAX_RESPONSE_SIZE_BYTES + 1]);
			}
		};
		try
		{
			StarService.readResponseBody(body);
			fail("Expected oversized responses to be rejected");
		}
		catch (IOException expected)
		{
			assertTrue(expected.getMessage().contains("size limit"));
		}
		finally
		{
			body.close();
		}
	}
}
