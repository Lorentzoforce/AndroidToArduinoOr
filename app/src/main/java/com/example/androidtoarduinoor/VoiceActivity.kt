package com.example.androidtoarduinoor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VoiceActivity : AppCompatActivity() {
    private lateinit var textStatus: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonClose: Button
    private lateinit var buttonMinimize: Button
    private lateinit var editRecognized: EditText
    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var deviceName: String
    private val REQUEST_RECORD_AUDIO = 1

    // 请求音频权限
    // Request audio recording permission
    private fun ensureAudioPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureAudioPermission()
        setContentView(R.layout.activity_voice)

        // 初始化 UI
        // Initialize UI
        textStatus = findViewById(R.id.text_voice_status)
        recyclerView = findViewById(R.id.recycler_chat)
        buttonClose = findViewById(R.id.button_close)
        buttonMinimize = findViewById(R.id.button_minimize)

        // 动态添加识别结果框
        // Dynamically add recognized text box
        editRecognized = EditText(this).apply {
            hint = "Listening..."
            isEnabled = false
            setBackgroundColor(0xFFF5F5F5.toInt())
            setPadding(24, 16, 24, 16)
            textSize = 16f
        }
        val root = findViewById<LinearLayout>(R.id.activity_voice_root)
        root.addView(editRecognized, root.childCount - 1)

        deviceName = intent.getStringExtra("device_name") ?: "Device"
        // 设置状态文本
        // Set status text
        textStatus.text = "Your voice is being converted to text and sent to device: $deviceName"

        // 初始化 RecyclerView
        // Initialize RecyclerView
        chatAdapter = ChatAdapter(chatList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        // 注册到全局语音管理器
        // Register to global voice session manager
        VoiceSessionManager.registerDevice(deviceName, this) { recognizedText ->
            runOnUiThread {
                editRecognized.setText(recognizedText)
                addMessage("You (Voice)", recognizedText)
            }
        }

        // 注册设备消息接收
        // Register device message receiving
        ChatSocketBridge.registerChat(deviceName, object : ChatHandler {
            override fun onMessageReceived(message: String) {
                runOnUiThread { addMessage("Device", message) }
            }
        })

        // 按钮事件
        // Button click events
        buttonClose.setOnClickListener {
            VoiceSessionManager.unregisterDevice(deviceName)
            finish()
        }

        buttonMinimize.setOnClickListener {
            VoiceSessionManager.minimize(deviceName)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ChatSocketBridge.unregisterChat(deviceName)
        // 注意：不要在这里调用 unregisterDevice，留给按钮控制
        // Note: Do not call unregisterDevice here, leave it for button control
    }

    private fun addMessage(sender: String, content: String) {
        chatList.add(ChatMessage(sender, content))
        chatAdapter.notifyItemInserted(chatList.size - 1)
        recyclerView.scrollToPosition(chatList.size - 1)
    }

    inner class ChatAdapter(private val list: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
        inner class ChatViewHolder(v: android.view.View) : RecyclerView.ViewHolder(v) {
            val sender: TextView = v.findViewById(R.id.text_sender)
            val content: TextView = v.findViewById(R.id.text_content)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ChatViewHolder {
            val v = layoutInflater.inflate(R.layout.item_chat_message, parent, false)
            return ChatViewHolder(v)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val msg = list[position]
            holder.sender.text = msg.sender
            holder.content.text = msg.content
        }

        override fun getItemCount(): Int = list.size
    }
}
