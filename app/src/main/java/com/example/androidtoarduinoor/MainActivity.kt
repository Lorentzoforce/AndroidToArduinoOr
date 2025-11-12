package com.example.androidtoarduinoor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var banner: TextView
    private lateinit var tvTitle: TextView
    private lateinit var buttonRefresh: Button
    private lateinit var buttonCloseAll: Button

    private var currentListType = "ARDUINO HOST"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        recyclerView = findViewById(R.id.recyclerView)
        banner = findViewById(R.id.text_banner)
        tvTitle = findViewById(R.id.tvTitle)
        buttonRefresh = findViewById(R.id.button_refresh)
        buttonCloseAll = findViewById(R.id.button_close_all)

        ensureLocationPermission()

        // 默认显示 Arduino Host
        // Default show Arduino Host
        showArduinoHostList()

        findViewById<Button>(R.id.button_arduino_host).setOnClickListener {
            currentListType = "ARDUINO HOST"
            showArduinoHostList()
        }

        findViewById<Button>(R.id.button_phone_host).setOnClickListener {
            currentListType = "PHONE HOST"
            showDeviceList(listOf("ESP32_2", "Arduino3"))
        }

        findViewById<Button>(R.id.button_calling_devices).setOnClickListener {
            currentListType = "CALLING DEVICES"
            val activeDevices = VoiceSessionManager.getActiveDevices()
            if (activeDevices.isEmpty()) {
                // "No devices are currently using voice-to-text auto-send"
                showDeviceList(listOf("No devices are currently using voice-to-text auto-send")) { }
            } else {
                showDeviceList(activeDevices) { deviceName ->
                    val intent = Intent(this, VoiceActivity::class.java)
                    intent.putExtra("device_name", deviceName)
                    startActivity(intent)
                }
            }
        }

        // 刷新按钮点击
        // Refresh button click
        buttonRefresh.setOnClickListener {
            when (currentListType) {
                "ARDUINO HOST" -> showArduinoHostList()
                "PHONE HOST" -> showDeviceList(listOf("ESP32_2", "Arduino3"))
                "CALLING DEVICES" -> {
                    val activeDevices = VoiceSessionManager.getActiveDevices()
                    if (activeDevices.isEmpty()) {
                        showDeviceList(listOf("No devices are currently using voice-to-text auto-send")) { }
                    } else {
                        showDeviceList(activeDevices) { deviceName ->
                            val intent = Intent(this, VoiceActivity::class.java)
                            intent.putExtra("device_name", deviceName)
                            startActivity(intent)
                        }
                    }
                }
            }
        }

        // 关闭所有语音功能按钮
        // Close all voice sessions button
        buttonCloseAll.setOnClickListener {
            val activeDevices = VoiceSessionManager.getActiveDevices()
            for (device in activeDevices) {
                VoiceSessionManager.stopSession(device)
            }
            updateBanner()
            showArduinoHostList()
        }

        updateBanner()
    }

    /** 动态获取当前 Wi-Fi SSID */
    // Get current Wi-Fi SSID dynamically
    private fun getCurrentWifiSSID(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo?.ssid

        return if (ssid != null && ssid != "<unknown ssid>") {
            ssid.replace("\"", "")
        } else {
            null
        }
    }

    /** 列出 Arduino Host 逻辑 */
    // Show Arduino Host list logic
    private fun showArduinoHostList() {
        val ssid = getCurrentWifiSSID(this)
        val devices = if (ssid != null) listOf("$ssid (WIFI HOST)") else emptyList()
        showDeviceList(devices)
    }

    private fun showDeviceList(devices: List<String>, onClick: ((String) -> Unit)? = null) {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DeviceAdapter(devices) { deviceName ->
            if (devices.size == 1 && devices[0] == "No devices are currently using voice-to-text auto-send") return@DeviceAdapter

            if (onClick != null) {
                onClick(deviceName)
            } else {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("device_name", deviceName)
                startActivity(intent)
            }
        }

        tvTitle.text = currentListType
        updateBanner()
        updateCloseAllVisibility()
    }

    private fun updateBanner() {
        val activeDevices = VoiceSessionManager.getActiveDevices()
        if (activeDevices.isNotEmpty()) {
            // "Some devices are using voice-to-text"
            banner.text = "Some devices are using voice-to-text"
            banner.visibility = TextView.VISIBLE
        } else {
            banner.visibility = TextView.GONE
        }
    }

    /** 当当前列表为 CALLING DEVICES 且有活跃设备时显示“关闭所有”按钮 */
    // Show "Close All" button when current list is CALLING DEVICES and there are active devices
    private fun updateCloseAllVisibility() {
        val activeDevices = VoiceSessionManager.getActiveDevices()
        buttonCloseAll.visibility =
            if (currentListType == "CALLING DEVICES" && activeDevices.isNotEmpty()) {
                Button.VISIBLE
            } else {
                Button.GONE
            }
    }

    private fun ensureLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }
}
