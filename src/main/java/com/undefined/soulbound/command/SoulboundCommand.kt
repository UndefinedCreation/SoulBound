package com.undefined.soulbound.command

import com.undefined.api.command.UndefinedCommand
import com.undefined.api.extension.string.miniMessage
import com.undefined.api.scheduler.TimeUnit
import com.undefined.api.scheduler.delay
import com.undefined.api.scheduler.repeatingTask
import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.event.GameEndEvent
import com.undefined.soulbound.game.*
import com.undefined.soulbound.util.TabManager
import com.undefined.soulbound.util.updateScoreboardStuff
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import kotlin.random.Random

class SoulboundCommand {

    var task: BukkitTask? = null

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

                task = delay(2, TimeUnit.HOURS) {
                    GameEndEvent().callEvent()

                    Bukkit.getServerTickManager().isFrozen = true
                    Bukkit.getOnlinePlayers().forEach {
                        it.player?.kick(Component.text("Session has ended!"))
                    }
                    Bukkit.setWhitelist(true)
                }

                Bukkit.getServerTickManager().isFrozen = false

                if (SoulManager.souls.isNotEmpty()) {
                    assignBoogieman()
                    return@addExecutePlayer false
                } else {
                    println("1")
                    Bukkit.getOnlinePlayers().giveSoulBounds()
                    println("2")
                    Bukkit.getOnlinePlayers().forEach {
                        println("3")
                        it.showTitle(Title.title("<gray>You will have...".miniMessage(), Component.empty()))
                    }
                    var amount = 0
                    repeatingTask(30, 5, 20) {
                        amount++
                        if (amount == 20) {
                            Bukkit.getOnlinePlayers().forEach {
                                val soul = it.getSoulData() ?: return@forEach
                                it.showTitle(Title.title("<gray>You have ${getColor(soul.lives)}${soul.lives} <gray>lives.".miniMessage(), Component.empty()))
                                it.playSound(it, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0F, 1.0F)
                                it.updateScoreboardStuff()
                            }
                        } else {
                            for (player in Bukkit.getOnlinePlayers()) {
                                val random = Random.nextInt(2, 6)
                                player.showTitle(Title.title("${getColor(random)}$random".miniMessage(), Component.empty()))
                                player.playSound(player, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                            }
                        }

                    }
                    delay(5*60*20) {
                        assignBoogieman()
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

        main.addSubCommand("addLife")
            .addPlayerSubCommand()
            .addTargetExecute {
                val player: Player = sender as? Player ?: return@addTargetExecute false
                val soulData: SoulData = target.getSoulData() ?: run {
                    player.sendRichMessage("<red>Invalid player!")
                    return@addTargetExecute false
                }
                player.sendRichMessage("<green>Life has been added!")
                soulData.lives++
                target.updateScoreboardStuff()
                target.getSoulMate()?.updateScoreboardStuff()
                return@addTargetExecute false
            }

        main.addSubCommand("end")
            .addExecutePlayer {

                GameEndEvent().callEvent()

                Bukkit.getServerTickManager().isFrozen = true
                Bukkit.getOnlinePlayers().forEach {
                    it.player?.kick(Component.text("Session has ended!"))
                }
                Bukkit.setWhitelist(true)

                task?.cancel()

                return@addExecutePlayer false
            }

        main.addSubCommand("removeLife")
            .addPlayerSubCommand()
            .addTargetExecute {
                val player: Player = sender as? Player ?: return@addTargetExecute false
                val soulData: SoulData = target.getSoulData() ?: run {
                    player.sendRichMessage("<red>Invalid player!")
                    return@addTargetExecute false
                }
                player.sendRichMessage("<green>Life has been removed!")
                soulData.lives--
                target.updateScoreboardStuff()
                target.getSoulMate()?.updateScoreboardStuff()
                return@addTargetExecute false
            }

        main.addSubCommand("reset")
            .addExecutePlayer {
                val player = player ?: return@addExecutePlayer false

                SoulBound.WORLD.persistentDataContainer.keys.forEach { SoulBound.WORLD.persistentDataContainer.remove(it) }
                SoulManager.souls.clear()

                player.sendRichMessage("<red>Soul bound have been cleared")
                Bukkit.getOnlinePlayers().forEach { it.updateScoreboardStuff() }

                task?.cancel()

                return@addExecutePlayer false
            }

        main.addSubCommand("boogieman")
            .addExecutePlayer {
                SoulManager.boogieman = null
                Bukkit.getOnlinePlayers().forEach {
                    it.showTitle(Title.title("<Green>Boogieman has killed!".miniMessage(), "<green>Was cleared by admin".miniMessage()))
                    it.playSound(it, Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F)
                }
                return@addExecutePlayer false
            }

        UndefinedCommand("gift")
            .addPlayerSubCommand()
            .addTargetExecute {
                val player: Player = sender as? Player ?: return@addTargetExecute false
                val soulData: SoulData = player.getSoulData() ?: return@addTargetExecute false
                val targetSoulData: SoulData = target.getSoulData() ?: run {
                    player.sendRichMessage("<red>That player is invalid!")
                    return@addTargetExecute false
                }
                if (soulData.lives < targetSoulData.lives) {
                    player.sendRichMessage("<red>You cannot gift to somebody with more lives than yourself!")
                    return@addTargetExecute false
                }
                if (soulData.lives < 2) {
                    player.sendRichMessage("<red>You don't have enough lives to be able to gift any!")
                    return@addTargetExecute false
                }
                val soulmate = player.getSoulMate()
                if (soulmate != null && target.uniqueId == soulmate.uniqueId) {
                    player.sendRichMessage("<red>You cannot give lives to your soulmate!")
                    return@addTargetExecute false
                }
                soulData.lives -= 1
                targetSoulData.lives += 1
                player.sendRichMessage("<green>You successfully gifted one life to ${target.name}")
                target.sendRichMessage("<green>You have been gifted one life by ${player.name}")
                player.updateScoreboardStuff()
                target.updateScoreboardStuff()
                return@addTargetExecute false
            }
    }

    fun assignBoogieman() {
        val data = SoulManager.souls.assignBoogieman() ?: return
        val player1 = Bukkit.getPlayer(data.player1) ?: return
        val player2 = Bukkit.getPlayer(data.player2) ?: return
        for (player in Bukkit.getOnlinePlayers()) {
            Bukkit.getOnlinePlayers().forEach {
                it.showTitle(Title.title("<yellow>Boogieman is beeing chosen".miniMessage(), "".miniMessage()))
                it.playSound(player, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
            }
            delay(20) {
                Bukkit.getOnlinePlayers().forEach {
                    it.showTitle(Title.title("<yellow>Boogieman is beeing chosen.".miniMessage(), "".miniMessage()))
                    player.playSound(player, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                }
                delay(20) {
                    Bukkit.getOnlinePlayers().forEach {
                        it.showTitle(Title.title("<yellow>Boogieman is beeing chosen..".miniMessage(), "".miniMessage()))
                        player.playSound(player, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                    }
                    delay(20) {
                        Bukkit.getOnlinePlayers().forEach {
                            it.showTitle(Title.title("<yellow>Boogieman is beeing chosen...".miniMessage(), "".miniMessage()))
                            player.playSound(player, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                        }
                        delay(20) {
                            Bukkit.getOnlinePlayers().forEach {
                                it.playSound(player, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F)
                                if (it.uniqueId != player1.uniqueId && it.uniqueId != player2.uniqueId) return@forEach it.showTitle(Title.title("<green>You are innocent!".miniMessage(), "".miniMessage()))
                                it.showTitle(Title.title("<red>You are the boogieman!".miniMessage(), "".miniMessage()))
                                SoulManager.boogieman = data.key
                            }
                        }
                    }
                }
            }
        }
    }

    fun getColor(int: Int): String = when (int) {
        2 -> "<yellow>"
        3 -> "<green>"
        else -> "<dark_green>"
    }

}