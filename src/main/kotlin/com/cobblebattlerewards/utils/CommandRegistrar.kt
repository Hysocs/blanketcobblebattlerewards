// File: CommandRegistrar.kt
package com.cobblebattlerewards.utils

import com.cobblebattlerewards.utils.ConfigManager.logDebug
import com.mojang.brigadier.CommandDispatcher
//import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object CommandRegistrar {

    private val logger = LoggerFactory.getLogger("CommandRegistrar")

    fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            logger.info("Registering commands for BlanketCobbleBattleRewards.")
            registerBattleRewardsCommand(dispatcher)
        }
    }

    // Register the `/blanketcobblebattlerewards` command
    private fun registerBattleRewardsCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val battleRewardsCommand = literal("blanketcobblebattlerewards")
            .requires { source ->
                val player = source.player
                player != null && hasPermission(player, "battle.manage", 2)
            }
            .executes { context ->
                context.source.sendFeedback({ Text.literal("BlanketCobbleBattleRewards v1.0.0") }, false)
                1
            }
            // Reload subcommand
            .then(
                literal("reload").executes { context ->
                    ConfigManager.reloadConfig() // Reload config
                    context.source.sendFeedback({ Text.literal("Cobblemon Battle Rewards configuration has been reloaded.") }, true)
                    logDebug("Configuration reloaded for BlanketCobbleBattleRewards.")
                    1
                }
            )
            // List rewards subcommand
            .then(
                literal("listrewards").executes { context ->
                    val rewardsList = getFormattedRewardsList()
                    context.source.sendFeedback({ Text.literal(rewardsList) }, false)
                    logDebug("Listed rewards.")
                    1
                }
            )

        dispatcher.register(battleRewardsCommand)
    }



    // Helper function to get formatted list of rewards
    private fun getFormattedRewardsList(): String {
        val globalRewards = ConfigManager.globalRewards.joinToString("\n") { it.message }
        val pokemonRewards = ConfigManager.pokemonRewards.map { (pokemon, rewards) ->
            "Pokemon $pokemon:\n" + rewards.rewards.joinToString("\n") { "  - ${it.message}" }
        }.joinToString("\n")
        val typeGroupRewards = ConfigManager.typeGroupRewards.map { (type, rewards) ->
            "Type Group $type:\n" + rewards.rewards.joinToString("\n") { "  - ${it.message}" }
        }.joinToString("\n")

        return "Global Rewards:\n$globalRewards\n\nPokemon Rewards:\n$pokemonRewards\n\nType Group Rewards:\n$typeGroupRewards"
    }

    // Check if the player has permission
    private fun hasPermission(player: ServerPlayerEntity, permission: String, level: Int): Boolean {
        //return if (hasLuckPermsPermission(player, permission, level)) {
        return if (player.hasPermissionLevel(level)) {
            true
        } else {
            player.hasPermissionLevel(level)
        }
    }

    // LuckPerms permission check
    //private fun hasLuckPermsPermission(player: ServerPlayerEntity, permission: String, level: Int): Boolean {
        //return try {
           // Permissions.check(player, permission, level)
        //} catch (e: NoClassDefFoundError) {
           // false
       // }
   /// }
}
