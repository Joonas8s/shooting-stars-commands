package com.shootingstarscommands;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

final class ShootingStarsGuidePanel extends PluginPanel
{
	ShootingStarsGuidePanel()
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JLabel title = new JLabel("Shooting Stars Commands");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		add(title, BorderLayout.NORTH);

		JTextArea guide = new JTextArea(
			"Type these commands in game chat:\n\n"
				+ "!stars\n"
				+ "Shows all active stars. The top five fit in one chat message.\n\n"
				+ "!stars t7\n"
				+ "Shows only tier 7 stars. Use t1 through t9.\n\n"
				+ "!stars t7+\n"
				+ "Shows tier 7 and higher.\n\n"
				+ "!stars varrock\n"
				+ "Matches a reported location, without regard to letter case.\n\n"
				+ "!stars t7 varrock\n"
				+ "Combines an exact tier and a location.\n\n"
				+ "!stars help\n"
				+ "Shows this compact guide in chat.");
		guide.setEditable(false);
		guide.setFocusable(false);
		guide.setLineWrap(true);
		guide.setWrapStyleWord(true);
		guide.setFont(FontManager.getRunescapeSmallFont());
		guide.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		guide.setBackground(ColorScheme.DARK_GRAY_COLOR);
		guide.setBorder(new EmptyBorder(6, 6, 6, 6));
		add(guide, BorderLayout.CENTER);
	}

	static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(ColorScheme.BRAND_ORANGE);
			graphics.fill(starPath());
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	private static Path2D starPath()
	{
		Path2D star = new Path2D.Double();
		int center = 8;
		double outerRadius = 7;
		double innerRadius = 3;
		for (int point = 0; point < 10; point++)
		{
			double angle = -Math.PI / 2 + point * Math.PI / 5;
			double radius = point % 2 == 0 ? outerRadius : innerRadius;
			double x = center + Math.cos(angle) * radius;
			double y = center + Math.sin(angle) * radius;
			if (point == 0)
			{
				star.moveTo(x, y);
			}
			else
			{
				star.lineTo(x, y);
			}
		}
		star.closePath();
		return star;
	}
}
