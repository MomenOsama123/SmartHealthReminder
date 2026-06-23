package com.example.smarthealthreminder.features.chatbot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R

class ChatAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val VIEW_TYPE_SHIMMER = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isLoading -> VIEW_TYPE_SHIMMER
            message.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layout = when (viewType) {
            VIEW_TYPE_USER -> R.layout.item_message_user
            VIEW_TYPE_BOT -> R.layout.item_message_bot
            VIEW_TYPE_SHIMMER -> R.layout.item_message_bot_shimmer
            else -> R.layout.item_message_bot
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
        
        // Add smooth entrance animation
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    override fun getItemCount(): Int = messages.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView? = itemView.findViewById(R.id.textMessage)

        fun bind(message: Message) {
            textMessage?.text = message.text
        }
    }
}