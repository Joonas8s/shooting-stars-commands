# Shooting Stars Commands

Adds a local `!stars` chat command that lists the highest-tier currently reported
Shooting Stars. Results are ordered by tier (highest first), then world.

## Commands

| Command | Result |
| --- | --- |
| `!stars` | All active stars (up to five shown) |
| `!stars t7` | Only tier 7 stars |
| `!stars t7+` | Tier 7 and higher |
| `!stars varrock` | Stars whose reported location contains “varrock” |
| `!stars t7 varrock` | Tier 7 stars whose reported location contains “varrock” |
| `!stars help` | Show these command forms in chat |

The plugin's orange star icon in RuneLite's side bar opens a read-only command guide.

## Setup

1. Install and enable the plugin in RuneLite.
2. In the plugin settings, enable **Use Star Miners feed**.
3. Enter your own Star Miners authorization key.
4. Type `!stars` in chat.

The Star Miners feed is a third-party service. Enabling it sends your IP address
to that service. The plugin sends no RuneScape account information and contains
no bundled authorization key.

Successful results are kept for 45 seconds to avoid unnecessary requests.
