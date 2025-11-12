package com.example.androidtoarduinoor

import android.content.Context

/**
 * ChatSocketBridge
 * Used to allow the voice recognition module to communicate with the currently active ChatActivity or VoiceActivity.
 * Enables message forwarding by registering device names with ChatHandler callbacks.
 */
interface ChatHandler {
    fun onMessageReceived(message: String)
}

object ChatSocketBridge {
    /*private val sockets = mutableMapOf<String, ChatHandler>()

    /**
     * 注册某个设备对应的 ChatHandler（可以是 ChatActivity 或 VoiceActivity）
     * Register a ChatHandler for a specific device (can be ChatActivity or VoiceActivity)
     */
    fun registerChat(deviceName: String, handler: ChatHandler) {
        sockets[deviceName] = handler
    }

    /**
     * 反注册设备，防止内存泄漏
     * Unregister a device to prevent memory leaks
     */
    fun unregisterChat(deviceName: String) {
        sockets.remove(deviceName)
    }

    /**
     * 向指定设备发送文本消息（在对应界面显示）
     * Send a text message to the specified device (displayed in the corresponding UI)
     */
    fun sendToDevice(deviceName: String, text: String) {
        sockets[deviceName]?.onMessageReceived(text)
    }*/

    private val handlers = mutableMapOf<String, ChatHandler>()
    private val senders = mutableMapOf<String, (String) -> Unit>()  // 真实发送函数 // Actual send function

    /**
     * 注册设备与其 ChatHandler，同时可选提供真实发送函数
     * Register a device with its ChatHandler, optionally providing the actual send function
     */
    fun registerChat(deviceName: String, handler: ChatHandler, sender: ((String) -> Unit)? = null) {
        handlers[deviceName] = handler
        sender?.let { senders[deviceName] = it }
    }

    /**
     * 注销设备
     * Unregister a device
     */
    fun unregisterChat(deviceName: String) {
        handlers.remove(deviceName)
        senders.remove(deviceName)
    }

    /**
     * 向指定设备发送消息
     * 1. 在 UI 显示
     * 2. 调用真实发送函数
     * Send message to specified device
     * 1. Display on UI
     * 2. Call the actual send function
     */
    fun sendToDevice(deviceName: String, text: String) {
        // 1. UI 显示 // Display on UI
        handlers[deviceName]?.onMessageReceived(text)
        // 2. 真实发送 // Actual sending
        senders[deviceName]?.invoke(text)
    }
}
