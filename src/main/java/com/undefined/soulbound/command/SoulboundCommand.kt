package com.undefined.soulbound.command

import com.undefined.akari.CamaraSequence
import com.undefined.akari.objects.CamaraAlgorithmType
import com.undefined.akari.objects.CamaraPoint
import com.undefined.api.extension.string.miniMessage
import com.undefined.api.scheduler.TimeUnit
import com.undefined.api.scheduler.delay
import com.undefined.api.scheduler.repeatingTask
import com.undefined.api.sendLog
import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.VoiceChatImpl
import com.undefined.soulbound.animation.Animation
import com.undefined.soulbound.event.GameEndEvent
import com.undefined.soulbound.game.*
import com.undefined.soulbound.manager.Config
import com.undefined.soulbound.util.*
import com.undefined.stellar.StellarCommand
import de.maxhenkel.voicechat.api.Group
import de.maxhenkel.voicechat.api.VoicechatConnection
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.random.Random


object SoulboundCommand {

    private var autoSessionEndTask: BukkitTask? = null
    val locations: HashMap<UUID, Location> = hashMapOf()
    val gameMode: HashMap<UUID, GameMode> = hashMapOf()

    init {
        val main = StellarCommand("soulbound")
            .addRequirements("undefined.event.soulbound")
        main.addArgument("start")
            .addExecution<Player> {
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
            }

        main.addArgument("sendToGroup")
            .addStringArgument("name")
            .addExecution<Player> {
                val group: Group = VoiceChatImpl.API.groupBuilder()
                    .setPersistent(false)
                    .setName(getArgument<String>("name"))
                    .build()

                for (player in Bukkit.getOnlinePlayers()) {
                    val connection: VoicechatConnection = VoiceChatImpl.API.getConnectionOf(player.uniqueId) ?: return@addExecution
                    connection.group = group
                }
            }

        main.addArgument("lives")
            .addOnlinePlayersArgument("target")
            .addExecution<Player> {
                val target = getArgument<Player>("target")
                val soulData: SoulData = target.getSoulData() ?: return@addExecution sender.sendRichMessage("<red>Invalid player!")
                sender.sendRichMessage("<green>${target.name} has ${getColor(soulData.lives)}${soulData.lives} <green>lives.")
                return@addExecution
            }

        main.addArgument("addLife")
            .addOnlinePlayersArgument("target")
            .addExecution<Player> {
                val target = getArgument<Player>("target")
                val soulData: SoulData = target.getSoulData() ?: return@addExecution sender.sendRichMessage("<red>Invalid player!")
                sender.sendRichMessage("<green>Life has been added!")
                soulData.lives++
                target.updateScoreboardStuff()
                target.getSoulMate()?.updateScoreboardStuff()
            }


        val eventsSubCommand = main.addArgument("events")
        eventsSubCommand.addArgument("nether")
            .addExecution<Player> {
                netherEvent(sender)
            }

        val configSubCommand = main.addArgument("config")
        val configGet = configSubCommand.addArgument("get")
        configGet.addArgument("netherAnimationDelay")
            .addExecution<Player> {
                sender.sendRichMessage("<gray>The nether animation delay is set to: ${Config.netherAnimationDelay}")
            }

        val configSet = configSubCommand.addArgument("set")
        configSet
            .addArgument("netherAnimationDelay")
            .addIntegerArgument("number")
            .addExecution<Player> {
                val number = getArgument<Int>("number")
                Config.netherAnimationDelay = number
                sendDebug("Config Update | Nether animation delay has been modified to (${number})")
            }

        main.addArgument("transfer")
            .addStringArgument("oldPlayer")
            .addStringArgument("newPlayer")
            .addExecution<Player> {
                sendDebug("--------------------")
                sendLog("Transfer Command | Getting old player")
                val oldPlayer = Bukkit.getOfflinePlayer(getArgument<String>("string"))
                if (oldPlayer.hasPlayedBefore()) return@addExecution
                sendLog("Transfer Command | Old player has joined before")
                sendLog("Transfer Command | Getting new player")
                val newPlayer = Bukkit.getOfflinePlayer(getArgument<String>("newPlayer"))
                val soulData = oldPlayer.getSoulData() ?: run {
                    sendLog("Transfer Command | Old player doesn't have any souldata")
                    return@addExecution sender.sendRichMessage("<red>Invalid player!")
                }

                sendLog("Transfer Command | Giving soulbound to new member")
                if (soulData.player1 == oldPlayer.uniqueId) soulData.player1 = newPlayer.uniqueId else soulData.player2 = newPlayer.uniqueId

                newPlayer.player?.run { this.updateScoreboardStuff() }
                sendLog("Transfer Command | Success!")
                sender.sendRichMessage("<green>Transferred!")
            }

        main.addArgument("get")
            .addExecution<Player> {
                sender.sendRichMessage("<gray>----------")
                SoulBound.WORLD.persistentDataContainer.keys.forEach {
                    val sould = SoulData.fromString(SoulBound.WORLD.persistentDataContainer.get(it, PersistentDataType.STRING))
                    sender.sendRichMessage("<gray>${it.key}: <aqua>${Bukkit.getOfflinePlayer(sould.player1).name} | ${Bukkit.getOfflinePlayer(sould.player2).name} | ${sould.lives}")
                }
                sender.sendRichMessage("<gray>----------")
            }

        main.addArgument("toggleDebugMode")
            .addExecution<Player> {
                sendDebug("--------------------")
                debugMode = !debugMode
                sendDebug("Toggle Debug | Debug mode has been toggled to (${debugMode})")
                sender.sendRichMessage("<green>Debug mode has been toggled to $debugMode", true)
            }

        main.addArgument("toggleCooldownShorter")
            .addExecution<Player> {
                sendDebug("--------------------")
                timerCooldownShorter = !timerCooldownShorter
                sendDebug("Toggle Cooldown Timer | Cooldown timer has been toggled to ($timerCooldownShorter)")
                sender.sendRichMessage("<green>Timer shorter mode has been toggled to $timerCooldownShorter", true)
            }

        main.addArgument("end")
            .addExecution<Player> {
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
            }

        main.addArgument("removeLife")
            .addOnlinePlayersArgument("players")
            .addExecution<Player> {
                sendDebug("--------------------")
                val target = getArgument<Player>("players")
                sendDebug("Remove Life | Removing life for (${target.name})")
                val soulData: SoulData = target.getSoulData() ?: run {
                    sendDebug("Remove Life | Invalid Player")
                    sender.sendRichMessage("<red>Invalid player!")
                    return@addExecution
                }
                sendDebug("Remove Life | Fetched target SoulData")
                sender.sendRichMessage("<green>Life has been removed!")
                soulData.lives--
                target.updateScoreboardStuff()
                target.getSoulMate()?.updateScoreboardStuff()
                sendDebug("Remove Life | End")
            }

        main.addArgument("reset")
            .addExecution<Player> {
                sendDebug("--------------------")

                SoulBound.WORLD.persistentDataContainer.keys.forEach { SoulBound.WORLD.persistentDataContainer.remove(it) }
                sendDebug("Reset Lives | Removed all World PDC")
                SoulManager.souls.clear()
                sendDebug("Reset Lives | Cleared all SoulData")

                sender.sendRichMessage("<red>Soul bound have been cleared", true)
                Bukkit.getOnlinePlayers().forEach { it.updateScoreboardStuff() }

                autoSessionEndTask?.cancel()
                sendDebug("Reset Lives | Cancel AutoSessionEndTask")

            }

        val boogieman = main.addArgument("boogieman")
        boogieman.addArgument("clear")
            .addExecution<Player> {
                sendDebug("--------------------")
                SoulManager.boogieman = null
                sendDebug("Boogieman Admin Clear | Boogieman has been cleared")
                Bukkit.getOnlinePlayers().forEach {
                    it.showTitle(Title.title("<Green>Boogieman has killed!".miniMessage(), "<green>Was cleared by admin".miniMessage()))
                    it.playSound(it, Sound.BLOCK_ANVIL_LAND, 1.0F, 1.0F)
                }
            }

        boogieman.addArgument("roll")
            .addExecution<Player> {
                if (SoulManager.boogieman == null) {
                    sender.sendRichMessage("<red>There is already a boogieman!")
                }
                assignBoogieman()
            }

        main.addArgument("setSoulmate")
            .addOfflinePlayersArgument("player")
            .addOfflinePlayersArgument("target")
            .addIntegerArgument("amount")
            .addExecution<Player> {
                sendDebug("--------------------")
                sendDebug("Set Soulmate | Getting all variables")
                val player = getArgument<OfflinePlayer>("player")
                val target = getArgument<OfflinePlayer>("target")
                val amountOfLives = getArgument<Int>("amount")
                sendDebug("Set Soulmate | Gotten all the variables")

                SoulManager.souls.filter { it.player1 == player.uniqueId || it.player2 == player.uniqueId || it.player1 == target.uniqueId || it.player2 == target.uniqueId }.forEach {
                    SoulBound.WORLD.persistentDataContainer.remove(NamespacedKey(SoulBound.INSTANCE, it.key.toString()))
                    SoulManager.souls.remove(it)
                }
                sendDebug("Set Soulmate | Removed all the previous souls")

                player.player?.health = 20.0
                target.player?.health = 20.0
                sendDebug("Set Soulmate | Set all health to 20.0")

                val soulData = SoulData(player.uniqueId, target.uniqueId, amountOfLives)
                sendDebug("Set Soulmate | Make SoulData")
                SoulManager.souls.add(soulData)
                sendDebug("Set Soulmate | Add SoulData to SoulManager")

                player.player?.updateScoreboardStuff()
                target.player?.updateScoreboardStuff()
                sendDebug("Set Soulmate | Update ScoreBoard stuff")
                sender.sendRichMessage("<green>You have successfully set a soulmate!", true)
            }

        val skin = main.addArgument("skin")

        skin.addArgument("normal")
            .addExecution<Player> {

            }

        skin.addArgument("gray")
            .addExecution<Player> {
//                sender.playerProfile.textures.setSkin()
            }

        main.register(SoulBound.INSTANCE)

        StellarCommand("gift")
            .addOnlinePlayersArgument("target")
            .addExecution<Player> {
                sendDebug("--------------------")
                sendDebug("Life gifting | Life from player (${sender.name})")
                val soulData: SoulData = sender.getSoulData() ?: return@addExecution
                val target = getArgument<Player>("target")
                sendDebug("Life gifting | Getting souldata with key (${soulData.key})")
                if (sender.uniqueId == target.uniqueId) {
                    sendDebug("Life gifting | Player can't gift himself lives")
                    sender.sendRichMessage("<red>You cannot gift yourself lives!")
                }
                val targetSoulData: SoulData = target.getSoulData() ?: run {
                    sender.sendRichMessage("<red>That player is invalid!")
                    sendDebug("Life gifting | Target player doesn't have any souldata")
                    return@addExecution
                }
                if (soulData.lives < targetSoulData.lives) {
                    sender.sendRichMessage("<red>You cannot gift to somebody with more lives than yourself!")
                    sendDebug("Life gifting | Life from player has less lives than its self.")
                    return@addExecution
                }
                if (soulData.lives < 2) {
                    sender.sendRichMessage("<red>You don't have enough lives to be able to gift any!")
                    sendDebug("Life gifting | Life from player has less than 2 life")
                    return@addExecution
                }
                val soulmate = sender.getSoulMate()
                sendDebug("Life gifting | Life from player soulmate (${soulmate?.name})")
                if (soulmate != null && target.uniqueId == soulmate.uniqueId) {
                    sender.sendRichMessage("<red>You cannot give lives to your soulmate!")
                    sendDebug("Life gifting | Trying to give live to its own soulmate")
                    return@addExecution
                }
                soulData.lives -= 1
                sendDebug("Life gifting | Removed life from player. Value (${soulData.lives})")
                targetSoulData.lives += 1
                sendDebug("Life gifting | Added life to target player. Value (${soulData.lives})")
                sender.sendRichMessage("<green>You successfully gifted one life to ${target.name}")
                target.sendRichMessage("<green>You have been gifted one life by ${sender.name}")
                sender.updateScoreboardStuff()
                target.updateScoreboardStuff()
                return@addExecution
            }
            .register(SoulBound.INSTANCE)
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

    private fun netherEvent(player: Player) {

        Bukkit.getOnlinePlayers().forEach {
            it.showTitle(Title.title("<gray>Next step in life is".miniMessage(), "".miniMessage()))
            it.playSound(it, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
        }
        delay(20) {
            Bukkit.getOnlinePlayers().forEach {
                it.showTitle(Title.title("<gray>Next step in life is.".miniMessage(), "".miniMessage()))
                it.playSound(it, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
            }
            delay(20) {
                Bukkit.getOnlinePlayers().forEach {
                    it.showTitle(Title.title("<gray>Next step in life is..".miniMessage(), "".miniMessage()))
                    it.playSound(it, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                }
                delay(20) {
                    Bukkit.getOnlinePlayers().forEach {
                        it.showTitle(Title.title("<gray>Next step in life is...".miniMessage(), "".miniMessage()))
                        it.playSound(it, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0F, 1.0F)
                    }

                    delay(20) {
                        delay(75) { // TODO Change
                            Animation.NETHER.animation(Location(player.location.world, -424.0, 111.0, -1803.0))
                            Bukkit.getOnlinePlayers().forEach { it.playSound(it, Sound.AMBIENT_NETHER_WASTES_MOOD, 1000F, 0.7F) }

                            spawnP(Location(player.location.world, -424.0, 111.0, -1803.0))

                        }

                        delay(35) {
                            Bukkit.getOnlinePlayers().forEach {
                                it.playSound(it, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f)
                            }
                        }

                        delay(196) {
                            Bukkit.getOnlinePlayers().forEach {
                                it.showTitle(Title.title("<RED>HELL.".miniMessage(), "".miniMessage()))
                                it.playSound(it, Sound.BLOCK_END_PORTAL_SPAWN, 1f, 0.1f)
                            }
                        }


                        Bukkit.getOnlinePlayers().forEach {
                            it.playSound(it, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f)
                        }

                        for (player in Bukkit.getOnlinePlayers()) {

                            Bukkit.getOnlinePlayers().forEach {
                                player.hidePlayer(it)
                            }

                            CamaraSequence(SoulBound.INSTANCE, CamaraAlgorithmType.SIMPLE)
                                .addPoint(CamaraPoint(SoulBound.WORLD, player.eyeLocation.x, player.eyeLocation.y, player.eyeLocation.z, player.eyeLocation.yaw, player.eyeLocation.pitch, durationIntoPoint = 0, delay = 0))
                                .addPoint(CamaraPoint(SoulBound.WORLD, player.x, 175.0, player.z,90f, 90f, durationIntoPoint = 30, delay = 5))
                                .addPoint(CamaraPoint(SoulBound.WORLD, -415.0, 175.0, -1803.0,90f, 90f, durationIntoPoint = 20, delay = 0))
                                .addPoint(CamaraPoint(SoulBound.WORLD, -415.0, 123.0, -1803.0,90f, -25f, durationIntoPoint = 20, delay = 0))
                                .addPoint(CamaraPoint(SoulBound.WORLD, -415.0, 119.0, -1803.0,90f, 15f, durationIntoPoint = 30, delay = 0))
                                .addPoint(CamaraPoint(SoulBound.WORLD, -415.0, 118.0, -1803.0,90f, 0f, durationIntoPoint = 30, delay = 0))
                                .addPoint(CamaraPoint(SoulBound.WORLD, -415.0, 117.0, -1803.0,90f, -15f, durationIntoPoint = 30, delay = 0))
                                .addPoint(CamaraPoint(SoulBound.WORLD, -415.0, 114.0, -1803.0,90f, -25f, durationIntoPoint = 30, delay = 60))
                                .play(player)

                            locations[player.uniqueId] = player.location
                            gameMode[player.uniqueId] = player.gameMode
                            player.gameMode = GameMode.SPECTATOR

                            player.teleport(Location(SoulBound.WORLD, -415.0, 114.0, -1803.0, 90f, -25f))

                        }

                        delay(321) {
                            Bukkit.getOnlinePlayers().forEach { player ->
                                locations[player.uniqueId]?.let { player.teleport(it) }
                                gameMode[player.uniqueId]?.let { player.gameMode = it }
                                Bukkit.getOnlinePlayers().forEach {
                                    player.showPlayer(it)
                                }
                            }
                            locations.clear()
                            gameMode.clear()
                        }

                    }
                }
            }
        }
    }

    fun spawnP(location: Location) {

        val list = listOf(Particle.FALLING_SPORE_BLOSSOM, Particle.REVERSE_PORTAL)

        repeatingTask(30, 6) {
            for (x in 0..50) {
                val location = location.clone().add(Random.nextInt(0, 10).toDouble(), Random.nextInt(0, 15).toDouble(), Random.nextInt(-10, 10).toDouble())
                location.world.spawnParticle(list.random(), location, 5)
            }
        }

    }

    fun getColor(int: Int): String = when (int) {
        2 -> "<yellow>"
        3 -> "<green>"
        else -> "<dark_green>"
    }

}