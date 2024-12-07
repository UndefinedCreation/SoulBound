package com.undefined.soulbound.camera

import com.undefined.soulbound.SoulBound
import org.bukkit.Location
import org.bukkit.util.Vector

data class Point(
    val position: Vector,
    val pointYaw: Float,
    val pointPitch: Float,
    val duration: Int = 15,
    val delay: Int = 10,
) : Location(SoulBound.WORLD, position.x, position.y, position.z, pointYaw, pointPitch)