package com.example.androidtoarduinoor

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderText: TextView = itemView.findViewById(R.id.text_sender)
        val contentText: TextView = itemView.findViewById(R.id.text_content)
        val container: LinearLayout = itemView as LinearLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.senderText.text = message.sender
        holder.contentText.text = message.content

        // 设置左右对齐 // Set left or right alignment
        val layoutParams = holder.contentText.layoutParams as LinearLayout.LayoutParams
        if (message.sender == "user") {
            // 用户消息右对齐 // User message aligns to the right
            layoutParams.gravity = Gravity.END
            holder.container.gravity = Gravity.END
            layoutParams.marginStart = 50
            layoutParams.marginEnd = 0
        } else {
            // 设备消息左对齐 // Device message aligns to the left
            layoutParams.gravity = Gravity.START
            holder.container.gravity = Gravity.START
            layoutParams.marginStart = 0
            layoutParams.marginEnd = 50
        }
        holder.contentText.layoutParams = layoutParams
    }

    override fun getItemCount(): Int = messages.size
}
