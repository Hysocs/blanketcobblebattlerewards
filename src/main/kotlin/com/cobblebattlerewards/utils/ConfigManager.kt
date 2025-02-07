// File: ConfigManager.kt
package com.cobblebattlerewards.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.*

// Data class for individual rewards
data class Reward(
    var type: String, // "item", "command", or "redeemable"
    var id: String, // Unique identifier for the reward
    var message: String,
    var command: String? = null,
    var redeemCommand: String? = null,
    var redeemMessage: String? = null,
    var chance: Double,
    var item: Item? = null,
    var cooldown: Long = 0L,
    var cooldownActiveMessage: String? = null,
    var triggerCondition: String = "BattleWon", // Default to BattleWon, can be either "BattleWon" or "Captured"
    var minLevel: Int = 1, // Minimum level requirement for the reward
    var maxLevel: Int = 100 // Maximum level for the reward
)

// Rest of the data classes remain the same...
data class Item(
    var id: String,
    var count: Int,
    var customName: String,
    var lore: List<String>,
    var trackerValue: Int
)

data class PokemonRewards(
    var rewards: List<Reward> = listOf()
)

data class TypeGroupRewards(
    var rewards: List<Reward> = listOf()
)

data class RewardOrder(
    var pokemon: Int = 1,
    var typeGroup: Int = 2,
    var global: Int = 3
)

data class GlobalRewards(
    var rewards: List<Reward> = listOf()
)

data class GlobalConfig(
    var version: String = "1.0.0",
    var debugEnabled: Boolean = false
)

data class BattleRewardsConfig(
    var orders: RewardOrder = RewardOrder(),
    var pokemonRewards: MutableMap<String, PokemonRewards> = mutableMapOf(),
    var typeGroupRewards: MutableMap<String, TypeGroupRewards> = mutableMapOf(),
    @SerializedName("global")
    var global: GlobalRewards = GlobalRewards(),
    var allowRewardsFromPlayerBattles: Boolean = true,
    var enablePokemonRewards: Boolean = true,
    var enableTypeGroupRewards: Boolean = true,
    var enableGlobalRewards: Boolean = true,
    var inventoryFullBehavior: String = "drop"
)

data class ConfigData(
    var globalConfig: GlobalConfig = GlobalConfig(),
    var battleRewardsConfig: BattleRewardsConfig = BattleRewardsConfig()
)

object ConfigManager {

    private val logger = LoggerFactory.getLogger("ConfigManager")
    private val configDirectory: Path = Paths.get("config", "CobblemonBattleRewards")
    private val configFile: Path = configDirectory.resolve("config.json")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // Define currentVersion as a constant at the object level
    private const val CURRENT_VERSION = "1.0.0"

    // Main configuration data
    var configData: ConfigData = ConfigData()
        private set
    val pokemonRewards: MutableMap<String, PokemonRewards> = mutableMapOf()
    val typeGroupRewards: MutableMap<String, TypeGroupRewards> = mutableMapOf()
    val globalRewards: List<Reward>
        get() = configData.battleRewardsConfig.global.rewards

    /**
     * Load main config on startup.
     */
    fun loadConfig() {
        loadRewardConfigData()
    }

    /**
     * Reload configuration without restarting the mod.
     */
    fun reloadConfig() {
        logger.info("Reloading configuration for CobbleBattleRewards...")
        loadRewardConfigData()
        logger.info("Configuration reload complete.")
    }

