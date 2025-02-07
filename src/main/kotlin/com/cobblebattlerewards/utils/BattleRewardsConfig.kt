package com.cobblebattlerewards.utils

import com.blanketutils.config.ConfigData
import com.blanketutils.config.ConfigManager
import com.blanketutils.config.ConfigMetadata
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

data class BattleRewardsConfig(
    override val version: String = "1.0.0",
    override val configId: String = "cobblebattlerewards",
    var debugEnabled: Boolean = true,
    var orders: RewardOrder = RewardOrder(),
    var pokemonRewards: MutableMap<String, PokemonRewards> = mutableMapOf(),
    var typeGroupRewards: MutableMap<String, TypeGroupRewards> = mutableMapOf(),
    @SerializedName("global")
    var globalRewards: GlobalRewards = GlobalRewards(),
    var allowRewardsFromPlayerBattles: Boolean = true,
    var enablePokemonRewards: Boolean = true,
    var enableTypeGroupRewards: Boolean = true,
    var enableGlobalRewards: Boolean = true,
    var inventoryFullBehavior: String = "drop"
) : ConfigData

data class RewardOrder(
    var pokemon: Int = 1,
    var typeGroup: Int = 2,
    var global: Int = 3
)

data class GlobalRewards(
    var rewards: List<Reward> = listOf()
)

data class PokemonRewards(
    var rewards: List<Reward> = listOf()
)

data class TypeGroupRewards(
    var rewards: List<Reward> = listOf()
)

data class Reward(
    var type: String, // "item", "command", or "redeemable"
    var id: String, // Unique identifier for the reward
    var message: String,
    var command: String = "",
    var redeemCommand: String = "",
    var redeemMessage: String = "",
    var chance: Double,
    var item: Item? = null,
    var cooldown: Long = 0L,
    var cooldownActiveMessage: String = "",
    var triggerCondition: String = "BattleWon",
    var minLevel: Int = 1,
    var maxLevel: Int = 100
)

data class Item(
    var id: String,
    var count: Int,
    var customName: String,
    var lore: List<String>,
    var trackerValue: Int
)

object BattleRewardsConfigManager {
    private val logger = LoggerFactory.getLogger("CobbleBattleRewards")
    private const val CURRENT_VERSION = "1.0.0"
    private lateinit var configManager: ConfigManager<BattleRewardsConfig>
    private var isInitialized = false

    private val configMetadata = ConfigMetadata(
        headerComments = listOf(
            "CobbleBattleRewards Configuration File",
            "",
            "This configuration file controls how battle rewards are distributed.",
            "",
            "Debug Settings:",
            "- debugEnabled: Enable detailed logging for troubleshooting",
            "",
            "Reward Order Settings:",
            "Controls the order in which different types of rewards are processed",
            "- pokemon: Priority for Pokémon-specific rewards",
            "- typeGroup: Priority for type-based rewards",
            "- global: Priority for global rewards",
            "",
            "Reward Types:",
            "1. Pokemon Rewards: Specific rewards for catching/defeating certain Pokémon",
            "2. Type Group Rewards: Rewards based on Pokémon types",
            "3. Global Rewards: General rewards that apply to all battles",
            "",
            "Reward Configuration:",
            "Each reward can have the following properties:",
            "- type: 'item', 'command', or 'redeemable'",
            "- id: Unique identifier for the reward",
            "- message: Message shown when reward is given",
            "- command: Command to execute (for command rewards)",
            "- redeemCommand: Command to execute when redeemed (for redeemable rewards)",
            "- redeemMessage: Message shown when redeemed",
            "- chance: Probability of the reward (0-100)",
            "- cooldown: Time in seconds before the reward can be given again",
            "- cooldownActiveMessage: Message shown when on cooldown",
            "- triggerCondition: 'BattleWon' or 'Captured'",
            "- minLevel: Minimum Pokémon level for the reward",
            "- maxLevel: Maximum Pokémon level for the reward"
        ),
        footerComments = listOf(
            "End of CobbleBattleRewards Configuration",
            "For more information, visit: https://github.com/yourusername/cobblebattlerewards"
        ),
        sectionComments = mapOf(
            "version" to "WARNING: Do not edit this value - doing so may corrupt your configuration",
            "configId" to "WARNING: Do not edit this value - changing this will create a new configuration file",
            "orders" to "Priority order for processing different types of rewards",
            "pokemonRewards" to "Rewards for specific Pokémon species",
            "typeGroupRewards" to "Rewards for Pokémon of specific types",
            "globalRewards" to "Rewards that apply to all battles",
            "allowRewardsFromPlayerBattles" to "Whether rewards can be earned from PvP battles",
            "inventoryFullBehavior" to "What happens when inventory is full: 'drop' or 'skip'"
        ),
        includeTimestamp = true,
        includeVersion = true
    )

    fun logDebug(message: String) {
        if (config.debugEnabled) {
            logger.debug(message)
        }
    }

    fun initializeAndLoad() {
        if (!isInitialized) {
            initialize()
            runBlocking { load() }
            isInitialized = true
        }
    }

    private fun initialize() {
        configManager = ConfigManager(
            currentVersion = CURRENT_VERSION,
            defaultConfig = createDefaultConfig(),
            configClass = BattleRewardsConfig::class,
            metadata = configMetadata
        )
    }

    private suspend fun load() {
        configManager.reloadConfig()
    }

    fun reloadBlocking() {
        runBlocking { configManager.reloadConfig() }
    }

    val config: BattleRewardsConfig
        get() = configManager.getCurrentConfig()

    fun cleanup() {
        if (isInitialized) {
            configManager.cleanup()
            isInitialized = false
        }
    }

    private fun createDefaultConfig() = BattleRewardsConfig(
        version = CURRENT_VERSION,
        debugEnabled = true,
        pokemonRewards = mutableMapOf(
            "pikachu" to createDefaultPikachuRewards()
        ),
        typeGroupRewards = mutableMapOf(
            "electric" to createDefaultElectricTypeRewards()
        ),
        globalRewards = createDefaultGlobalRewards()
    )

    private fun createDefaultPikachuRewards() = PokemonRewards(
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
                triggerCondition = "Captured"
            )
        )
    )

    private fun createDefaultElectricTypeRewards() = TypeGroupRewards(
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
                    lore = listOf("Use this potion wisely"),
                    trackerValue = 1234
                ),
                cooldown = 60,
                cooldownActiveMessage = "You need to wait %time% seconds before receiving another Electric Potion.",
                triggerCondition = "BattleWon"
            )
        )
    )


    private fun createDefaultGlobalRewards() = GlobalRewards(
        rewards = listOf(
            Reward(
                type = "command",
                id = "money_reward_low_level",
                message = "You received $150!",
                command = "eco deposit 150 impactor:dollars %player%",
                chance = 100.0,
                minLevel = 1,
                maxLevel = 49
            ),
            Reward(
                type = "command",
                id = "money_reward_high_level",
                message = "You received $300!",
                command = "eco deposit 300 impactor:dollars %player%",
                chance = 100.0,
                minLevel = 50,
                maxLevel = 100
            ),
            Reward(
                type = "command",
                id = "huntcoin_reward",
                message = "You received one Huntcoin !",
                command = "eco deposit 1 impactor:huntcoins %player%",
                chance = 10.0
            ),
            Reward(
                type = "command",
                id = "christmascoin_reward",
                message = "You received one Christmascoin !",
                command = "eco deposit 1 impactor:christmascoins %player%",
                chance = 75.0
            )
        )
    )
}