package com.undefined.soulbound

import com.undefined.api.event.event
import com.undefined.soulbound.game.getSoulData
import com.undefined.soulbound.game.getSoulMate
import com.undefined.soulbound.util.updateColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.player.PlayerJoinEvent

class SoulboundListener {
    init {
        event<EntityDamageEvent> {
            val player = entity as? Player ?: return@event
            val soulData = player.getSoulData() ?: return@event
            isCancelled = true
            val player1 = Bukkit.getPlayer(soulData.player1) ?: return@event player.damage(finalDamage)
            player1.damage(finalDamage)
            val player2 = Bukkit.getPlayer(soulData.player2) ?: run {
                player.health = player1.health
                player.damage(0.00000001)
                return@event
            }
            player2.health = player1.health
            player2.damage(0.00000001)

            if (player1.health <= 0 || player2.health <= 0) {
                soulData.lives--
                player1.updateColor()
                player2.updateColor()
                if (soulData.lives <= 0) {
                    player1.world.strikeLightningEffect(player1.location)
                    player1.world.strikeLightningEffect(player2.location)

                    Bukkit.getOnlinePlayers().forEach {
                        it.playSound(it, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1f)
                    }
                    
                    player1.gameMode = GameMode.SPECTATOR
                    player2.gameMode = GameMode.SPECTATOR
                }
            }
        }

        event<EntityRegainHealthEvent> {
            val player = entity as? Player ?: return@event
            val soulData = player.getSoulData() ?: return@event

            val changePlayer: Player = if (player.uniqueId == soulData.player1) player else Bukkit.getPlayer(soulData.player2) ?: return@event
            changePlayer.health += amount
        }

        event<PlayerJoinEvent> {
            val soulmate = player.getSoulMate() ?: return@event
            player.health = soulmate.health
        }

//        event<EntityAirChangeEvent> {
//            if (entity !is Player) return@event
//            println(amount)
//        }
    }
}