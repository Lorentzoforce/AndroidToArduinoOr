package com.example.androidtoarduinoor

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

// ✅ 实现 ChatHandler 接口，让它能被 ChatSocketBridge 回调
// ✅ Implement ChatHandler interface so it can be called by ChatSocketBridge
class ChatActivity : AppCompatActivity(), ChatHandler {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var buttonClear: Button
    private lateinit var buttonCall: Button
    private lateinit var textDeviceName: TextView
    private lateinit var textConnectionStatus: TextView
    private lateinit var container: FrameLayout

    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var deviceName: String

    // TCP Socket
    // TCP socket for communication
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private val isConnected = AtomicBoolean(false)
    private var receiveJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        container = findViewById(R.id.main_container)
        textDeviceName = findViewById(R.id.text_device_name)
        recyclerView = findViewById(R.id.recyclerView_chat)
        editMessage = findViewById(R.id.edit_message)
        buttonSend = findViewById(R.id.button_send)
        buttonClear = findViewById(R.id.button_clear)
        buttonCall = findViewById(R.id.button_call)

        // ✅ 连接状态文本（动态添加）
        // ✅ Connection status text (added dynamically)
        textConnectionStatus = TextView(this).apply {
            text = "Connection status: Not connected" // originally: 连接状态：未连接
            textSize = 14f
        }
        val rootLayout = findViewById<LinearLayout>(R.id.chat_root)
        rootLayout.addView(textConnectionStatus, 1) // 插在设备名下方 // Insert under device name

        deviceName = intent.getStringExtra("device_name") ?: "Device" // 设备 -> Device
        textDeviceName.text = deviceName

        chatAdapter = ChatAdapter(chatList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        // 点击发送
        // Click Send
        buttonSend.setOnClickListener {
            val message = editMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessage("You", message) // 你 -> You
                editMessage.text.clear()
                sendMessage(message)
            }
        }

        // 点击清空
        // Click Clear
        buttonClear.setOnClickListener {
            hideKeyboard()
            chatList.clear()
            chatAdapter.notifyDataSetChanged()
        }

        // 返回按钮
        // Back button
        findViewById<Button>(R.id.button_back).setOnClickListener { finish() }

        // + 按钮弹出菜单
        // + button shows menu
        buttonCall.setOnClickListener {
            hideKeyboard()
            showBottomMenu()
        }

