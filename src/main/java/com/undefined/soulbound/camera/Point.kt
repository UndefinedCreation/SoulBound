package com.undefined.soulbound.camera

import com.undefined.soulbound.SoulBound
import org.bukkit.Location
import org.bukkit.util.Vector

data class Point(
    val position: Vector,
    val pointYaw: Float,
    val pointPitch: Float
) : Location(SoulBound.INSTANCE.WORLD, position.x, position.y, position.z, pointYaw, pointPitch)