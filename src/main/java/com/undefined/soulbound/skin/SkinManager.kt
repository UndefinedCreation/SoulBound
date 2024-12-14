package com.undefined.soulbound.skin

import com.mojang.authlib.properties.Property
import com.undefined.api.scheduler.delay
import com.undefined.soulbound.SoulBound
import com.undefined.soulbound.skin.SkinManager.connection
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.server.network.ServerGamePacketListenerImpl
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
import javax.imageio.ImageIO

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

                done.invoke(Pair(skinInfo.texture().data.value, skinInfo.texture().data.signature))
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

    fun Player.setSkin(pair: Pair<String, String>) {

        val craftPlayer = this as CraftPlayer
        val serverPlayer = craftPlayer.handle

        val profile = serverPlayer.gameProfile

        val pMap = profile.properties
        val property = pMap.get("textures").iterator().next()
        pMap.remove("textures", property)
        pMap.put("textures", Property("textures", pair.first, pair.second))

        Bukkit.getOnlinePlayers().forEach {
            if (it == this) {
                this.connection().send(ClientboundPlayerInfoRemovePacket(listOf(this.uniqueId)))
            } else {
                it.hidePlayer(this@setSkin)
            }
        }

        delay(1) {
            Bukkit.getOnlinePlayers().forEach {
                if (it == this) {
                    this.connection().send(ClientboundPlayerInfoUpdatePacket(
                        ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        (player as CraftPlayer).handle
                    ))
                } else {
                    it.showPlayer(this@setSkin)
                }
            }
        }
    }

    private fun Player.connection(): ServerGamePacketListenerImpl = (this as CraftPlayer).handle.connection

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

        // Create a new grayscale image with the same dimensions
        val grayscaleImage = BufferedImage(
            originalImage.width,
            originalImage.height,
            BufferedImage.TYPE_BYTE_GRAY
        )

        // Draw the original image into the grayscale image
        val graphics = grayscaleImage.createGraphics()
        graphics.drawImage(originalImage, 0, 0, null)
        graphics.dispose()

        // Write the grayscale image to the output file
        ImageIO.write(grayscaleImage, "png", outputFile)
    }

}