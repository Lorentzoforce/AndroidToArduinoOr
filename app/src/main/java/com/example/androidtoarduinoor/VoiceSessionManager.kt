package com.example.androidtoarduinoor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * VoiceSessionManager — Manages global voice recognition sessions.
 * Supports multiple devices, minimization, and broadcasting recognized text.
 */
object VoiceSessionManager {
    private val activeDevices = mutableSetOf<String>()           // 正在使用语音的设备 / Devices currently using voice
    private val minimizedDevices = mutableSetOf<String>()        // 最小化的设备 / Minimized devices
    private var globalRecognizer: VoiceRecognizer? = null
    private var listeningJob: Job? = null
    private var currentCallback: ((String) -> Unit)? = null     // 当前 VoiceActivity 回调 / Current VoiceActivity callback

    var isListening = false
        private set

    private val TAG = "VoiceSessionManager"

    // ================================
    // Compatible API for MainActivity
    // ================================

    /** 获取当前正在使用语音的设备（MainActivity 使用） / Get currently active voice devices */
    fun getActiveDevices(): List<String> = activeDevices.toList()

    /** 停止指定设备的语音会话（MainActivity 使用） / Stop voice session for a device */
    fun stopSession(deviceName: String) {
        activeDevices.remove(deviceName)
        minimizedDevices.remove(deviceName)
        if (activeDevices.isEmpty()) {
            stopGlobalListening()
        }
        Log.i(TAG, "Session stopped: $deviceName (remaining: ${activeDevices.size})")
    }

    // ================================
    // API for VoiceActivity
    // ================================

    /** 注册设备进入语音识别（VoiceActivity 调用） / Register device for voice recognition */
    fun registerDevice(deviceName: String, context: Context, onResult: (String) -> Unit) {
        activeDevices.add(deviceName)
        currentCallback = onResult
        startGlobalListening(context)
        Log.i(TAG, "Device registered: $deviceName (total: ${activeDevices.size})")
    }

    /** 注销设备（VoiceActivity 关闭时调用） / Unregister device (called when VoiceActivity closes) */
    fun unregisterDevice(deviceName: String) {
        activeDevices.remove(deviceName)
        minimizedDevices.remove(deviceName)
        if (activeDevices.isEmpty()) {
            stopGlobalListening()
        }
        Log.i(TAG, "Device unregistered: $deviceName (remaining: ${activeDevices.size})")
    }

    /** 最小化设备 / Minimize device */
    fun minimize(deviceName: String) {
        minimizedDevices.add(deviceName)
    }

    /** 获取 Calling Devices（兼容旧代码） / Get calling devices (legacy support) */
    fun getCallingDevices(): List<String> = getActiveDevices()

    // ================================
    // Internal core logic
    // ================================

    private fun startGlobalListening(context: Context) {
        if (isListening) return
        isListening = true
        globalRecognizer = VoiceRecognizer(context)
        listeningJob = CoroutineScope(Dispatchers.Main).launch {
            globalRecognizer?.startListening { text ->
                if (text.isNotBlank()) {
                    currentCallback?.invoke(text)  // 通知当前 VoiceActivity / Notify current VoiceActivity
                    broadcastToAllDevices(text)    // 发送给所有设备 / Broadcast to all devices
                }
            }
        }
        Log.i(TAG, "Global voice recognition started")
    }

    private fun stopGlobalListening() {
        isListening = false
        currentCallback = null
        listeningJob?.cancel()
        globalRecognizer?.stopListening()
        globalRecognizer = null
        listeningJob = null
        Log.i(TAG, "Global voice recognition stopped")
    }

    /** 广播文本到所有活跃设备 / Broadcast text to all active devices */
    private fun broadcastToAllDevices(text: String) {
        activeDevices.forEach { device ->
            ChatSocketBridge.sendToDevice(device, text)
            Log.i(TAG, "Broadcast voice text → $device: $text")
        }
    }
}
