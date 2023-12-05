package net.idrnd.idvoicegpt.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.idrnd.idvoicegpt.R
import net.idrnd.idvoicegpt.ui.ChatMessages

/**
 * An adapter to support a list of chat messages.
 */
class ChatAdapter : ListAdapter<ChatMessages, ChatAdapter.ChatViewHolder>(ChatDiffCallback) {

    class ChatViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val chatUserText: TextView = itemView.findViewById(R.id.chat_user_text)
        private val chatGPTText: TextView = itemView.findViewById(R.id.chat_gpt_text)
        private var chatMessages: ChatMessages? = null

        fun bind(chatMessages: ChatMessages) {
            this.chatMessages = chatMessages
            chatUserText.text = chatMessages.question
            chatGPTText.text = chatMessages.response
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.chat_messages_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatMessages = getItem(position)
        holder.bind(chatMessages)
    }
}

object ChatDiffCallback : DiffUtil.ItemCallback<ChatMessages>() {
    override fun areItemsTheSame(oldItem: ChatMessages, newItem: ChatMessages): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ChatMessages, newItem: ChatMessages): Boolean {
        return oldItem.question == newItem.question && oldItem.response == newItem.response
    }
}
