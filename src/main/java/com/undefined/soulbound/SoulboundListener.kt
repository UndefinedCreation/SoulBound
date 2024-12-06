package com.undefined.soulbound

import com.undefined.api.event.event
import com.undefined.api.extension.string.miniMessage
import com.undefined.api.scheduler.delay
import com.undefined.soulbound.event.GameEndEvent
import com.undefined.soulbound.game.SoulManager
import com.undefined.soulbound.game.getSoulData
import com.undefined.soulbound.game.getSoulMate
import com.undefined.soulbound.util.sendDebug
import com.undefined.soulbound.util.updateScoreboardStuff
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class SoulboundListener {

    val playersDamaged: MutableList<UUID> = mutableListOf()

    init {
        event<EntityDamageEvent> {
            val playerDamaged = entity as? Player ?: return@event
            sendDebug("--------------------")
            sendDebug("Health Sync | Damaged Player: ${playerDamaged.name}")
            val soulmate = playerDamaged.getSoulMate() ?: return@event
            sendDebug("Health Sync | Damaged Soulmate: ${soulmate.name}")

            if (playerDamaged.uniqueId in playersDamaged) {
                playersDamaged.remove(playerDamaged.uniqueId)
                sendDebug("Health Sync | Player has damaged itself")
                DamageModifier.entries.forEach {
                    if (!isApplicable(it)) return@forEach
                    if (it == DamageModifier.BASE) return@forEach
                    setDamage(it, 0.0)
                }
                return@event
            }
            sendDebug("Health Sync | Not same entity causing damage")

            soulmate.health = playerDamaged.health
            sendDebug("Health Sync | Setting soulmate health to damage player (${playerDamaged.health})")
            val currentVelocity = soulmate.velocity.clone()
            playersDamaged.add(soulmate.uniqueId)
            soulmate.damage(finalDamage, soulmate)
            sendDebug("Health Sync | Damaging soulmate final ($finalDamage)")
            soulmate.velocity = currentVelocity
            sendDebug("Health Sync | Setting soulmate old velocity")

            if (!soulmate.isDead) return@event

            sendDebug("Health Sync | Soulmate died!")

            val soulData = playerDamaged.getSoulData() ?: return@event
            sendDebug("Health Sync | Getting souldata")
            soulData.lives--
            sendDebug("Health Sync | Removing live from souldata. New value (${soulData.lives})")

            if (soulData.lives <= 0) {
                sendDebug("Health Sync | Lives is 0. Removing players from game")
                playerDamaged.gameMode = GameMode.SPECTATOR
                soulmate.gameMode = GameMode.SPECTATOR
                sendDebug("Health Sync | Set both players to spectator")

                playerDamaged.world.strikeLightningEffect(playerDamaged.location)
                playerDamaged.world.strikeLightningEffect(soulmate.location)
                sendDebug("Health Sync | Strike Lightning at death locations")

                Bukkit.getOnlinePlayers().forEach {
                    it.playSound(it, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1f)
                }
                sendDebug("Health Sync | Death sound played to all players")
            } else {
                playerDamaged.updateScoreboardStuff()
                soulmate.updateScoreboardStuff()
                sendDebug("Health Sync | Updating scoreboard information")
            }
        }

        event<EntityRegainHealthEvent> {
            val player = entity as? Player ?: return@event
            sendDebug("--------------------")
            sendDebug("Health Sync Regain | Regaining Player: ${player.name}")
            val soulMate = player.getSoulMate() ?: return@event
            sendDebug("Health Sync Regain | Soulmate: ${soulMate.name}")
            delay(1) {
                soulMate.health = if (player.health >= 20.0) 20.0 else player.health
                sendDebug("Health Sync Regain | Updated soulmate health to main regainer. Value (${soulMate.health})")
            }
        }


        event<InventoryOpenEvent> {
            val array = this.inventory.contents.toMutableList()

            for (item in inventory.contents) {
                if (item == null) continue
                if (item.enchantments.size >= 2) array.remove(item)
                for (enchantment in item.enchantments) {
                    if (enchantment.key == Enchantment.PROTECTION)
                        if (enchantment.value > 1) array.remove(item)
                }
            }

            this.inventory.contents = array.toTypedArray()
        }

        event<InventoryClickEvent> {
            currentItem?.let {
                if (it.type.name.contains("HELMET")) {
                    inventory.remove(it)
                    isCancelled = true
                }
            }
        }

        event<PrepareItemCraftEvent> {
            if (recipe != null) {
                val m = recipe!!.result.type
                if (m.name.contains("ENCHANTING_TABLE")) {
                    inventory.result = ItemStack(Material.AIR)
                }
            }
        }

        event<PlayerJoinEvent> {
            val soulmate = player.getSoulMate() ?: return@event
            player.health = soulmate.health
            player.updateScoreboardStuff()
        }

        event<GameEndEvent> {
            if (SoulManager.boogieman != null) {
                SoulManager.souls.firstOrNull { it.key == SoulManager.boogieman }?.let {
                    it.lives--
                }
            }
        }

        event<AsyncChatEvent> {
            if (!player.hasPermission("chat")) isCancelled = true
        }

        event<EnchantItemEvent> {
            if (this.expLevelCost != 2 && this.expLevelCost != 1) {
                isCancelled = true
            }
        }

        event<EntityDeathEvent> {
            if (entity is Player) {
                val killer = entity.killer ?: return@event

                val data1 = killer.getSoulData() ?: return@event
                val data2 = (entity as Player).getSoulData() ?: return@event
                if (data2.key == data1.key) return@event

                val boogieman = SoulManager.boogieman ?: return@event
                val killerSoulData = killer.getSoulData() ?: return@event
                if (killerSoulData.key != boogieman) return@event

                SoulManager.boogieman = null
                for (player in Bukkit.getOnlinePlayers()) {
                    player.showTitle(Title.title("<Green>Boogieman has killed!".miniMessage(), "<green>${killer.name} killed ${entity.name}".miniMessage()))
                    player.playSound(player, Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F)
                }
            }
        }


    }
}