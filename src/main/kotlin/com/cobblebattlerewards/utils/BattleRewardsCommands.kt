package com.cobblebattlerewards.utils

import com.blanketutils.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import com.mojang.brigadier.context.CommandContext
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object BattleRewardsCommands {
    private val logger = LoggerFactory.getLogger("CommandRegistrar")
    private val manager = CommandManager("cobblebattlerewards")

    fun registerCommands() {
        manager.command("cobblebattlerewards", aliases = listOf("cbr")) {
            // Base command
            executes { context ->
                executeBaseCommand(context)
            }

            // Reload subcommand
            subcommand("reload", permission = "cobblebattlerewards.reload") {
                executes { context -> executeReloadCommand(context) }
            }

            // List rewards subcommand
            subcommand("listrewards", permission = "cobblebattlerewards.list") {
                executes { context -> executeListRewardsCommand(context) }
            }
        }

        // Register all commands
        manager.register()
    }

    private fun executeBaseCommand(context: CommandContext<ServerCommandSource>): Int {
        CommandManager.sendSuccess(
            context.source,
            "§aCobblemon Battle Rewards v1.0.0",
            false
        )
        return 1
    }

    private fun executeReloadCommand(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        try {
            BattleRewardsConfigManager.reloadBlocking()
            CommandManager.sendSuccess(
                source,
                "§aCobblemon Battle Rewards configuration has been reloaded.",
                true
            )
            BattleRewardsConfigManager.logDebug("Configuration reloaded for CobbleBattleRewards.")
            return 1
        } catch (e: Exception) {
            CommandManager.sendError(
                source,
                "§cFailed to reload configuration: ${e.message}"
            )
            BattleRewardsConfigManager.logDebug("Error reloading configuration: ${e.message}")
            e.printStackTrace()
            return 0
        }
    }

    private fun executeListRewardsCommand(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val config = BattleRewardsConfigManager.config

        val messageBuilder = StringBuilder()

        // Global Rewards
        messageBuilder.append("§6Global Rewards:§r\n")
        config.globalRewards.rewards.forEach { reward ->
            messageBuilder.append("  §7- ${reward.message}§r\n")
        }

        // Pokemon Rewards
        messageBuilder.append("\n§6Pokemon Rewards:§r\n")
        config.pokemonRewards.forEach { (pokemon, rewards) ->
            messageBuilder.append("§ePokemon $pokemon:§r\n")
            rewards.rewards.forEach { reward ->
                messageBuilder.append("  §7- ${reward.message}§r\n")
            }
        }

        // Type Group Rewards
        messageBuilder.append("\n§6Type Group Rewards:§r\n")
        config.typeGroupRewards.forEach { (type, rewards) ->
            messageBuilder.append("§eType Group $type:§r\n")
            rewards.rewards.forEach { reward ->
                messageBuilder.append("  §7- ${reward.message}§r\n")
            }
        }

        source.sendFeedback({ Text.literal(messageBuilder.toString()) }, false)
        BattleRewardsConfigManager.logDebug("Listed rewards.")
        return 1
    }
}