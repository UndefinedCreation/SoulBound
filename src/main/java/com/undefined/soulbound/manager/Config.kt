package com.undefined.soulbound.manager

import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.camera.Point
import com.undefined.soulbound.exception.InvalidConfigException
import com.undefined.soulbound.util.sendDebug
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.util.Vector

object Config {

    private val config: FileConfiguration = SoulBound.INSTANCE.config

    val netherPoints: List<Point>
        get() {
            sendDebug("--------------------")
            sendDebug("Config | Getting nether-points configuration section...")
            val netherPointsSection = config.getList("nether-points") ?: throw InvalidConfigException()
            sendDebug("Config | Successfully gotten configuration section")
            sendDebug("Config | Getting keys...")
            return netherPointsSection.map {
                sendDebug("Config | Getting configuration section for (${it})")
                val pointSection = it as? LinkedHashMap<String, Int> ?: throw InvalidConfigException()
                sendDebug("Config | Successfully gotten configuration section for ($it)")
                Point(
                    Vector(
                        pointSection["x"] ?: throw InvalidConfigException(),
                        pointSection["y"] ?: throw InvalidConfigException(),
                        pointSection["z"] ?: throw InvalidConfigException()
                    ),
                    (pointSection["yaw"] ?: throw InvalidConfigException()).toFloat(),
                    (pointSection["pitch"] ?: throw InvalidConfigException()).toFloat(),
                    pointSection["duration"] ?: throw InvalidConfigException(),
                    pointSection["delay"] ?: 5,
                )
            }
        }

}