package com.undefined.soulbound.manager

import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.exception.InvalidConfigException
import org.bukkit.configuration.file.FileConfiguration

object Config {

    private val config: FileConfiguration = SoulBound.INSTANCE.config

    val excludedGrayscalePlayers = config.getStringList("excluded-grayscale-players")

    val netherFlyingUpwardDuration = 30
    var netherAnimationDelay: Int = config.getInt("nether-animation-delay")
        set(value) {
            config.set("nether-animation-delay", value)
            field = config.getInt("nether-animation-delay")
        }


    fun getValueFromSection(section: LinkedHashMap<String, Any?>, name: String): Double {
        val value = section[name] ?: throw InvalidConfigException()
        if (value is Int) return value.toDouble()
        if (value is Double) return value
        return 0.0
    }

}