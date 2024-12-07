package com.undefined.soulbound.animation

import com.undefined.api.extension.string.asString
import com.undefined.api.scheduler.repeatingTask
import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.manager.Config
import com.undefined.soulbound.util.WorldEditUtil
import com.undefined.soulbound.util.sendDebug
import org.bukkit.Location
import java.io.File

object Animation {

    object NETHER {

        fun animation(location: Location) {
            sendDebug("--------------------")
            sendDebug("Animation Nether | At location (${location.asString()})")

            val schems = SoulBound.ANIMATION_FOLDER.list()?.filter { it.contains(".schem") }
                ?.map { File(SoulBound.ANIMATION_FOLDER, it) } ?: return
            sendDebug("Animation Nether | Gotten all schematics ${schems.map { it.name }}")

            var amount = 1

            repeatingTask(Config.netherAnimationDelay, times = 5) {
                sendDebug("Animation Nether | Pasting Stage ($amount)")
                WorldEditUtil.paste(schems.firstOrNull { it.name.contains("s$amount") }, location)
                amount++
            }
        }

    }

}