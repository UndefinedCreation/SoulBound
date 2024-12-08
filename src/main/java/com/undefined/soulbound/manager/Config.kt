package com.undefined.soulbound.manager

import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.camera.Point
import com.undefined.soulbound.exception.InvalidConfigException
import com.undefined.soulbound.util.sendDebug
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.util.Vector

object Config {

    private val config: FileConfiguration = SoulBound.INSTANCE.config

    val netherFlyingUpwardDuration = 30
    var netherAnimationDelay: Int = config.getInt("nether-animation-delay")
        set(value) {
            config.set("nether-animation-delay", value)
            field = config.getInt("nether-animation-delay")
        }

    val netherPoints: List<Point>
        get() {
            sendDebug("--------------------")
            sendDebug("Config | Getting nether-points configuration section...")
            val netherPointsSection = config.getList("nether-points") ?: throw InvalidConfigException()
            sendDebug("Config | Successfully gotten configuration section")
            sendDebug("Config | Getting keys...")
            return netherPointsSection.map {
                sendDebug("Config | Getting configuration section for (${it})")
                val section = it as? LinkedHashMap<String, Any?> ?: throw InvalidConfigException()
                sendDebug("Config | Successfully gotten configuration section for ($it)")
                Point(
                    Vector(
                        getValueFromSection(section, "x"),
                        getValueFromSection(section, "y"),
                        getValueFromSection(section, "z")
                    ),
                    getValueFromSection(section, "yaw").toFloat(),
                    getValueFromSection(section, "pitch").toFloat(),
                    getValueFromSection(section, "duration").toInt(),
                    getValueFromSection(section, "delay").toInt(),
                )
            }
        }

    fun getValueFromSection(section: LinkedHashMap<String, Any?>, name: String): Double {
        val value = section[name] ?: throw InvalidConfigException()
        if (value is Int) return value.toDouble()
        if (value is Double) return value
        return 0.0
    }

}