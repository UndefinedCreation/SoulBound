package com.undefined.soulbound.util

import com.undefined.soulbound.game.getSoulData
import com.undefined.soulbound.manager.Config
import com.undefined.soulbound.skin.SkinManager
import com.undefined.soulbound.skin.SkinManager.setSkins
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

object TabManager {
    var colorTeams: HashMap<ChatColor, Team> = hashMapOf()
}

fun Player.updateScoreboardStuff() {
    sendDebug("TabManager | Updated scoreboard stuff")
    val data = getSoulData() ?: return

    val team = when(data.lives) {
        1 -> {
            if (name !in Config.excludedGrayscalePlayers)
                SkinManager.getGraySkin(name) { setSkins(it) }
            TabManager.colorTeams[ChatColor.RED]!!
        }
        2 -> TabManager.colorTeams[ChatColor.YELLOW]!!
        3 -> TabManager.colorTeams[ChatColor.GREEN]!!
        4, 5, 6 -> TabManager.colorTeams[ChatColor.DARK_GREEN]!!
        else -> TabManager.colorTeams[ChatColor.GRAY]
    } ?: return
    team.addPlayer(this)
}