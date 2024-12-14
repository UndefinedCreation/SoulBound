package com.undefined.soulbound

import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent


class VoiceChatImpl : VoicechatPlugin {

    companion object {
        lateinit var API: VoicechatServerApi
    }

    override fun getPluginId(): String = "soulbound"

    override fun registerEvents(registration: EventRegistration) {
        registration.registerEvent(
            VoicechatServerStartedEvent::class.java
        ) {
            API = it.voicechat
        }
    }

}