package com.undefined.soulbound.util

import com.undefined.soulbound.game.getSoulData
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

object TabManager {
    var colorTeams: HashMap<ChatColor, Team> = hashMapOf()
}

fun Player.updateScoreboardStuff() {
    val data = getSoulData() ?: return

    val team = when(data.lives) {
        1 -> TabManager.colorTeams[ChatColor.RED]!!
        2 -> TabManager.colorTeams[ChatColor.YELLOW]!!
        3 -> TabManager.colorTeams[ChatColor.GREEN]!!
        4, 5, 6 -> TabManager.colorTeams[ChatColor.DARK_GREEN]!!
        else -> TabManager.colorTeams[ChatColor.GRAY]
    } ?: return
    team.addPlayer(this)
}