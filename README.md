# BlanketCobbleBattleRewards Configuration Guide

This guide will help you configure **BlanketCobbleBattleRewards** to customize rewards, add commands, items, and manage redeemable rewards. Below you'll find instructions for different configuration features.

## Configuration File Location

After starting your server with the mod installed, the configuration files will be generated in:
```
/config/BlanketCobblemonBattleRewards/
```

### Main Config File
- `config.json`: Stores all reward configurations.
- `lost_rewards.json`: Stores data about players' lost rewards.

---

## How to Configure Rewards

### Reward Structure
A reward in the configuration file looks like this:
```
{
    "message": "You received a special reward!",
    "command": "give %player% minecraft:diamond 1",
    "chance": 50.0,
    "item": {
        "id": "minecraft:nether_star",
        "count": 1,
        "customName": "Shiny Nether Star",
        "lore": ["A special shiny item!", "Right-click to redeem"],
        "trackerValue": 1000
    },
    "redeemable": true,
    "redeemCommand": "give %player% minecraft:diamond 1",
    "redeemMessage": "You redeemed the item and received a Diamond!",
    "cooldown": 300,
    "forceToGui": true
}
```

### Parameters Explained
- **`message`**: Displayed when the reward is given.
- **`command`**: Command executed when the reward is granted. Use `%player%` as a placeholder for the player's name.
- **`chance`**: Chance percentage for this reward (e.g., `50.0` is 50%).
- **`item`**: Specifies the item to give. Includes:
  - **`id`**: Item identifier (e.g., `minecraft:diamond`).
  - **`count`**: Number of items.
  - **`customName`**: Custom name for the item.
  - **`lore`**: List of lore strings (text displayed in the item's description).
  - **`trackerValue`**: Internal value for tracking or unique identification.
- **`redeemable`**: If `true`, the item can be redeemed through the `/lostrewards` command.
- **`redeemCommand`**: Command executed when the redeemable item is redeemed.
- **`redeemMessage`**: Message displayed when the item is redeemed.
- **`cooldown`**: Time in seconds before the reward can be granted again.
- **`forceToGui`**: If `true`, the item will go into the **Lost Rewards GUI** instead of the player's inventory.

---

## Adding Rewards

### Add a Command-Based Reward
To create a reward that executes a command:
1. Open the `config.json` file.
2. Add a new entry under the desired section (`global`, `pokemonRewards`, `typeGroupRewards`).
3. Example:
```
{
    "message": "Congratulations! Here is a free Diamond.",
    "command": "give %player% minecraft:diamond 1",
    "chance": 100.0
}
```

### Add an Item-Based Reward
To create a reward that gives an item:
1. Define the `item` object in your reward.
2. Example:
```
{
    "message": "You received a special pickaxe!",
    "item": {
        "id": "minecraft:diamond_pickaxe",
        "count": 1,
        "customName": "Ultimate Pickaxe",
        "lore": ["This pickaxe can mine anything!", "Handle with care!"],
        "trackerValue": 100
    },
    "chance": 75.0
}
```

### Add a Redeemable Item Reward
To allow players to redeem an item through the `/lostrewards` command:
1. Set `"redeemable": true`.
2. Define the `redeemCommand` and `redeemMessage`.
3. Example:
```
{
    "message": "You received a redeemable Nether Star!",
    "item": {
        "id": "minecraft:nether_star",
        "count": 1,
        "customName": "Redeemable Nether Star",
        "lore": ["Right-click to redeem!"],
        "trackerValue": 999
    },
    "redeemable": true,
    "redeemCommand": "give %player% minecraft:emerald 10",
    "redeemMessage": "You redeemed your Nether Star and received 10 Emeralds!",
    "forceToGui": true
}
```

---

## Managing Lost Rewards

### How It Works
If a player’s inventory is full, rewards are automatically stored in the **Lost Rewards GUI**, accessible via the `/lostrewards` command.

### Commands
- `/lostrewards`: Opens the **Lost Rewards GUI** to claim items.
- `/lostrewards clear`: Clears all stored rewards for the player.

---

## Example Configurations

### Global Rewards Example
```
{
    "global": {
        "rewards": [
            {
                "message": "You received a Diamond!",
                "command": "give %player% minecraft:diamond 1",
                "chance": 50.0
            },
            {
                "message": "You received a Golden Apple!",
                "item": {
                    "id": "minecraft:golden_apple",
                    "count": 1,
                    "customName": "Special Golden Apple",
                    "lore": ["A golden apple with mysterious power."]
                },
                "chance": 50.0
            }
        ]
    }
}
```

### Pokémon-Specific Rewards Example
```
{
    "pokemonRewards": {
        "pikachu": {
            "rewards": [
                {
                    "message": "Pikachu dropped a Thunderstone!",
                    "item": {
                        "id": "cobblemon:thunder_stone",
                        "count": 1,
                        "customName": "Special Thunderstone",
                        "lore": ["A unique Thunderstone.", "Exclusive to Pikachu battles."]
                    },
                    "chance": 100.0,
                    "redeemable": true,
                    "redeemCommand": "give %player% minecraft:nether_star 1",
                    "redeemMessage": "You redeemed your Thunderstone and received a Nether Star!"
                }
            ]
        }
    }
}
```

---

## Debugging and Logs
- Enable debug mode by setting `"debugEnabled": true` in the `globalConfig` section.
- Debug messages will appear in the console for detailed insights.

