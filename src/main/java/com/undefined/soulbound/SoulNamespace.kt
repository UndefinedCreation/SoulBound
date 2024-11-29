package com.undefined.soulbound

import org.bukkit.NamespacedKey

object SoulNamespace {

    object PLAYER {
        val SOUL_BOUND = NamespacedKey("soulbound", "SOULBOUND_PLAYER")
        val LIVES = NamespacedKey("soulbound", "LIVES_PLAYER")
    }

    object ITEM {
        val TNT = NamespacedKey("soulbound", "TNT")
    }

}