package com.undefined.soulbound.camera

import com.undefined.api.scheduler.delay
import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.manager.Config
import com.undefined.soulbound.util.sendDebug
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

class CameraSequence(
    val points: List<Point>,
    private val player: Player
) {

    operator fun invoke() {
        sendDebug("--------------------")
        sendDebug("Camera Sequence | Started sequence for (${player.name})")
        val itemDisplay = SoulBound.WORLD.spawn(player.eyeLocation, ItemDisplay::class.java) { entity ->
            entity.setItemStack(ItemStack(Material.AIR))
            entity.viewRange = Float.MAX_VALUE
            entity.teleportDuration = 30
        }
        sendDebug("Camera Sequence | Spawned item display")
        delay(1) {
            player.gameMode = GameMode.SPECTATOR
            sendDebug("Camera Sequence | Set player game mode to spectator")
            player.spectatorTarget = itemDisplay
            sendDebug("Camera Sequence | Add player as spectator target")
            val locationWithY = player.location.clone()
            locationWithY.y = 175.0
            itemDisplay.teleport(locationWithY)
            sendDebug("Camera Sequence | Teleported item display to (${itemDisplay.location.toVector()})")
            sendDebug("Camera Sequence | Starting runnable...")
            delay(Config.netherFlyingUpwardDuration) {
                teleportToPoints(itemDisplay)
            }
        }
    }

    private fun teleportToPoints(display: ItemDisplay) {
        val path = CameraAlgorithm.generatePerTickPath(Point(
            player.location.toVector(),
            player.location.yaw,
            player.location.pitch
        ), points)
        sendDebug("Camera Sequence - Generated per tick path: (${path})")
//        val smoothPath: Map<Tick, Point> = CameraAlgorithm.generateSmoothPath(perTickPath)
//        sendDebug("Camera Sequence | Generated smooth path: (${smoothPath.toList().joinToString(", ") { entry -> "${entry.first}: ${entry.second}" }})")

        object : BukkitRunnable() {
            var currentTick: Tick = 1

            override fun run() {
                if (path.keys.sum() < currentTick) return this.cancel()
                val currentPoint = path[currentTick] ?: run {
                    currentTick++
                    return
                }
                println(currentPoint)
                val currentTickIndex = path.keys.indexOf(currentTick)
                val lastTick = path.toList()[currentTickIndex - 1].first
                println("currentTick: $currentTick, pathSize: ${path.size}, lastTick: $lastTick, currentTickIndex: $currentTickIndex")
                val tickDifference = currentTick - lastTick
                display.teleportDuration = tickDifference
                display.teleport(currentPoint)
                currentTick++
            }
        }.runTaskTimer(SoulBound.INSTANCE, 0, 1)
//        println("\n${points.joinToString(", ") { it.position.toString() }}\n")
//        val point = points[currentPoint]
//        display.teleportDuration = point.duration
//        delay(1) {
//            display.teleport(point)
//            sendDebug("Camera Sequence | Teleported display to (${point.position})")
//            if (points.lastIndex == currentPoint) return@delay
//            currentPoint += 1
//            delay(point.duration + point.delay) {
//                teleportToPoints(display)
//            }
//        }
    }
}