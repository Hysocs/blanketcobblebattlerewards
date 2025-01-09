# BlanketCobbleBattleRewards

A Minecraft mod that adds customizable rewards for winning battles and capturing Pokémon in Cobblemon.

## Features

- Multiple reward types (global, Pokémon-specific, type-based)
- Configurable chances and cooldowns
- Level-based conditions
- Work with any economy that supports giving players eco via commands
- Redeemable items system

## Basic Setup and Usage

### Installation
1. Place the mod in your server's `mods` folder
2. Start the server once to generate configuration files
3. Configure basic rewards in `config/CobblemonBattleRewards/config.json`

### Simple Reward Configuration

#### Basic Money Reward
```json
{
    "global": {
        "rewards": [
            {
                "type": "command",
                "id": "money_reward",
                "message": "You earned 100 coins!",
                "command": "eco give %player% 100",
                "chance": 100.0
            }
        ]
    }
}
```

#### Basic Item Reward
```json
{
    "global": {
        "rewards": [
            {
                "type": "item",
                "id": "diamond_reward",
                "message": "You received a diamond!",
                "item": {
                    "id": "minecraft:diamond",
                    "count": 1
                },
                "chance": 50.0
            }
        ]
    }
}
```

## Advanced Configuration Guide

### Valid Input Values

#### Reward Types
`type` field accepts one of:
- `"command"` - Executes a command
- `"item"` - Gives an item directly
- `"redeemable"` - Creates a redeemable item token

#### Trigger Conditions
`triggerCondition` field accepts one of:
- `"BattleWon"` - Triggers when player wins a battle
- `"Captured"` - Triggers when player captures a Pokémon

#### Inventory Full Behavior
`inventoryFullBehavior` field accepts:
- `"drop"` - Drops items on the ground

#### Pokemon Types
Valid types for `typeGroupRewards`:
- `"normal"`
- `"fire"`
- `"water"`
- `"electric"`
- `"grass"`
- `"ice"`
- `"fighting"`
- `"poison"`
- `"ground"`
- `"flying"`
- `"psychic"`
- `"bug"`
- `"rock"`
- `"ghost"`
- `"dragon"`
- `"dark"`
- `"steel"`
- `"fairy"`

### Configuration Structure

The configuration system is divided into several key components:

### Global Configuration
```json
{
    "globalConfig": {
        "version": "1.0.0",      // Config version - do not modify
        "debugEnabled": false     // Enables detailed logging for troubleshooting
    }
}
```

### Battle Rewards Configuration
```json
{
    "battleRewardsConfig": {
        "orders": {
            "pokemon": 1,         // Priority for Pokemon-specific rewards
            "typeGroup": 2,       // Priority for type-based rewards
            "global": 3           // Priority for global rewards
        },
        "allowRewardsFromPlayerBattles": true,  // Enable/disable rewards from PvP
        "enablePokemonRewards": true,           // Enable/disable Pokemon-specific rewards
        "enableTypeGroupRewards": true,         // Enable/disable type-based rewards
        "enableGlobalRewards": true,            // Enable/disable global rewards
        "inventoryFullBehavior": "drop"         // Behavior when inventory is full
    }
}
```

### Reward Types Explained

#### 1. Command Rewards
Used for executing commands (like giving money or items):
```json
{
    "type": "command",
    "id": "unique_reward_id",        // Unique identifier for the reward
    "message": "Reward message",     // Message displayed to player
    "command": "command string",     // Command to execute (%player% for player name)
    "chance": 50.0,                 // Probability (0-100)
    "cooldown": 300,                // Time in seconds between rewards
    "cooldownActiveMessage": "",    // Message when cooldown is active
    "triggerCondition": "BattleWon", // When to give reward
    "minLevel": 1,                  // Minimum Pokemon level required
    "maxLevel": 100                 // Maximum Pokemon level allowed
}
```

#### 2. Item Rewards
For giving items directly:
```json
{
    "type": "item",
    "id": "unique_reward_id",
    "message": "Reward message",
    "chance": 50.0,
    "cooldown": 300,
    "cooldownActiveMessage": "Wait %time% seconds",
    "triggerCondition": "BattleWon",
    "minLevel": 1,
    "maxLevel": 100,
    "item": {
        "id": "minecraft:item_id",    // Minecraft/Cobblemon item identifier
        "count": 1,                   // Number of items (1-64)
        "customName": "Item Name",    // Custom display name
        "lore": [                     // Item description
            "Line 1",
            "Line 2"
        ],
        "trackerValue": 12345        // Unique identifier for redeemable items
    }
}
```

#### 3. Redeemable Rewards
For items that can be right-clicked to claim:
```json
{
    "type": "redeemable",
    "id": "unique_reward_id",
    "message": "Initial receive message",
    "redeemCommand": "give %player% item 1",  // Command executed on redemption
    "redeemMessage": "Redemption message",    // Message shown when redeemed
    "chance": 50.0,
    "cooldown": 300,
    "cooldownActiveMessage": "Wait %time% seconds",
    "triggerCondition": "BattleWon",
    "minLevel": 1,
    "maxLevel": 100,
    "item": {
        // Same structure as Item Rewards
    }
}
```

### Value Ranges

- `chance`: 0.0 to 100.0 (percentage)
- `cooldown`: 0 or greater (seconds)
- `minLevel`: 1 to 100
- `maxLevel`: 1 to 100
- `count` (in item): 1 to 64
- `trackerValue`: Any unique integer

### Economy Integration
Compatible with any economy mod that uses commands. Example formats:
```json
{
    "type": "command",
    "command": "eco give %player% 100"     // Standard economy
    // OR
    "command": "balance add %player% 100"  // Impactor
    // OR
    "command": "ps money add %player% 100" // PlayerShops
}
```

### Debug Mode
Enable detailed logging by setting in config:
```json
{
    "globalConfig": {
        "debugEnabled": true
    }
}
```

## Important Notes

- Some configuration changes require server restart
- Each reward type can be globally enabled/disabled
- Reward priorities determine which type is processed first
- The `%player%` placeholder is replaced with player name
- The `%time%` placeholder in cooldown messages is replaced with remaining seconds
- Supports Minecraft color codes (§) in messages

## Technical Support

For bug reports or feature requests, please use the issue tracker on our repository.