        // 滚动消息列表时隐藏键盘
        // Hide keyboard when scrolling chat list
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) hideKeyboard()
            false
        }

        // 连接设备
        // Connect to device
        connectToDevice()

        // ✅ 注册到 ChatSocketBridge
        // ✅ Register to ChatSocketBridge
        ChatSocketBridge.registerChat(deviceName, this) { message ->
            sendMessage(message)  // 真实发送 // actually send
        }
    }

    // ✅ 实现 ChatHandler 接口
    // ✅ Implement ChatHandler interface
    override fun onMessageReceived(message: String) {
        runOnUiThread {
            addMessage("Device", message) // 设备 -> Device
        }
    }

    //横幅显示程序在使用语音识别
    // Banner shows when device is using voice recognition
    private lateinit var bannerVoiceSession: TextView

    override fun onResume() {
        super.onResume()
        updateVoiceSessionBanner()
    }

    private fun updateVoiceSessionBanner() {
        val callingDevices = VoiceSessionManager.getCallingDevices()
        val rootLayout = findViewById<LinearLayout>(R.id.chat_root)

        // 如果已有横幅则先移除
        // If banner exists, remove it first
        if (::bannerVoiceSession.isInitialized) rootLayout.removeView(bannerVoiceSession)

        if (callingDevices.isNotEmpty()) {
            val current = callingDevices.joinToString(", ")
            bannerVoiceSession = TextView(this).apply {
                text = "🎙 This device is using voice-to-text: $current (click to return)" // 中文替换成英文
                textSize = 15f
                setPadding(24, 16, 24, 16)
                setBackgroundColor(0xFFBBDEFB.toInt()) // 淡蓝背景 // Light blue background
                setTextColor(0xFF000000.toInt())
                setOnClickListener {
                    // 默认进入第一个设备的语音界面
                    // By default, enter the first device's voice interface
                    val intent = Intent(this@ChatActivity, VoiceActivity::class.java)
                    intent.putExtra("device_name", callingDevices.first())
                    startActivity(intent)
                }
            }
            rootLayout.addView(bannerVoiceSession, 0)
        }
    }

    /** =======================  UI逻辑  ======================= **/
    // UI logic
    private fun addMessage(sender: String, content: String) {
        chatList.add(ChatMessage(sender, content))
        chatAdapter.notifyItemInserted(chatList.size - 1)
        recyclerView.scrollToPosition(chatList.size - 1)
    }

    private fun updateConnectionStatus(connected: Boolean) {
        runOnUiThread {
            textConnectionStatus.text =
                if (connected) "Connection status: ✅ Connected" // 已连接 -> Connected
                else "Connection status: ❌ Not connected" // 未连接 -> Not connected
        }
    }

    private fun showBottomMenu() {
        val dialog = BottomSheetDialog(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 32, 32, 32)
        }

        fun createMenuButton(title: String): Button {
            return Button(this).apply {
                text = title
                textSize = 16f
                isAllCaps = false
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f   // ✅ 平均分配宽度 // evenly distribute width
                ).apply {
                    marginStart = 8
                    marginEnd = 8
                }
            }
        }

        val callBtn = createMenuButton("CALL")
        val carBtn = createMenuButton("CAR")
        val customBtn = createMenuButton("CUSTOM")
        val settingBtn = createMenuButton("SETTING")

        layout.addView(callBtn)
        layout.addView(carBtn)
        layout.addView(customBtn)
        layout.addView(settingBtn)
        dialog.setContentView(layout)

        callBtn.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, VoiceActivity::class.java)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
        }

        carBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "CAR feature not available yet", Toast.LENGTH_SHORT).show() // 中文 -> English
        }

        customBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "CUSTOM feature not available yet", Toast.LENGTH_SHORT).show()
        }

        settingBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "SETTING feature not available yet", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }

    /** =======================  网络逻辑  ======================= **/
    // Network logic
    private fun getCurrentSSID(): String? {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        var ssid = info.ssid
        if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length - 1)
        }
        return ssid
    }

    private fun connectToDevice() {
        when {
            deviceName.contains("(WIFI HOST)") -> {
                connectToArduino()
            }
            deviceName.contains("(PHONE CLIENT)") -> {
                Toast.makeText(this, "PHONE CLIENT connection logic not implemented", Toast.LENGTH_SHORT).show()
            }
            deviceName.contains("(SERIAL)") -> {
                Toast.makeText(this, "SERIAL connection logic not implemented", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Unknown device type: $deviceName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToArduino() {
        val ssid = getCurrentSSID() ?: return
        if (!ssid.contains("ArduinoAP")) return

        val ip = "192.168.4.1"
        val port = 23

        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = Socket()
                socket?.connect(InetSocketAddress(ip, port), 5000)
                writer = PrintWriter(socket?.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                isConnected.set(true)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Connected to Arduino", Toast.LENGTH_SHORT).show()
                    updateConnectionStatus(true)
                }
                startReceiving()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Failed to connect to Arduino", Toast.LENGTH_SHORT).show()
                    updateConnectionStatus(false)
                }
            }
        }
    }

    private fun startReceiving() {
        receiveJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isConnected.get()) {
                    val line = reader?.readLine() ?: break
                    if (line.isNotEmpty()) {
                        withContext(Dispatchers.Main) { addMessage("Device", line) } // 设备 -> Device
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateConnectionStatus(false)
            }
        }
    }

    fun sendMessage(message: String) {
        if (deviceName.contains("(WIFI HOST)")) {
            if (!isConnected.get()) {
                Toast.makeText(this, "Device not connected", Toast.LENGTH_SHORT).show() // 未连接设备 -> Device not connected
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    writer?.println(message)
                    writer?.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                    updateConnectionStatus(false)
                }
            }
        } else {
            addMessage("Device", "Received: $message (fake)") // 设备 -> Device
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        receiveJob?.cancel()
        try { socket?.close() } catch (_: Exception) {}
        isConnected.set(false)
        // ✅ 反注册
        // ✅ Unregister
        ChatSocketBridge.unregisterChat(deviceName)
    }

    /** =======================  Adapter ======================= **/
    inner class ChatAdapter(private val list: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

        inner class ChatViewHolder(val view: android.view.View) :
            RecyclerView.ViewHolder(view) {
            val senderText: TextView = view.findViewById(R.id.text_sender)
            val contentText: TextView = view.findViewById(R.id.text_content)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ChatViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val msg = list[position]
            holder.senderText.text = msg.sender
            holder.contentText.text = msg.content

            val params = holder.view.layoutParams as RecyclerView.LayoutParams
            if (msg.sender == "You") { // 你 -> You
                holder.view.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
                holder.senderText.textAlignment = android.view.View.TEXT_ALIGNMENT_VIEW_END
                holder.contentText.textAlignment = android.view.View.TEXT_ALIGNMENT_VIEW_END
            } else {
                holder.view.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
                holder.senderText.textAlignment = android.view.View.TEXT_ALIGNMENT_VIEW_START
                holder.contentText.textAlignment = android.view.View.TEXT_ALIGNMENT_VIEW_START
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
