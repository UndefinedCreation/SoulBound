package com.undefined.soulbound.util

var debugMode = true

var timerCooldownShorter = false

fun sendDebug(string: String) {
    if (debugMode) com.undefined.api.sendLog(string)
}