package com.undefined.soulbound

import com.undefined.api.UndefinedAPI
import com.undefined.api.scheduler.TimeUnit
import com.undefined.api.scheduler.repeatingTask
import com.undefined.soulbound.command.SoulboundCommand
import com.undefined.soulbound.game.SoulManager
import com.undefined.soulbound.game.loadSoulData
import com.undefined.soulbound.game.saveAll
import com.undefined.soulbound.util.TabManager
import org.bukkit.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team

class SoulBound : JavaPlugin() {

    companion object {
        lateinit var INSTANCE: SoulBound
        lateinit var UNDEFINED: UndefinedAPI
        lateinit var WORLD: World
    }


    override fun onEnable() {
        INSTANCE = this
        UNDEFINED = UndefinedAPI(this)
        WORLD = Bukkit.getWorld("world")!!

        Bukkit.getServerTickManager().isFrozen = true

        SoulManager.souls = WORLD.loadSoulData().toMutableList()
        val main = Bukkit.getScoreboardManager().mainScoreboard
        TabManager.colorTeams = HashMap<ChatColor, Team>().apply {
            ChatColor.entries.forEach { color ->
                val team = main.getTeam(color.name) ?: main.registerNewTeam(color.name)
                this[color] = team
                team.color = color
            }
        }

        SoulboundListener()
        SoulboundCommand()

        repeatingTask(5, TimeUnit.MINUTES) { WORLD.saveAll(SoulManager.souls) }

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

    }

    override fun onDisable() {
        WORLD.saveAll(SoulManager.souls)
    }
}
