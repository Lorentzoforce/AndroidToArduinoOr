package com.example.androidtoarduinoor

// 数据类，用于表示一条聊天消息
// Data class representing a chat message
data class ChatMessage(
    val sender: String,   // 谁发的消息 // Who sent the message
    val content: String   // 消息内容 // Message content
)
