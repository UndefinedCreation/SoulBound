package com.undefined.soulbound.camera

import jdk.javadoc.doclet.Taglet.Location
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import java.awt.Component

class CameraSequence(
    points: List<Location>,
    private val player: Player,
    private val component: Component,
    teleportDuration: Int = 15,
    timelineSeparation: Int = 30,
) {

    val currentPoint: Location = points.first()
    val itemDisplay: ItemDisplay? = null

    fun start() {
        itemDisplay.
    }

}