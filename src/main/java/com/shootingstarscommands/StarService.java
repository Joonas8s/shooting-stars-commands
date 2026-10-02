package com.shootingstarscommands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Text;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

@Slf4j
@Singleton
public class StarService
{
	static final String STAR_MINERS_ENDPOINT = "https://public.starminers.site/crowdsource";
	private static final String PLUGIN_NAME = "Shooting Stars Commands";
	static final long CACHE_DURATION_MILLIS = 45_000L;
	static final long REQUEST_COOLDOWN_MILLIS = 45_000L;
	static final long MAX_RESPONSE_SIZE_BYTES = 64L * 1024L;
	private static final long MAX_REPORT_AGE_SECONDS = 90L * 60L;
	private static final long MAX_FUTURE_REPORT_SECONDS = 5L * 60L;

	private final OkHttpClient okHttpClient;
	private final Gson gson;
	private final ShootingStarsConfig config;
	private final Object requestLock = new Object();
	private final List<ResultCallback> pendingCallbacks = new ArrayList<>();

	private List<Star> cachedStars = Collections.emptyList();
	private long cachedAtMillis;
	private long lastRequestAtMillis;
	private Call activeCall;

	@Inject
	StarService(OkHttpClient okHttpClient, Gson gson, ShootingStarsConfig config)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
		this.config = config;
	}

	/**
	 * Gets active stars from the short-lived cache or queues one non-blocking HTTP request.
	 * Every callback is called from the caller thread for cached data, or an OkHttp thread for
	 * fresh data. Callers that touch RuneLite state must move that work to the client thread.
	 */
	public void fetchActiveStars(ResultCallback callback)
	{
		List<Star> cacheHit = null;
		List<ResultCallback> requestCreationFailureCallbacks = null;
		Call callToEnqueue = null;
		boolean requestIsCoolingDown = false;
		synchronized (requestLock)
		{
			long nowMillis = System.currentTimeMillis();
			if (cachedAtMillis > 0 && nowMillis - cachedAtMillis < CACHE_DURATION_MILLIS)
			{
				cacheHit = cachedStars;
			}
			else if (activeCall != null)
			{
				pendingCallbacks.add(callback);
				return;
			}
			else if (isRequestCooldownActive(lastRequestAtMillis, nowMillis))
			{
				requestIsCoolingDown = true;
			}
			else
			{
				pendingCallbacks.add(callback);
				lastRequestAtMillis = nowMillis;
				try
				{
					Request.Builder builder = new Request.Builder()
						.url(STAR_MINERS_ENDPOINT)
						.cacheControl(CacheControl.FORCE_NETWORK)
						.header("Plugin", PLUGIN_NAME);
					String authorizationKey = config.authorizationKey();
					if (authorizationKey != null && !authorizationKey.trim().isEmpty())
					{
						builder.header("Authorization", authorizationKey.trim());
					}
					callToEnqueue = okHttpClient.newCall(builder.build());
					activeCall = callToEnqueue;
				}
				catch (IllegalArgumentException e)
				{
					log.debug("Could not create Star Miners request", e);
					requestCreationFailureCallbacks = takePendingCallbacks();
				}
			}
		}

		if (cacheHit != null)
		{
			callback.onSuccess(cacheHit, true);
			return;
		}
		if (requestIsCoolingDown)
		{
			callback.onFailure("Shooting Stars were recently unavailable. Try again shortly.");
			return;
		}
		if (requestCreationFailureCallbacks != null)
		{
			notifyFailure(requestCreationFailureCallbacks, "Unable to retrieve Shooting Stars right now.");
			return;
		}

		if (callToEnqueue == null)
		{
			return;
		}

		callToEnqueue.enqueue(new okhttp3.Callback()
		{
			@Override
			public void onFailure(Call failedCall, IOException e)
			{
				log.debug("Star Miners request failed", e);
				completeFailure(failedCall, "Unable to retrieve Shooting Stars right now.");
			}

			@Override
			public void onResponse(Call responseCall, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						log.debug("Star Miners request returned HTTP {}", response.code());
						completeFailure(responseCall, response.code() == 401 || response.code() == 403
							? "Star Miners rejected the authorization key."
							: "Unable to retrieve Shooting Stars right now.");
						return;
					}

					List<Star> stars = parseStars(gson, readResponseBody(body), Instant.now().getEpochSecond());
					completeSuccess(responseCall, stars);
				}
				catch (IOException | JsonParseException | IllegalStateException e)
				{
					log.debug("Could not read Star Miners response", e);
					completeFailure(responseCall, "Unable to retrieve Shooting Stars right now.");
				}
			}
		});
	}

	static boolean isRequestCooldownActive(long lastRequestMillis, long nowMillis)
	{
		return lastRequestMillis > 0 && nowMillis - lastRequestMillis < REQUEST_COOLDOWN_MILLIS;
	}

	static String readResponseBody(ResponseBody body) throws IOException
	{
		long contentLength = body.contentLength();
		if (contentLength > MAX_RESPONSE_SIZE_BYTES)
		{
			throw new IOException("Star Miners response exceeded the size limit");
		}

		BufferedSource source = body.source();
		if (source.request(MAX_RESPONSE_SIZE_BYTES + 1))
		{
			throw new IOException("Star Miners response exceeded the size limit");
		}
		return source.readString(StandardCharsets.UTF_8);
	}

	public void cancelPendingRequest()
	{
		Call callToCancel;
		synchronized (requestLock)
		{
			callToCancel = activeCall;
			activeCall = null;
			pendingCallbacks.clear();
		}
		if (callToCancel != null)
		{
			callToCancel.cancel();
		}
	}

	static List<Star> parseStars(Gson gson, String responseBody, long nowEpochSeconds)
	{
		JsonElement root = gson.fromJson(responseBody, JsonElement.class);
		if (root == null || !root.isJsonArray())
		{
			throw new JsonParseException("Star Miners response was not a JSON array");
		}

		Map<Integer, Star> newestStarByWorld = new HashMap<>();
		JsonArray stars = root.getAsJsonArray();
		for (JsonElement element : stars)
		{
			if (element == null || !element.isJsonObject())
			{
				continue;
			}

			Star star = parseStar(element.getAsJsonObject(), nowEpochSeconds);
			if (star == null)
			{
				continue;
			}

			Star existing = newestStarByWorld.get(star.getWorld());
			if (existing == null || existing.getLastUpdated() < star.getLastUpdated())
			{
				newestStarByWorld.put(star.getWorld(), star);
			}
		}

		List<Star> parsedStars = new ArrayList<>(newestStarByWorld.values());
		parsedStars.sort(Comparator.comparingInt(Star::getTier).reversed()
			.thenComparingInt(Star::getWorld));
		return parsedStars;
	}

	private static Star parseStar(JsonObject entry, long nowEpochSeconds)
	{
		try
		{
			if (!hasValue(entry, "world") || !hasValue(entry, "tier") || !hasValue(entry, "calledAt"))
			{
				return null;
			}

			int world = entry.get("world").getAsInt();
			int tier = entry.get("tier").getAsInt();
			long lastUpdated = (long) entry.get("calledAt").getAsDouble();
			if (world <= 0 || tier < 1 || tier > 9 || lastUpdated <= 0
				|| lastUpdated > nowEpochSeconds + MAX_FUTURE_REPORT_SECONDS
				|| nowEpochSeconds - lastUpdated > MAX_REPORT_AGE_SECONDS)
			{
				return null;
			}

			return new Star(world, tier, location(entry), lastUpdated);
		}
		catch (NumberFormatException | UnsupportedOperationException | IllegalStateException e)
		{
			return null;
		}
	}

	private static boolean hasValue(JsonObject object, String key)
	{
		return object.has(key) && !object.get(key).isJsonNull();
	}

	private static String location(JsonObject entry)
	{
		if (!hasValue(entry, "calledLocation"))
		{
			return "";
		}

		String rawLocation = entry.get("calledLocation").getAsString();
		return Text.removeTags(rawLocation).replaceAll("[\\r\\n\\t]+", " ").trim();
	}

	private void completeSuccess(Call completedCall, List<Star> stars)
	{
		List<ResultCallback> callbacks;
		List<Star> immutableStars = Collections.unmodifiableList(new ArrayList<>(stars));
		synchronized (requestLock)
		{
			if (activeCall != completedCall)
			{
				return;
			}
			activeCall = null;
			cachedStars = immutableStars;
			cachedAtMillis = System.currentTimeMillis();
			callbacks = takePendingCallbacks();
		}
		for (ResultCallback callback : callbacks)
		{
			callback.onSuccess(immutableStars, false);
		}
	}

	private void completeFailure(Call completedCall, String message)
	{
		List<ResultCallback> callbacks;
		synchronized (requestLock)
		{
			if (completedCall != null && activeCall != completedCall)
			{
				return;
			}
			activeCall = null;
			callbacks = takePendingCallbacks();
		}
		notifyFailure(callbacks, message);
	}

	private static void notifyFailure(List<ResultCallback> callbacks, String message)
	{
		for (ResultCallback callback : callbacks)
		{
			callback.onFailure(message);
		}
	}

	private List<ResultCallback> takePendingCallbacks()
	{
		List<ResultCallback> callbacks = new ArrayList<>(pendingCallbacks);
		pendingCallbacks.clear();
		return callbacks;
	}

	public interface ResultCallback
	{
		void onSuccess(List<Star> stars, boolean fromCache);

		void onFailure(String message);
	}
}
