package com.undefined.soulbound

import com.undefined.api.event.event
import com.undefined.api.extension.string.miniMessage
import com.undefined.api.scheduler.delay
import com.undefined.soulbound.event.GameEndEvent
import com.undefined.soulbound.game.SoulManager
import com.undefined.soulbound.game.getSoulData
import com.undefined.soulbound.game.getSoulMate
import com.undefined.soulbound.util.updateScoreboardStuff
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentOffer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack

class SoulboundListener {
    init {
        event<EntityDamageEvent> {
            val player = entity as? Player ?: return@event
            val soulmate = player.getSoulMate() ?: return@event

            if (damageSource.causingEntity == player) return@event

            soulmate.health = player.health
            val currentVelocity = soulmate.velocity.clone()
            soulmate.damage(finalDamage, soulmate)
            soulmate.velocity = currentVelocity

            if (soulmate.isDead) {
                val soulData = player.getSoulData() ?: return@event

                soulData.lives--

                if (soulData.lives <= 0) {
                    player.gameMode = GameMode.SPECTATOR
                    soulmate.gameMode = GameMode.SPECTATOR

                    player.world.strikeLightningEffect(player.location)
                    player.world.strikeLightningEffect(soulmate.location)

                    Bukkit.getOnlinePlayers().forEach {
                        it.playSound(it, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1f)
                    }

                } else {
                    player.updateScoreboardStuff()
                    soulmate.updateScoreboardStuff()
                }

            }
        }

        event<EntityRegainHealthEvent> {
            val player = entity as? Player ?: return@event
            val soulMate = player.getSoulMate() ?: return@event
            delay(1) { soulMate.health = if (player.health >= 20.0) 20.0 else player.health }
        }


        event<InventoryOpenEvent> {

            val array = this.inventory.contents.toMutableList()

            this.inventory.contents.forEach { item ->
                item?.let {
                    if (item.enchantments.size >= 2) array.remove(item)
                    item.enchantments.forEach {
                        if (it.key == Enchantment.PROTECTION) {
                            if (it.value > 1) array.remove(item)
                        }
                    }
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

        event<EntityDeathEvent> {
            if (entity is Player) {
                val killer = entity.killer ?: return@event

                val data1 = killer.getSoulData() ?: return@event
                val data2 = (entity as Player).getSoulData() ?: return@event
                if (data2.key == data1.key) return@event

                SoulManager.boogieman?.let { bUUID ->
                    killer.getSoulData()?.let {
                        if (it.key == bUUID) {
                            SoulManager.boogieman = null

                            Bukkit.getOnlinePlayers().forEach {

                                it.showTitle(Title.title("<Green>Boogieman has killed!".miniMessage(), "<green>${killer.name} killed ${entity.name}".miniMessage()))
                                it.playSound(it, Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F)
                            }

                        }
                    }

                }
            }
        }
    }
}