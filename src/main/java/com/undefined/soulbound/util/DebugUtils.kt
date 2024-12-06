package com.undefined.soulbound.util

var debugMode = true

fun sendDebug(string: String) {
    if (debugMode) com.undefined.api.sendLog(string)
}