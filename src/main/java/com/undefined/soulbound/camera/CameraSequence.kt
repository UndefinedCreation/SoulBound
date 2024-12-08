package com.undefined.soulbound.camera

import com.undefined.api.scheduler.delay
import com.undefined.api.scheduler.repeatingTask
import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.util.sendDebug
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import kotlin.math.floor

class CameraSequence(
    val points: List<Point>,
    private val player: Player
) {

    var currentPoint: Int = 0

    operator fun invoke() {
        sendDebug("--------------------")
        sendDebug("Camera Sequence | Started sequence for (${player.name})")
        val itemDisplay = SoulBound.WORLD.spawn(player.eyeLocation, ItemDisplay::class.java) { entity ->
            entity.setItemStack(ItemStack(Material.AIR))
            entity.viewRange = Float.MAX_VALUE
            entity.teleportDuration = 30
        }
        val perTickPath = CameraAlgorithm.generatePerTickPath(points)
        val smoothPath = CameraAlgorithm.generateSmoothPath(perTickPath)
        sendDebug("Camera Sequence | Spawned item display")
        object : BukkitTask {

        }
        player.gameMode = GameMode.SPECTATOR
        player.teleport(itemDisplay)
        sendDebug("Camera Sequence | Set player game mode to spectator")
        delay(1) {
            player.spectatorTarget = itemDisplay
            sendDebug("Camera Sequence | Add player as spectator target")
            val locationWithY = player.location.clone()
            locationWithY.y = 175.0
            itemDisplay.teleport(locationWithY)
            sendDebug("Camera Sequence | Teleport item display to (${itemDisplay.location.toVector()})")
            delay(40) {
                teleportToPoints(itemDisplay)
            }
            sendDebug("Camera Sequence | Teleported player to points")
        }
    }

    private fun teleportToPoints(display: ItemDisplay) {
        println("\n${points.joinToString(", ") { it.position.toString() }}\n")
        val point = points[currentPoint]
        display.teleportDuration = point.duration
        delay(1) {
            display.teleport(point)
            sendDebug("Camera Sequence | Teleported display to (${point.position})")
            if (points.lastIndex == currentPoint) return@delay
            currentPoint += 1
            delay(point.duration + point.delay) {
                teleportToPoints(display)
            }
        }
    }
}