package com.undefined.soulbound.game

import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.util.sendDebug
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.*

object SoulManager {
    var souls: MutableList<SoulData> = mutableListOf()
    var boogieman: UUID? = null
}

fun Collection<Player>.giveSoulBounds() {
    val players: MutableList<Player> = this.toMutableList()
    sendDebug("--------------------")
    sendDebug("Give SoulBounds | Called with players: ${players.joinToString(", ")}")

    for (x in 1 .. players.size / 2) {
        val player1 = players.random().also { players.remove(it) }
        val player2 = players.random().also { players.remove(it) }
        player1.health = 20.0
        player2.health = 20.0

        val lives = Random().nextInt(4,6)
        val soulData = SoulData(player1.uniqueId, player2.uniqueId, lives)

        SoulManager.souls.add(soulData)
        sendDebug("Added SoulData for players: ${player1.name}, ${player2.name}")
    }
}

fun Collection<SoulData>.assignBoogieman(): SoulData? = this.filter { it.lives > 1 }.randomOrNull()

fun World.addSoulData(soul: SoulData) {
    persistentDataContainer[NamespacedKey(SoulBound.INSTANCE, soul.key.toString()), PersistentDataType.STRING] = soul.toString()
}

fun World.loadSoulData(): List<SoulData> =
     persistentDataContainer.keys.map { SoulData.fromString(persistentDataContainer.get(it, PersistentDataType.STRING)!!) }

fun World.saveAll(list: List<SoulData>) = list.forEach { addSoulData(it) }

fun World.saveSoulData(souls: List<SoulData>) {
    SoulManager.souls.addAll(souls)
}

fun OfflinePlayer.getSoulData(): SoulData? =
    SoulManager.souls.filter { it.player1 == uniqueId || it.player2 == uniqueId }.getOrNull(0)

fun OfflinePlayer.getSoulMate(): Player? {
    val soulData = getSoulData() ?: return null
    return if (uniqueId == soulData.player1) Bukkit.getPlayer(soulData.player2) else Bukkit.getPlayer(soulData.player1)
}