package com.undefined.soulbound.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player

fun Player.sendRichMessage(message: String, broadcastToOps: Boolean) {
    sendRichMessage(message)
    if (broadcastToOps) for (onlinePlayer in Bukkit.getOnlinePlayers()) {
        if (!onlinePlayer.isOp) continue
        onlinePlayer.sendRichMessage("<gray>[$message<gray>]")
    }
}