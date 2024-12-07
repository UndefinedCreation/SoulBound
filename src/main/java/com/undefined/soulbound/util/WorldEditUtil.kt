package com.undefined.soulbound.util

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Location
import java.io.File
import java.io.FileInputStream

object WorldEditUtil {

    fun paste(schematic: File?, location: Location) {
        sendDebug("Schematic paste | Start paste. Is file valid: (${schematic != null})")

        schematic ?: return
        val clipboardFormat = ClipboardFormats.findByFile(schematic)
        sendDebug("Schematic paste | Clipboard format (${clipboardFormat?.name})")
        val reader = clipboardFormat?.getReader(FileInputStream(schematic)) ?: return
        sendDebug("Schematic paste | Reader (${reader.dataVersion})")
        val clipboard = reader.read()
        sendDebug("Schematic paste | Clipboard (${clipboard.height} | ${clipboard.width}) ")

        WorldEdit.getInstance().newEditSessionBuilder()
            .world(BukkitAdapter.adapt(location.world))
            .build().use {
                sendDebug("Schematic paste | EditSession")

                val blockV = BlockVector3.at(location.x.toInt(), location.y.toInt(), location.z.toInt())

                sendDebug("Schematic paste | Block Vector 3 (${blockV.toVector3()})")

                val operation = ClipboardHolder(clipboard)
                    .createPaste(it)
                    .to(blockV).build()
                sendDebug("Schematic paste | Operation")

                try {
                    Operations.complete(operation)
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }

                sendDebug("Schematic paste | Operation finished")
            }

    }

}