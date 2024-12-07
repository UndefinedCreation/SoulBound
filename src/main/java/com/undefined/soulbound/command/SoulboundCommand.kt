package com.undefined.soulbound.command

import com.undefined.api.command.UndefinedCommand
import com.undefined.api.extension.string.miniMessage
import com.undefined.api.scheduler.TimeUnit
import com.undefined.api.scheduler.delay
import com.undefined.api.scheduler.repeatingTask
import com.undefined.api.sendLog
import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.camera.CameraSequence
import com.undefined.soulbound.event.GameEndEvent
import com.undefined.soulbound.game.*
import com.undefined.soulbound.manager.Config
import com.undefined.soulbound.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import kotlin.random.Random

class SoulboundCommand {

    var autoSessionEndTask: BukkitTask? = null

    init {
        val main = UndefinedCommand("soulbound", permission = "undefined.event.soulbound")
        main.addSubCommand("start")
            .addExecutePlayer {
                sendDebug("--------------------")
                sendDebug("Game Stat | Start command")
                var a = 5
                repeatingTask(20*60*30, 3) {
                    sendDebug("Game Stat | Time left ${30*a}")
                    a--
                    Bukkit.getOnlinePlayers().forEach {
                        it.showTitle(Title.title("<red>${30*a} minutes left".miniMessage(), Component.empty()))
                    }
                }

                autoSessionEndTask = delay(2, TimeUnit.HOURS) {
                    sendDebug("--------------------")
                    sendDebug("Game Stat | Auto Session end")
                    GameEndEvent().callEvent()

                    Bukkit.getServerTickManager().isFrozen = true
                    Bukkit.getOnlinePlayers().forEach {
                        it.player?.kick(Component.text("Session has ended!"))
                    }
                    sendDebug("Game Stat | Kicked all players")
                    Bukkit.setWhitelist(true)
                    sendDebug("Game Stat | Session End Succes")
                }

                Bukkit.getServerTickManager().isFrozen = false
                sendDebug("Game Stat | Server unfrozen")
                sendDebug("Game Stat | Time to next choose ${if (timerCooldownShorter) 2 else 20*60*5}")
                delay(if (timerCooldownShorter) 2 else 20*60*5) {
                    if (SoulManager.souls.isNotEmpty()) {
                        assignBoogieman()
                        return@delay
                    } else {
                        sendDebug("--------------------")
                        sendDebug("SoulBound | Choosing Soulbound")
                        Bukkit.getOnlinePlayers().giveSoulBounds()
                        sendDebug("SoulBound | Running choosing animation")
                        Bukkit.getOnlinePlayers().forEach {
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
                                sendDebug("SoulBound | Choosing animation finished")
                                for (player in Bukkit.getOnlinePlayers()) {
                                    val random = Random.nextInt(2, 6)
                                    player.showTitle(Title.title("${getColor(random)}$random".miniMessage(), Component.empty()))
                                    player.playSound(player, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                                }
                            }

                        }
                        sendDebug("SoulBound | Finished soulbound linking")

                        delay(5*60*20) {
                            assignBoogieman()
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


        val eventsSubCommand = main.addSubCommand("events")

        eventsSubCommand.addSubCommand("nether")
            .addExecutePlayer {
                netherEvent()
                return@addExecutePlayer false
            }


        var oldPlayer: OfflinePlayer? = null // Player that we take the soulbound from

        main.addSubCommand("transfer")
            .addStringSubCommand()
            .addStringExecute {
                sendDebug("--------------------")
                sendLog("Transfer Command | Getting old player")
                oldPlayer = Bukkit.getOfflinePlayer(string)
                if (oldPlayer!!.hasPlayedBefore()) return@addStringExecute true
                sendLog("Transfer Command | Old player has joined before")
                return@addStringExecute false
            }
            .addStringSubCommand()
            .addStringExecute {
                sendLog("Transfer Command | Getting new player")
                val newPlayer = Bukkit.getOfflinePlayer(string)
                val soulData = oldPlayer!!.getSoulData() ?: run {
                    sendLog("Transfer Command | Old player doesn't have any souldata")
                    sender.sendRichMessage("<red>Invalid player!")
                    return@addStringExecute false
                }

                sendLog("Transfer Command | Giving soulbound to new member")
                if (soulData.player1 == oldPlayer!!.uniqueId) soulData.player1 = newPlayer.uniqueId else soulData.player2 = newPlayer.uniqueId

                newPlayer.player?.run { this.updateScoreboardStuff() }

                sendLog("Transfer Command | Success!")

                sender.sendRichMessage("<green>Transferred!")
                return@addStringExecute false
            }

        main.addSubCommand("get")
            .addExecutePlayer {
                val player = player ?: return@addExecutePlayer false

                player.sendRichMessage("<gray>----------")
                SoulBound.WORLD.persistentDataContainer.keys.forEach {
                    player.sendRichMessage("<gray>${it.key}: <aqua>${SoulBound.WORLD.persistentDataContainer.get(it, PersistentDataType.STRING)}")
                }
                player.sendRichMessage("<gray>----------")

                return@addExecutePlayer false
            }

        main.addSubCommand("toggleDebugMode")
            .addExecutePlayer {
                sendDebug("--------------------")
                debugMode = !debugMode
                sendDebug("Toggle Debug | Debug mode has been toggled to (${debugMode})")

                sendRichMessage("<green>Debug mode has been toggled to $debugMode", true)
                return@addExecutePlayer false
            }

        main.addSubCommand("toggleCooldownShorter")
            .addExecutePlayer {
                sendDebug("--------------------")
                timerCooldownShorter = !timerCooldownShorter
                sendDebug("Toggle Cooldown Timer | Cooldown timer has been toggled to ($timerCooldownShorter)")
                sendRichMessage("<green>Timer shorter mode has been toggled to $timerCooldownShorter", true)
                return@addExecutePlayer false
            }

        main.addSubCommand("end")
            .addExecutePlayer {
                sendDebug("--------------------")
                GameEndEvent().callEvent()
                sendDebug("Game End | GameEndEvent called")

                Bukkit.getServerTickManager().isFrozen = true
                sendDebug("Game End | Tick rate has been frozen")
                Bukkit.getOnlinePlayers().forEach {
                    it.player?.kick(Component.text("Session has ended!"))
                }
                sendDebug("Game End | All players have been kicked")
                Bukkit.setWhitelist(true)
                sendDebug("Game End | Set whitelist to true")
                autoSessionEndTask?.cancel()
                sendDebug("--------------------")
                return@addExecutePlayer false
            }

        main.addSubCommand("removeLife")
            .addPlayerSubCommand()
            .addTargetExecute {
                sendDebug("--------------------")
                val player: Player = sender as? Player ?: return@addTargetExecute false
                sendDebug("Remove Life | Removing life for (${target.name})")
                val soulData: SoulData = target.getSoulData() ?: run {
                    sendDebug("Remove Life | Invalid Player")
                    player.sendRichMessage("<red>Invalid player!")
                    return@addTargetExecute false
                }
                sendDebug("Remove Life | Fetched target SoulData")
                player.sendRichMessage("<green>Life has been removed!")
                soulData.lives--
                target.updateScoreboardStuff()
                target.getSoulMate()?.updateScoreboardStuff()
                sendDebug("Remove Life | End")
                return@addTargetExecute false
            }

        main.addSubCommand("reset")
            .addExecutePlayer {
                sendDebug("--------------------")

                SoulBound.WORLD.persistentDataContainer.keys.forEach { SoulBound.WORLD.persistentDataContainer.remove(it) }
                sendDebug("Reset Lives | Removed all World PDC")
                SoulManager.souls.clear()
                sendDebug("Reset Lives | Cleared all SoulData")

                sendRichMessage("<red>Soul bound have been cleared", true)
                Bukkit.getOnlinePlayers().forEach { it.updateScoreboardStuff() }

                autoSessionEndTask?.cancel()
                sendDebug("Reset Lives | Cancel AutoSessionEndTask")

                return@addExecutePlayer false
            }

        main.addSubCommand("boogieman")
            .addExecutePlayer {
                sendDebug("--------------------")
                SoulManager.boogieman = null
                sendDebug("Boogieman Admin Clear | Boogieman has been cleared")
                Bukkit.getOnlinePlayers().forEach {
                    it.showTitle(Title.title("<Green>Boogieman has killed!".miniMessage(), "<green>Was cleared by admin".miniMessage()))
                    it.playSound(it, Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F)
                }
                return@addExecutePlayer false
            }

        UndefinedCommand("gift")
            .addPlayerSubCommand()
            .addTargetExecute {
                sendDebug("--------------------")
                val player: Player = sender as? Player ?: return@addTargetExecute false
                sendDebug("Life gifting | Life from player (${player.name})")
                val soulData: SoulData = player.getSoulData() ?: return@addTargetExecute false
                sendDebug("Life gifting | Getting souldata with key (${soulData.key})")
                val targetSoulData: SoulData = target.getSoulData() ?: run {
                    player.sendRichMessage("<red>That player is invalid!")
                    sendDebug("Life gifting | Target player doesn't have any souldata")
                    return@addTargetExecute false
                }
                if (soulData.lives < targetSoulData.lives) {
                    player.sendRichMessage("<red>You cannot gift to somebody with more lives than yourself!")
                    sendDebug("Life gifting | Life from player has less lives than its self.")
                    return@addTargetExecute false
                }
                if (soulData.lives < 2) {
                    player.sendRichMessage("<red>You don't have enough lives to be able to gift any!")
                    sendDebug("Life gifting | Life from player has less than 2 life")
                    return@addTargetExecute false
                }
                val soulmate = player.getSoulMate()
                sendDebug("Life gifting | Life from player soulmate (${soulmate?.name})")
                if (soulmate != null && target.uniqueId == soulmate.uniqueId) {
                    player.sendRichMessage("<red>You cannot give lives to your soulmate!")
                    sendDebug("Life gifting | Trying to give live to its own soulmate")
                    return@addTargetExecute false
                }
                soulData.lives -= 1
                sendDebug("Life gifting | Removed life from player. Value (${soulData.lives})")
                targetSoulData.lives += 1
                sendDebug("Life gifting | Added life to target player. Value (${soulData.lives})")
                player.sendRichMessage("<green>You successfully gifted one life to ${target.name}")
                target.sendRichMessage("<green>You have been gifted one life by ${player.name}")
                player.updateScoreboardStuff()
                target.updateScoreboardStuff()
                return@addTargetExecute false
            }
    }

    fun assignBoogieman() {
        sendDebug("--------------------")
        sendDebug("Boogieman Assigment | Start of Boogieman assigment")
        val data = SoulManager.souls.assignBoogieman() ?: return
        sendDebug("Boogieman Assigment | SoulData choosen with key (${data.key})")
        val player1 = Bukkit.getPlayer(data.player1) ?: return
        val player2 = Bukkit.getPlayer(data.player2) ?: return
        sendDebug("Boogieman Assigment  | Boogieman players ${player1.name} | ${player2.name}")
        Bukkit.getOnlinePlayers().forEach {
            it.showTitle(Title.title("<yellow>Boogieman is being chosen".miniMessage(), "".miniMessage()))
            it.playSound(it, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
        }
        delay(20) {
            Bukkit.getOnlinePlayers().forEach {
                it.showTitle(Title.title("<yellow>Boogieman is being chosen.".miniMessage(), "".miniMessage()))
                it.playSound(it, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
            }
            delay(20) {
                Bukkit.getOnlinePlayers().forEach {
                    it.showTitle(Title.title("<yellow>Boogieman is being chosen..".miniMessage(), "".miniMessage()))
                    it.playSound(it, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                }
                delay(20) {
                    Bukkit.getOnlinePlayers().forEach {
                        it.showTitle(Title.title("<yellow>Boogieman is being chosen...".miniMessage(), "".miniMessage()))
                        it.playSound(it, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                    }
                    delay(20) {
                        sendDebug("Boogieman Assigment | Last message!")
                        Bukkit.getOnlinePlayers().forEach {
                            it.playSound(it, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F)
                            if (it.uniqueId != player1.uniqueId && it.uniqueId != player2.uniqueId) return@forEach it.showTitle(Title.title("<green>You are innocent!".miniMessage(), "".miniMessage()))
                            it.showTitle(Title.title("<red>You are the boogieman!".miniMessage(), "".miniMessage()))
                            SoulManager.boogieman = data.key
                        }
                    }
                }
            }
        }
    }

    private fun netherEvent() {
        for (player in Bukkit.getOnlinePlayers()) {
            val sequence = CameraSequence(Config.netherPoints, player)
            sequence()
        }
    }

    fun getColor(int: Int): String = when (int) {
        2 -> "<yellow>"
        3 -> "<green>"
        else -> "<dark_green>"
    }

}