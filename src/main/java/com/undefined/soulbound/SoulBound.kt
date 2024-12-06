package com.undefined.soulbound

import com.undefined.api.UndefinedAPI
import com.undefined.api.scheduler.TimeUnit
import com.undefined.api.scheduler.repeatingTask
import com.undefined.soulbound.command.SoulboundCommand
import com.undefined.soulbound.game.SoulManager
import com.undefined.soulbound.game.loadSoulData
import com.undefined.soulbound.game.saveAll
import com.undefined.soulbound.util.TabManager
import com.undefined.soulbound.util.sendDebug
import org.bukkit.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team
import org.slf4j.Logger

class SoulBound : JavaPlugin() {

    companion object {
        lateinit var INSTANCE: SoulBound
        lateinit var UNDEFINED: UndefinedAPI
        lateinit var WORLD: World
        lateinit var LOGGER: Logger
    }

    override fun onEnable() {
        sendDebug("--------------------")
        INSTANCE = this
        UNDEFINED = UndefinedAPI(this)
        WORLD = Bukkit.getWorld("world")!!
        LOGGER = slF4JLogger

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
        SoulboundCommand()
        sendDebug("Main | Loaded SoulboundCommand")

        repeatingTask(5, TimeUnit.MINUTES) {
            sendDebug("Main | Saving all SoulData...")
            WORLD.saveAll(SoulManager.souls)
            sendDebug("Main | Successfully Saved all SoulData")
        }

        val sr = ShapedRecipe(NamespacedKey(this, "TNT"), ItemStack(Material.TNT))
        sr.shape(
            "PSP",
            "SGS",
            "PSP"
        )

        sr.setIngredient('P', Material.PAPER)
        sr.setIngredient('S', Material.SAND)
        sr.setIngredient('G', Material.GUNPOWDER)

        Bukkit.addRecipe(sr)
        sendDebug("Main | Added TNT recipe")
    }

    override fun onDisable() {
        WORLD.saveAll(SoulManager.souls)
    }
}
