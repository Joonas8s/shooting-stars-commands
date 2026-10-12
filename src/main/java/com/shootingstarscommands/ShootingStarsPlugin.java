package com.shootingstarscommands;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Shooting Stars Commands",
	description = "Adds a !stars chat command for active Shooting Stars",
	tags = {"shooting", "stars", "star", "mining", "chat"},
	internalName = ShootingStarsConfig.GROUP
)
public class ShootingStarsPlugin extends Plugin
{
	private static final String STARS_COMMAND = "!stars";
	private static final int MAX_STARS_IN_RESPONSE = 5;
	private static final int MAX_RESPONSE_LENGTH = 240;
	private static final int MAX_LOCATION_LENGTH = 40;
	private static final String HELP_MESSAGE = "Usage: !stars | !stars t7 | !stars t7+ | !stars varrock | !stars t7 varrock";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private ShootingStarsConfig config;

	@Inject
	private StarService starService;

	@Inject
	private ClientToolbar clientToolbar;

	private volatile boolean running;
	private ShootingStarsGuidePanel guidePanel;
	private NavigationButton guideNavigation;

	@Override
	protected void startUp()
	{
		running = true;
		chatCommandManager.registerCommandAsync(STARS_COMMAND, this::showStars);
		guidePanel = new ShootingStarsGuidePanel();
		guideNavigation = NavigationButton.builder()
			.tooltip("Shooting Stars Commands")
			.icon(ShootingStarsGuidePanel.createIcon())
			.priority(7)
			.panel(guidePanel)
			.build();
		clientToolbar.addNavigation(guideNavigation);
		log.debug("Shooting Stars Commands started");
	}

	@Override
	protected void shutDown()
	{
		running = false;
		chatCommandManager.unregisterCommand(STARS_COMMAND);
		starService.cancelPendingRequest();
		clientToolbar.removeNavigation(guideNavigation);
		guideNavigation = null;
		guidePanel = null;
		log.debug("Shooting Stars Commands stopped");
	}

	@Provides
	ShootingStarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ShootingStarsConfig.class);
	}

	private void showStars(ChatMessage chatMessage, String commandText)
	{
		if (isHelpCommand(commandText))
		{
			setResponse(chatMessage, HELP_MESSAGE);
			return;
		}

		StarFilter filter = StarFilter.fromCommand(commandText);
		if (!config.useStarMinersFeed())
		{
			setResponse(chatMessage, "Enable Use Star Miners feed in the plugin settings to retrieve live stars.");
			return;
		}

		if (config.authorizationKey() == null || config.authorizationKey().trim().isEmpty())
		{
			setResponse(chatMessage, "Set your Star Miners authorization key in the plugin settings.");
			return;
		}

		starService.fetchActiveStars(new StarService.ResultCallback()
		{
			@Override
			public void onSuccess(List<Star> stars, boolean fromCache)
			{
				if (running)
				{
					setResponse(chatMessage, formatStars(filter.apply(stars), filter));
				}
			}

			@Override
			public void onFailure(String message)
			{
				if (running)
				{
					setResponse(chatMessage, message);
				}
			}
		});
	}

	private void setResponse(ChatMessage chatMessage, String response)
	{
		clientThread.invoke(() ->
		{
			if (!running)
			{
				return;
			}

			String[] lines = response.split("\\n");
			ChatMessageBuilder responseBuilder = new ChatMessageBuilder();
			appendHeader(responseBuilder, lines[0]);

			for (int index = 1; index < lines.length; index++)
			{
				responseBuilder.append("\n");
				appendStarLine(responseBuilder, lines[index]);
			}

			String formattedResponse = responseBuilder.build().replace("\n", "<br>");
			MessageNode messageNode = chatMessage.getMessageNode();
			messageNode.setRuneLiteFormatMessage(formattedResponse);
			client.refreshChat();
		});
	}

	private static void appendHeader(ChatMessageBuilder builder, String header)
	{
		int openingBracket = header.indexOf('[');
		int closingBracket = header.indexOf(']');

		if (openingBracket == -1 || closingBracket <= openingBracket)
		{
			builder.append(Color.RED, header);
			return;
		}

		builder.append(Color.RED, header.substring(0, openingBracket + 1));
		builder.append(Color.BLACK, header.substring(openingBracket + 1, closingBracket));
		builder.append(Color.RED, header.substring(closingBracket));
	}

	private static void appendStarLine(ChatMessageBuilder builder, String line)
	{
		int separator = line.indexOf(" - ");

		if (separator == -1)
		{
			builder.append(Color.RED, line);
			return;
		}

		builder.append(Color.BLACK, line.substring(0, separator + 3));
		builder.append(Color.RED, line.substring(separator + 3));
	}

	static String formatStars(List<Star> stars)
	{
		return formatStars(stars, StarFilter.fromCommand("!stars"));
	}

	static String formatStars(List<Star> stars, StarFilter filter)
	{
		if (stars.isEmpty())
		{
			return filter.isAll()
				? "No active stars were found."
				: "No active stars matched " + filter.description() + ".";
		}

		StringBuilder response = new StringBuilder(filter.isAll() ? "Stars:" : "Stars [" + filter.description() + "]:");
		int starsShown = 0;
		for (Star star : stars)
		{
			if (starsShown == MAX_STARS_IN_RESPONSE)
			{
				break;
			}

			String entry = "W" + star.getWorld() + " T" + star.getTier();
			if (!star.getLocation().isEmpty())
			{
				entry += " - " + abbreviateLocation(star.getLocation());
			}

			if (response.length() + 1 + entry.length() > MAX_RESPONSE_LENGTH)
			{
				break;
			}

			response.append('\n').append(entry);
			starsShown++;
		}

		if (starsShown == 0)
		{
			return "Active stars were found, but their details were too long to display.";
		}
		if (starsShown < stars.size())
		{
			String remaining = "+" + (stars.size() - starsShown) + " more";
			if (response.length() + 1 + remaining.length() <= MAX_RESPONSE_LENGTH)
			{
				response.append('\n').append(remaining);
			}
		}
		return response.toString();
	}

	static boolean isHelpCommand(String commandText)
	{
		return "!stars help".equalsIgnoreCase(commandText)
			|| "!stars commands".equalsIgnoreCase(commandText);
	}

	private static String abbreviateLocation(String location)
	{
		if (location.length() <= MAX_LOCATION_LENGTH)
		{
			return location;
		}
		return location.substring(0, MAX_LOCATION_LENGTH - 1) + "…";
	}

}
