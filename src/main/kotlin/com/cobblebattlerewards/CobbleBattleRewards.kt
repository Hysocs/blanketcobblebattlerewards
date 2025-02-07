package com.cobblebattlerewards

import com.cobblebattlerewards.utils.*
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object CobbleBattleRewards : ModInitializer {
	private val logger = LoggerFactory.getLogger("blanketcobblebattlerewards")
	private val battles = ConcurrentHashMap<UUID, BattleState>()
	private val cooldowns = ConcurrentHashMap<UUID, MutableMap<String, Long>>()

	data class BattleState(
		var actors: List<BattleActor> = emptyList(),
		var playerPokemon: Pokemon? = null,
		var opponentPokemon: Pokemon? = null,
		var isResolved: Boolean = false,
		var isCaptured: Boolean = false,
		var playerWon: Boolean = false
	)

	override fun onInitialize() {
		logger.info("BlanketCobbleBattleRewards: Initializing...")
		ConfigManager.loadConfig()
		setupEventHandlers()
		CommandRegistrar.registerCommands()
		logger.info("BlanketCobbleBattleRewards: Ready")
	}

	private fun setupEventHandlers() {
		with(CobblemonEvents) {
			BATTLE_STARTED_PRE.subscribe { event ->
				battles[event.battle.battleId] = BattleState()
				logger.info("Battle pre-start for Battle ID: ${event.battle.battleId}")
			}

			BATTLE_STARTED_POST.subscribe { event ->
				battles[event.battle.battleId]?.let { state ->
					state.actors = event.battle.actors.toList()
					updatePokemonStates(event.battle.battleId)
				}
				logger.info("Battle fully started for Battle ID: ${event.battle.battleId}")
			}

			POKEMON_SENT_POST.subscribe { event ->
				findBattleByPokemon(event.pokemon)?.let { battleId ->
					updatePokemonState(battleId, event.pokemon)
					logger.info("Updated Pokemon state for battle $battleId")
				}
			}

			POKEMON_CAPTURED.subscribe { event ->
				findBattleByPokemon(event.pokemon)?.let { battleId ->
					battles[battleId]?.let { state ->
						logger.info("Pokémon captured during battle: ${event.pokemon.species.name}, Level: ${event.pokemon.level}")
						state.isCaptured = true
						handleBattleEnd(battleId, false)
					}
				}
			}

			BATTLE_VICTORY.subscribe { event ->
				battles[event.battle.battleId]?.let { state ->
					if (!state.isCaptured) {
						handleBattleEnd(event.battle.battleId, true)
						logger.info("Processing victory rewards for Battle ID: ${event.battle.battleId}, Pokemon Level: ${state.opponentPokemon?.level}")
					} else {
						logger.info("Skipping victory rewards for Battle ID: ${event.battle.battleId} as Pokémon was captured.")
					}
				}
			}

			BATTLE_FLED.subscribe { event ->
				logger.info("Battle fled, removing battle ${event.battle.battleId}")
				battles.remove(event.battle.battleId)
			}
		}

		UseItemCallback.EVENT.register { player, world, hand ->
			handleItemUse(player as ServerPlayerEntity, hand)
		}

		ServerTickEvents.START_SERVER_TICK.register(::processPendingRewards)
	}

	private fun handleItemUse(player: ServerPlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
		if (hand != Hand.MAIN_HAND) return TypedActionResult.pass(player.getStackInHand(hand))

		val stack = player.getStackInHand(hand)
		val trackerValue = stack.get(DataComponentTypes.CUSTOM_DATA)?.nbt?.getInt("trackerValue")
			?: return TypedActionResult.pass(stack)

		findRewardByTracker(trackerValue)?.let { reward ->
			if (reward.type.equals("redeemable", ignoreCase = true)) {
				executeReward(player, reward)
				stack.decrement(1)
				return TypedActionResult.success(stack)
			}
		}
		return TypedActionResult.pass(stack)
	}

	private fun handleBattleEnd(battleId: UUID, isVictory: Boolean) {
		battles[battleId]?.let { state ->
			state.isResolved = true
			state.playerWon = isVictory && !state.isCaptured

			if (state.playerWon || state.isCaptured) {
				val pokemonLevel = state.opponentPokemon?.level ?: 1
				logger.info("Processing rewards for level $pokemonLevel Pokemon")
				processRewards(battleId)
			} else {
				logger.info("Skipping rewards as player did not win the battle")
			}
		}
	}

	private fun processRewards(battleId: UUID) {
		val state = battles[battleId] ?: return
		val player = (state.actors.find { it is PlayerBattleActor } as? PlayerBattleActor)?.uuid?.let {
			state.playerPokemon?.getOwnerPlayer() as? ServerPlayerEntity
		} ?: return

		val rewardCategories = listOf(
			"pokemon" to { applyPokemonRewards(player, state) },
			"typeGroup" to { applyTypeRewards(player, state) },
			"global" to { applyGlobalRewards(player, state) }
		).sortedBy { (category, _) ->
			when (category) {
				"pokemon" -> ConfigManager.configData.battleRewardsConfig.orders.pokemon
				"typeGroup" -> ConfigManager.configData.battleRewardsConfig.orders.typeGroup
				"global" -> ConfigManager.configData.battleRewardsConfig.orders.global
				else -> Int.MAX_VALUE
			}
		}

		for ((category, applyReward) in rewardCategories) {
			if (applyReward()) {
				logger.info("Successfully applied $category rewards")
				break
			}
		}
	}

	private fun applyPokemonRewards(player: ServerPlayerEntity, state: BattleState): Boolean {
		val pokemonName = state.opponentPokemon?.species?.name?.lowercase() ?: return false
		return ConfigManager.pokemonRewards[pokemonName]?.rewards
			?.filter { isEligible(it, state) }
			?.randomOrNull()
			?.let { executeReward(player, it) } ?: false
	}

	private fun applyTypeRewards(player: ServerPlayerEntity, state: BattleState): Boolean {
		val types = listOfNotNull(
			state.opponentPokemon?.species?.primaryType?.name?.lowercase(),
			state.opponentPokemon?.species?.secondaryType?.name?.lowercase()
		)

		return types.any { type ->
			ConfigManager.typeGroupRewards[type]?.rewards
				?.filter { isEligible(it, state) }
				?.randomOrNull()
				?.let { executeReward(player, it) } ?: false
		}
	}

	private fun applyGlobalRewards(player: ServerPlayerEntity, state: BattleState): Boolean {
		return ConfigManager.globalRewards
			.filter { isEligible(it, state) }
			.randomOrNull()
			?.let { executeReward(player, it) } ?: false
	}

	private fun isEligible(reward: Reward, state: BattleState): Boolean {
		// First check the probability
		if (Random.nextDouble(100.0) > reward.chance) {
			return false
		}

		// Check level requirements
		val pokemonLevel = state.opponentPokemon?.level ?: 1
		if (pokemonLevel < reward.minLevel || pokemonLevel > reward.maxLevel) {
			logger.info("Pokemon level $pokemonLevel is outside reward level range (${reward.minLevel}-${reward.maxLevel})")
			return false
		}

		// Then check the trigger condition
		return when(reward.triggerCondition.lowercase()) {
			"battlewon" -> state.playerWon && !state.isCaptured
			"captured" -> state.isCaptured
			else -> false
		}
	}

	private fun executeReward(player: ServerPlayerEntity, reward: Reward): Boolean {
		val rewardId = "reward_${reward.id}"
		val currentTime = System.currentTimeMillis()
		val lastUsed = cooldowns.getOrPut(player.uuid) { mutableMapOf() }[rewardId] ?: 0L

		if (currentTime - lastUsed < reward.cooldown * 1000) {
			val remaining = ((reward.cooldown * 1000 - (currentTime - lastUsed)) / 1000).toInt()
			player.sendMessage(Text.literal(
				reward.cooldownActiveMessage?.replace("%time%", remaining.toString())
					?: "Wait $remaining seconds before using this reward again."
			), false)
			return false
		}

		when (reward.type.lowercase()) {
			"item", "redeemable" -> reward.item?.let { createItemStack(it) }?.let { stack ->
				player.inventory.insertStack(stack)
				player.sendMessage(Text.literal(reward.message), false)
			}
			"command" -> reward.command?.let { cmd ->
				try {
					player.server.commandManager.dispatcher.execute(
						cmd.replace("%player%", player.name.string),
						player.server.commandSource
					)
					player.sendMessage(Text.literal(reward.message), false)
				} catch (e: Exception) {
					logger.error("Failed to execute command for ${player.name.string}: ${e.message}")
					return false
				}
			}
		}

		cooldowns.getOrPut(player.uuid) { mutableMapOf() }[rewardId] = currentTime
		return true
	}

	private fun createItemStack(item: Item): ItemStack {
		val stack = ItemStack(
			Registries.ITEM.get(Identifier.of(item.id)) ?: Items.STONE,
			item.count
		).apply {
			if (item.customName.isNotEmpty()) {
				set(DataComponentTypes.CUSTOM_NAME, Text.literal(item.customName).formatted(Formatting.GOLD))
			}

			set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(NbtCompound().apply {
				putString("customName", item.customName)
				putInt("trackerValue", item.trackerValue)
			}))

			if (item.lore.isNotEmpty()) {
				set(DataComponentTypes.LORE, LoreComponent(
					item.lore.map { Text.literal(it).formatted(Formatting.GRAY) }
				))
			}
		}
		return stack
	}

	private fun findBattleByPokemon(pokemon: Pokemon): UUID? =
		battles.entries.firstOrNull { (_, state) ->
			state.actors.any { actor ->
				actor.pokemonList.any { it.effectedPokemon.uuid == pokemon.uuid }
			}
		}?.key

	private fun updatePokemonStates(battleId: UUID) {
		battles[battleId]?.actors?.forEach { actor ->
			when (actor) {
				is PlayerBattleActor -> actor.pokemonList.firstOrNull()?.effectedPokemon?.let {
					battles[battleId]?.playerPokemon = it
					logger.info("Tracking Player's Pokémon: ${it.species.name}, UUID: ${it.uuid}, Level: ${it.level}")
				}
				else -> actor.pokemonList.firstOrNull()?.effectedPokemon?.let {
					battles[battleId]?.opponentPokemon = it
					logger.info("Tracking Opponent's Pokémon: ${it.species.name}, UUID: ${it.uuid}, Level: ${it.level}")
					logger.info("Opponent Pokémon Type(s): Primary - ${it.species.primaryType.name.lowercase()}, Secondary - ${it.species.secondaryType?.name?.lowercase() ?: "None"}")
				}
			}
		}
	}

	private fun updatePokemonState(battleId: UUID, pokemon: Pokemon) {
		battles[battleId]?.let { state ->
			if (pokemon.entity?.owner is ServerPlayerEntity) {
				state.playerPokemon = pokemon
			} else {
				state.opponentPokemon = pokemon
			}
			logger.info("Updated Pokemon state: ${pokemon.species.name}, Level: ${pokemon.level}")
		}
	}

	private fun findRewardByTracker(trackerValue: Int): Reward? {
		return ConfigManager.configData.battleRewardsConfig.run {
			pokemonRewards.values.flatMap { it.rewards } +
					typeGroupRewards.values.flatMap { it.rewards } +
					ConfigManager.globalRewards
		}.find { it.item?.trackerValue == trackerValue }
	}

	private fun processPendingRewards(server: MinecraftServer) {
		battles.entries.removeIf { (battleId, state) ->
			if (state.isResolved) {
				logger.info("Cleaned up battle tracking for Battle ID: $battleId")
				true
			} else {
				false
			}
		}
	}
}