package com.undefined.soulbound

import com.undefined.api.event.event
import com.undefined.api.scheduler.delay
import com.undefined.soulbound.game.getSoulData
import com.undefined.soulbound.game.getSoulMate
import com.undefined.soulbound.util.updateScoreboardStuff
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.player.PlayerJoinEvent

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

        event<PlayerJoinEvent> {
            val soulmate = player.getSoulMate() ?: return@event
            player.health = soulmate.health
        }
    }
}