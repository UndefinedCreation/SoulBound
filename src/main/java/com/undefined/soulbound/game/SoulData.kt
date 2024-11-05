package com.undefined.soulbound.game

import java.util.UUID

data class SoulData(
    val player1: UUID,
    val player2: UUID,
    var lives: Int,
    val key: UUID = UUID.randomUUID()
) {

    companion object {
        fun fromString(string: String): SoulData {
            val content = string.split("@")
            val uuid1 = UUID.fromString(content[0])
            val uuid2 = UUID.fromString(content[1])
            val lives = content[2].toInt()
            val key = UUID.fromString(content[3])
            return SoulData(uuid1, uuid2, lives, key)
        }
    }

    override fun toString(): String {
        return "$player1@$player2@$lives@$key"
    }
}
