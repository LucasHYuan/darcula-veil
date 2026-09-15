package com.lucashyuan.darculaveil

import com.intellij.util.messages.Topic

interface VeilStateListener {

    fun veilStateChanged()

    companion object {
        @JvmField val TOPIC: Topic<VeilStateListener> = Topic.create("Darcula Veil state", VeilStateListener::class.java)
    }
}