    /**
     * Load and populate main configuration data from config.json
     */
    private fun loadRewardConfigData() {
        if (!Files.exists(configFile)) {
            logger.warn("Main config file does not exist. Creating default config.")
            createDefaultConfigData()
            return
        }

        val jsonContent = Files.readString(configFile).trim()
        if (jsonContent.isEmpty()) {
            logger.warn("Main config file is empty. Creating default config.")
            createDefaultConfigData()
            return
        }

        try {
            configData = gson.fromJson(jsonContent, ConfigData::class.java)

            // Check config version
            if (configData.globalConfig.version != CURRENT_VERSION) {
                logger.warn("Config version (${configData.globalConfig.version}) is outdated. Updating to $CURRENT_VERSION.")
                backupConfigFile()
                updateConfigVersion()
                saveConfigData()
            }

            // Populate helper maps with lowercase keys
            configData.battleRewardsConfig.pokemonRewards.forEach { (key, value) ->
                pokemonRewards[key.lowercase()] = value
            }
            configData.battleRewardsConfig.typeGroupRewards.forEach { (key, value) ->
                typeGroupRewards[key.lowercase()] = value
            }

            logger.info("Main configuration loaded successfully.")
            logDebug("Loaded Main Config Data: $configData")
        } catch (e: JsonSyntaxException) {
            logger.error("Invalid JSON syntax in main config file: ${e.message}")
            backupConfigFile()
            createDefaultConfigData()
        } catch (e: Exception) {
            logger.error("Error loading main config data: ${e.message}")
            backupConfigFile()
            createDefaultConfigData()
        }
    }

    /**
     * Backup the main config file before modifications.
     */
    private fun backupConfigFile() {
        try {
            if (Files.exists(configFile)) {
                val backupFileWithTimestamp = configDirectory.resolve("config_backup_${System.currentTimeMillis()}.json")
                Files.copy(configFile, backupFileWithTimestamp, StandardCopyOption.REPLACE_EXISTING)
                logger.warn("Backup of main config created at $backupFileWithTimestamp")
            }
        } catch (e: Exception) {
            logger.error("Failed to create backup of main config: ${e.message}")
        }
    }

    /**
     * Update the main config version to the current version.
     */
    private fun updateConfigVersion() {
        configData.globalConfig.version = CURRENT_VERSION
    }

