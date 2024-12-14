package com.undefined.soulbound

import com.undefined.api.UndefinedAPI
import com.undefined.api.scheduler.TimeUnit
import com.undefined.api.scheduler.delay
import com.undefined.api.scheduler.repeatingTask
import com.undefined.soulbound.command.SoulboundCommand
import com.undefined.soulbound.game.SoulManager
import com.undefined.soulbound.game.loadSoulData
import com.undefined.soulbound.game.saveAll
import com.undefined.soulbound.util.TabManager
import com.undefined.soulbound.util.sendDebug
import de.maxhenkel.voicechat.api.BukkitVoicechatService
import org.bukkit.*
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team
import org.slf4j.Logger
import java.io.File


class SoulBound : JavaPlugin() {

    companion object {
        lateinit var INSTANCE: SoulBound
        lateinit var UNDEFINED: UndefinedAPI
        lateinit var WORLD: World
        lateinit var LOGGER: Logger
        lateinit var CONFIG: FileConfiguration
        lateinit var ANIMATION_FOLDER: File
    }

    var voiceChat: VoiceChatImpl? = null

    override fun onEnable() {
        sendDebug("--------------------")
        INSTANCE = this
        UNDEFINED = UndefinedAPI(this)
        WORLD = Bukkit.getWorld("world")!!
        LOGGER = slF4JLogger
        CONFIG = this.config
        saveDefaultConfig()

//        val service = server.servicesManager.load(BukkitVoicechatService::class.java) ?: throw IllegalStateException("HELP ME")
//        voiceChat = VoiceChatImpl()
//        service.registerPlugin(voiceChat)

        sendDebug("Main | Creating Folders")
        ANIMATION_FOLDER = File(dataFolder, "animation").apply { mkdir() }
        Bukkit.getServerTickManager().isFrozen = true
        sendDebug("Main | Frozen ticks")

        SoulManager.souls = WORLD.loadSoulData().toMutableList()
        sendDebug("Main | Loaded in SoulData")
        val main = Bukkit.getScoreboardManager().mainScoreboard
        TabManager.colorTeams = HashMap<ChatColor, Team>().apply {
            ChatColor.entries.forEach { color ->
                val team = main.getTeam(color.name) ?: main.registerNewTeam(color.name)
                this[color] = team
                team.color = color
            }
        }
        sendDebug("Main | Setup TagManager")

        SoulboundListener()
        sendDebug("Main | Loaded SoulboundListener")
        SoulboundCommand
        sendDebug("Main | Loaded SoulboundCommand")

        repeatingTask(5, TimeUnit.MINUTES) {
            sendDebug("Main | Saving all SoulData...")
            WORLD.saveAll(SoulManager.souls)
            sendDebug("Main | Successfully Saved all SoulData")
        }

        val tntRecipe = ShapedRecipe(NamespacedKey(this, "TNT"), ItemStack(Material.TNT))
        tntRecipe.shape(
            "PSP",
            "SGS",
            "PSP"
        )

        tntRecipe.setIngredient('P', Material.PAPER)
        tntRecipe.setIngredient('S', Material.SAND)
        tntRecipe.setIngredient('G', Material.GUNPOWDER)

        Bukkit.addRecipe(tntRecipe)
        sendDebug("Main | Added TNT recipe")

        val nameTagRecipe = ShapedRecipe(NamespacedKey(this, "NAMETAG"), ItemStack(Material.NAME_TAG))
        nameTagRecipe.shape(
            " IS",
            " LI",
            "L  "
        )

        nameTagRecipe.setIngredient('S', Material.STRING)
        nameTagRecipe.setIngredient('I', Material.IRON_INGOT)
        nameTagRecipe.setIngredient('L', Material.LEATHER)

        Bukkit.addRecipe(nameTagRecipe)
    }

    override fun onDisable() {
        WORLD.saveAll(SoulManager.souls)
        voiceChat?.let {
            server.servicesManager.unregister(it)
            LOGGER.info("Successfully unregistered voice chat broadcast plugin")
        }
    }

}
