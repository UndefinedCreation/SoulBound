package com.undefined.soulbound.skin

import com.destroystokyo.paper.profile.ProfileProperty
import com.google.gson.JsonParser
import com.mojang.authlib.properties.Property
import com.undefined.api.extension.hidePlayer
import com.undefined.api.extension.showPlayer
import com.undefined.api.scheduler.delay
import com.undefined.api.scheduler.sync
import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.util.updateScoreboardStuff
import net.minecraft.network.protocol.game.*
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.mineskin.JsoupRequestHandler
import org.mineskin.MineSkinClient
import org.mineskin.data.Visibility
import org.mineskin.exception.MineSkinRequestException
import org.mineskin.request.GenerateRequest
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


object SkinManager {

    private val CLIENT = MineSkinClient.builder()
        .requestHandler(::JsoupRequestHandler)
        .userAgent("Souldbound/v1.0") // TODO: update this with your own user agent
        .apiKey("13680d5f5def562e8b90e42ff1b7076ae5560c39d143b01bbcbe0c814ff77141") // TODO: update this with your own API key (https://account.mineskin.org/keys)
        .build()

    private val outputFile: File = File(SoulBound.INSTANCE.dataFolder, "skins").apply { mkdir() }


    fun getNormalSkin(name: String, done: (Pair<String, String>) -> (Unit)) {
        val outFile = File(outputFile, "$name.png")
        if (!outFile.exists()) {
            downloadPng("https://mineskin.eu/download/$name", outFile)
        }
        getInfo(outFile, done)
    }

    fun getGraySkin(name: String, done: (Pair<String, String>) -> (Unit)) {

        val outFile = File(outputFile, "$name.png")
        val grayOut = File(outputFile, "${name}_gray.png")

        if (!grayOut.exists()) {
            downloadPng("https://mineskin.eu/download/$name", outFile)
            convertToGrayscale(outFile, grayOut)
        }

        getInfo(grayOut, done)
    }

    private fun getInfo(file: File, done: (Pair<String, String>) -> (Unit)) {

        val request = GenerateRequest.upload(file)
            .name("SOULBOUND")
            .visibility(Visibility.PUBLIC)

        CLIENT.queue().submit(request)
            .thenCompose { queueResponse ->
                return@thenCompose queueResponse.job.waitForCompletion(CLIENT)
            }
            .thenCompose { jobResponse -> return@thenCompose jobResponse.getOrLoadSkin(CLIENT) }
            .thenAccept { skinInfo ->
                sync { done.invoke(Pair(skinInfo.texture().data.value, skinInfo.texture().data.signature)) }
            }
            .exceptionally { throwable ->
                throwable.printStackTrace()
                if (throwable is MineSkinRequestException) {

                    val response = throwable.response
                    val details = response.errorOrMessage
                    details.ifPresent {
                        println(it.code + ": " + it.message)
                    }

                }
                return@exceptionally null
            }

    }

    @OptIn(ExperimentalEncodingApi::class)
    fun extractSkinUrl(textures: String): URL {
        val newTextures = String(Base64.decode(textures), StandardCharsets.UTF_8)
        val url: String = JsonParser().parse(newTextures).asJsonObject
            .getAsJsonObject("textures")
            .getAsJsonObject("SKIN")
            .get("url")
            .asString
        return URL(url)
    }

    fun Player.setSkins(pair: Pair<String, String>) {
        val pp = playerProfile
        pp.setProperty(ProfileProperty("textures", pair.first, pair.second))
        playerProfile = pp
    }

    @Deprecated("PACKETS :(")
    fun Player.setSkin(pair: Pair<String, String>) {
        val craftPlayer = this as CraftPlayer
        val serverPlayer = craftPlayer.handle

        serverPlayer.connection.sendPacket(ClientboundPlayerInfoRemovePacket(listOf(serverPlayer.uuid)))

        val gameProfile = serverPlayer.gameProfile
        val properties = gameProfile.properties
        val property = properties.get("textures").iterator().next()
        properties.remove("textures", property)
        properties.put("textures", Property("textures", pair.first, pair.second))

        delay(1) {
            serverPlayer.connection.sendPacket(ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, serverPlayer))

            serverPlayer.connection.sendPacket(
                ClientboundRespawnPacket(
                    CommonPlayerSpawnInfo(
                        serverPlayer.serverLevel().dimensionTypeRegistration(),
                        serverPlayer.serverLevel().dimension(),
                        0,
                        serverPlayer.gameMode.gameModeForPlayer,
                        null,
                        false,
                        false,
                        serverPlayer.lastDeathLocation,
                        0,
                        0
                    ),
                    3
                )
            )

            serverPlayer.connection.sendPacket(ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, serverPlayer))
            serverPlayer.connection.sendPacket(
                ClientboundGameEventPacket(
                    ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START,
                    0F
                )
            )
            this@setSkin.health = 20.0
            this@setSkin.totalExperience = this@setSkin.totalExperience
            this@setSkin.teleport(this@setSkin.location.clone())
            this@setSkin.foodLevel = this@setSkin.foodLevel
            this@setSkin.updateInventory()
            this@setSkin.updateScoreboardStuff()
            serverPlayer.onUpdateAbilities()


        }

        Bukkit.getOnlinePlayers().forEach {
            it.hidePlayer()
        }

        delay(1) {
            Bukkit.getOnlinePlayers().forEach {
                it.showPlayer()
            }
        }
    }



    private fun downloadPng(url: String, outputFile: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                println("File downloaded to: ${outputFile.absolutePath}")
            } else {
                println("Server returned HTTP response code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            println("Error downloading file: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    private fun convertToGrayscale(inputFile: File, outputFile: File) {
        // Read the input image
        val originalImage: BufferedImage = ImageIO.read(inputFile)

        // Create a new image with the same dimensions and ARGB type to preserve transparency
        val grayscaleImage = BufferedImage(
            originalImage.width,
            originalImage.height,
            BufferedImage.TYPE_INT_ARGB
        )

        // Loop through each pixel to apply grayscale conversion
        for (y in 0 until originalImage.height) {
            for (x in 0 until originalImage.width) {
                val argb = originalImage.getRGB(x, y)

                // Extract alpha, red, green, and blue components
                val alpha = argb ushr 24 and 0xFF
                val red = argb ushr 16 and 0xFF
                val green = argb ushr 8 and 0xFF
                val blue = argb and 0xFF

                // Calculate the grayscale value
                val gray = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()

                // Combine the grayscale value with the original alpha channel
                val grayscaleArgb = (alpha shl 24) or (gray shl 16) or (gray shl 8) or gray

                // Set the new pixel value
                grayscaleImage.setRGB(x, y, grayscaleArgb)
            }
        }

        // Write the grayscale image to the output file
        ImageIO.write(grayscaleImage, "png", outputFile)
    }

}