    /**
     * Save the main configuration data to config.json
     */
    private fun saveConfigData() {
        try {
            // Ensure all rewards are correctly mapped
            configData.battleRewardsConfig.pokemonRewards = pokemonRewards.mapKeys { it.key.lowercase() }.toMutableMap()
            configData.battleRewardsConfig.typeGroupRewards = typeGroupRewards.mapKeys { it.key.lowercase() }.toMutableMap()
            configData.battleRewardsConfig.global.rewards = globalRewards

            val jsonString = gson.toJson(configData)
            Files.writeString(
                configFile,
                jsonString,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            logger.info("Successfully saved main config data to $configFile")
            logDebug("Saved Main Config Data: $jsonString")
        } catch (e: Exception) {
            logger.error("Error saving main config data: ${e.message}")
        }
    }

    /**
     * Create default main config data.
     */
    private fun createDefaultConfigData() {
        try {
            Files.createDirectories(configDirectory)

            // Default global rewards with level-based rewards
            val defaultGlobalRewards = GlobalRewards(
                rewards = listOf(
                    // Basic money reward with level scaling
                    Reward(
                        type = "command",
                        id = "money_reward_low_level",
                        message = "You received $150!",
                        command = "eco deposit 150 impactor:dollars %player%",
                        chance = 100.0,
                        cooldown = 0L,
                        cooldownActiveMessage = "",
                        triggerCondition = "BattleWon",
                        minLevel = 1,
                        maxLevel = 49
                    ),
                    // Higher level money reward
                    Reward(
                        type = "command",
                        id = "money_reward_high_level",
                        message = "You received $300!",
                        command = "eco deposit 300 impactor:dollars %player%",
                        chance = 100.0,
                        cooldown = 0L,
                        cooldownActiveMessage = "",
                        triggerCondition = "BattleWon",
                        minLevel = 50,
                        maxLevel = 100
                    ),
                    // Huntcoin reward
                    Reward(
                        type = "command",
                        id = "huntcoin_reward",
                        message = "You received one Huntcoin !",
                        command = "eco deposit 1 impactor:huntcoins %player%",
                        chance = 10.0,
                        cooldown = 0L,
                        cooldownActiveMessage = "",
                        triggerCondition = "BattleWon",
                        minLevel = 1,
                        maxLevel = 100
                    ),
                    // Christmascoin reward
                    Reward(
                        type = "command",
                        id = "christmascoin_reward",
                        message = "You received one Christmascoin !",
                        command = "eco deposit 1 impactor:christmascoins %player%",
                        chance = 75.0,
                        cooldown = 0L,
                        cooldownActiveMessage = "",
                        triggerCondition = "BattleWon",
                        minLevel = 1,
                        maxLevel = 100
                    )
                )
            )

            // Default Pokémon-specific reward for Pikachu
            val pikachuRewards = PokemonRewards(
                rewards = listOf(
                    Reward(
                        type = "redeemable",
                        id = "thunderstone",
                        message = "You redeemed a Thunderstone!",
                        redeemCommand = "give %player% minecraft:nether_star 1",
                        redeemMessage = "Thunderstone redeemed successfully!",
                        chance = 100.0,
                        item = Item(
                            id = "cobblemon:thunder_stone",
                            count = 1,
                            customName = "Redeemable Thunderstone",
                            lore = listOf(
                                "Right-click to redeem",
                                "A special item"
                            ),
                            trackerValue = 5553
                        ),
                        cooldown = 120,
                        cooldownActiveMessage = "You need to wait %time% seconds before redeeming the Thunderstone again.",
                        triggerCondition = "Captured",
                        minLevel = 1,
                        maxLevel = 100
                    )
                )
            )

            // Default type-specific reward for Electric-type Pokémon
            val electricTypeRewards = TypeGroupRewards(
                rewards = listOf(
                    Reward(
                        type = "item",
                        id = "electric_potion",
                        message = "You received an Electric Potion!",
                        chance = 100.0,
                        item = Item(
                            id = "minecraft:potion",
                            count = 1,
                            customName = "Electric Potion",
                            lore = listOf(
                                "Use this potion wisely"
                            ),
                            trackerValue = 1234
                        ),
                        cooldown = 60,
                        cooldownActiveMessage = "You need to wait %time% seconds before receiving another Electric Potion.",
                        triggerCondition = "BattleWon",
                        minLevel = 1,
                        maxLevel = 100
                    )
                )
            )

            // Assemble the full configuration
            configData = ConfigData().apply {
                globalConfig = GlobalConfig(
                    version = CURRENT_VERSION,
                    debugEnabled = true
                )

                battleRewardsConfig = BattleRewardsConfig().apply {
                    orders = RewardOrder(
                        pokemon = 1,
                        typeGroup = 2,
                        global = 3
                    )
                    pokemonRewards["pikachu"] = pikachuRewards
                    typeGroupRewards["electric"] = electricTypeRewards
                    global = defaultGlobalRewards
                    allowRewardsFromPlayerBattles = true
                    enablePokemonRewards = true
                    enableTypeGroupRewards = true
                    enableGlobalRewards = true
                    inventoryFullBehavior = "drop"
                }
            }

            // Populate helper maps with lowercase keys
            configData.battleRewardsConfig.pokemonRewards.forEach { (key, value) ->
                pokemonRewards[key.lowercase()] = value
            }
            configData.battleRewardsConfig.typeGroupRewards.forEach { (key, value) ->
                typeGroupRewards[key.lowercase()] = value
            }

            // Write config to file
            saveConfigData()
            logger.info("Default config data created successfully.")
        } catch (e: Exception) {
            logger.error("Error creating default config data: ${e.message}")
        }
    }

    /**
     * Log debug messages if debug is enabled in main config.
     */
    fun logDebug(message: String) {
        if (configData.globalConfig.debugEnabled) {
            println(message)
            logger.debug(message)
        }
    }
}