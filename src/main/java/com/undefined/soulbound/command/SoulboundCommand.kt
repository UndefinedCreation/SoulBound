package com.undefined.soulbound.command

import com.undefined.api.command.UndefinedCommand
import com.undefined.api.extension.string.miniMessage
import com.undefined.api.scheduler.TimeUnit
import com.undefined.api.scheduler.delay
import com.undefined.api.scheduler.repeatingTask
import com.undefined.soulbound.game.*
import com.undefined.soulbound.util.updateColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import kotlin.random.Random

class SoulboundCommand {

    init {
        val main = UndefinedCommand("soulbound", permission = "undefined.event.soulbound")
        main.addSubCommand("start")
            .addExecutePlayer {

                var a = 5
                repeatingTask(20*60*30, 3) {
                    a--
                    Bukkit.getOnlinePlayers().forEach {
                        it.showTitle(Title.title("<red>${30*a} minutes left".miniMessage(), Component.empty()))
                    }
                }

                delay(2, TimeUnit.HOURS) {
                    Bukkit.getServerTickManager().isFrozen = true
                    Bukkit.getOnlinePlayers().forEach {
                        it.player?.kick(Component.text("Session has ended!"))
                    }
                    Bukkit.setWhitelist(true)
                }

                Bukkit.getServerTickManager().isFrozen = false

                delay(10, TimeUnit.SECONDS) {
                    Bukkit.broadcastMessage(SoulManager.souls.isNotEmpty().toString())
                    if (SoulManager.souls.isNotEmpty()) {
                        delay(2, TimeUnit.MINUTES) {
                            // TODO("BOOGIE MAN!!!")
                        }
                        return@delay
                    } else {
                        Bukkit.broadcastMessage("Start")
                        Bukkit.getOnlinePlayers().giveSoulBounds()
                        Bukkit.getOnlinePlayers().forEach {
                            it.showTitle(Title.title("<gray>You will have...".miniMessage(), Component.empty()))
                        }
                        var amount = 0
                        repeatingTask(5, 5, 20) {
                            amount++
                            if (amount == 18) {
                                Bukkit.getOnlinePlayers().forEach {
                                    val soul = it.getSoulData() ?: return@forEach
                                    it.showTitle(Title.title("<gray>You have ${getColor(soul.lives)}${soul.lives} <green>lives.".miniMessage(), Component.empty()))
                                    it.playSound(it, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0F, 1.0F)
                                    it.updateColor()
                                }
                            } else {
                                for (player in Bukkit.getOnlinePlayers()) {
                                    val random = Random.nextInt(2,6)
                                    player.showTitle(Title.title("${getColor(random)}$random".miniMessage(), Component.empty()))
                                    player.playSound(player, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                                }
                            }

                        }

                    }
                }
                return@addExecutePlayer false
            }

        main.addSubCommand("lives")
            .addPlayerSubCommand()
            .addTargetExecute {
                val player: Player = sender as? Player ?: return@addTargetExecute false
                val soulData: SoulData = target.getSoulData() ?: run {
                    player.sendRichMessage("<red>Invalid player!")
                    return@addTargetExecute false
                }
                player.sendRichMessage("<green>${target.name} has ${getColor(soulData.lives)}${soulData.lives} <green>lives.")
                return@addTargetExecute false
            }

        main.addSubCommand("setlives") // TODO
            .addPlayerSubCommand()
            .addTargetExecute {
                val player: Player = sender as? Player ?: return@addTargetExecute false
                val soulData: SoulData = target.getSoulData() ?: run {
                    player.sendRichMessage("<red>Invalid player!")
                    return@addTargetExecute false
                }
                player.sendRichMessage("<green>${target.name} has ${getColor(soulData.lives)}${soulData.lives} <green>lives.")
                return@addTargetExecute false
            }

        UndefinedCommand("gift")
            .addPlayerSubCommand()
            .addTargetExecute {
                val player: Player = sender as? Player ?: return@addTargetExecute false
                val soulData: SoulData = player.getSoulData() ?: return@addTargetExecute false
                val targetSoulData: SoulData = player.getSoulData() ?: run {
                    player.sendRichMessage("<red>That player is invalid!")
                    return@addTargetExecute false
                }
                val soulmate = player.getSoulMate()
                if (soulData.lives < 2) {
                    player.sendRichMessage("<red>You don't have enough lives to be able to gift any!")
                    return@addTargetExecute false
                }
                if (soulmate != null && target.uniqueId == soulmate.uniqueId) {
                    player.sendRichMessage("<red>You cannot give lives to your soulmate!")
                    return@addTargetExecute false
                }
                soulData.lives -= 1
                targetSoulData.lives += 1
                return@addTargetExecute false
            }
    }

    fun getColor(int: Int): String = when (int) {
        2 -> "<yellow>"
        3 -> "<green>"
        else -> "<dark_green>"
    }

}