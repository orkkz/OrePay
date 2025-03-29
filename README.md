# OrePay

Turn your mining efforts into wealth with OrePay - the ultimate ore rewards plugin for Minecraft servers.

## Overview

OrePay is a powerful, lightweight plugin that rewards players with in-game currency when they mine ores in your Minecraft server. With extensive configuration options and a flexible permission system, OrePay enhances the mining experience while providing server administrators complete control over the economy.

## Features

- **Customizable Rewards**: Configure different reward amounts for each ore type.
- **Vein Mining Support**: Built-in detection for vein mining with configurable reward adjustment.
- **Multiple Notification Styles**: Choose between chat messages, action bar, titles, or subtitles.
- **Multiplier System**: Permission-based, temporary, and world-specific multipliers.
- **Player Statistics**: Track mining stats across your server.
- **Toggle System**: Players can disable rewards if they choose.
- **Database Support**: Store data in SQLite (default) or MySQL.
- **Developer API**: Integrate OrePay with your own plugins using our API and custom events.
- **PlaceholderAPI Support**: Use OrePay data in other plugins.

## Commands

- `/orepay help` - Shows all available commands and their descriptions.
- `/orepay stats [player]` - View mining statistics for yourself or another player.
- `/orepay toggle [on/off] [player]` - Toggle ore rewards on or off for yourself or another player.
- `/orepay reload` - Reload the plugin configuration.

Shorthand alias: `/op` can be used in place of `/orepay`.

## Permissions

### Main Permissions
- `orepay.earn` - Allows players to earn rewards for mining ores (default: true)
- `orepay.command` - Allows players to use the OrePay command (default: true)

### Command Permissions
- `orepay.command.help` - View help information (default: true)
- `orepay.command.reload` - Reload the plugin (default: op)
- `orepay.command.stats` - View your own statistics (default: true)
- `orepay.command.stats.others` - View statistics for other players (default: op)
- `orepay.command.toggle` - Toggle your own rewards (default: true)
- `orepay.command.toggle.others` - Toggle rewards for other players (default: op)

### Multiplier Permissions
- `orepay.multiplier.1.5` - Give players a 1.5x multiplier (default: false)
- `orepay.multiplier.2` - Give players a 2x multiplier (default: false)
- `orepay.multiplier.3` - Give players a 3x multiplier (default: false)

## Dependencies

- **Required**: Vault (for economy integration)
- **Optional**: PlaceholderAPI (for placeholders support)

## Installation

1. Place the OrePay.jar file in your plugins folder
2. Install Vault and an economy plugin (if not already installed)
3. Start/restart your server
4. Edit the config.yml to customize your experience
5. Use `/orepay reload` to apply changes

## Building from Source

### Prerequisites
- Java 17 or higher
- Maven

### Build
```bash
git clone https://github.com/yourusername/orepay.git
cd orepay
mvn clean package
```

The compiled JAR will be in the `target` directory.

## License

This project is licensed under the MIT License - see the LICENSE file for details.