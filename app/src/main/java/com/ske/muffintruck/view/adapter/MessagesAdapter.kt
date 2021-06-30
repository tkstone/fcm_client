/*
 * Copyright 2017 Simon Marquis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ske.muffintruck.view.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.moshi.Moshi
import com.ske.muffintruck.R
import com.ske.muffintruck.data.model.Message
import com.ske.muffintruck.data.model.Payload
import com.ske.muffintruck.utils.copyToClipboard
import com.ske.muffintruck.utils.safeStartActivity
import com.ske.muffintruck.view.adapter.MessagesAdapter.Action.PRIMARY
import com.ske.muffintruck.view.adapter.MessagesAdapter.Action.SECONDARY
import com.ske.muffintruck.view.ui.TimeAgoTextView
import kotlin.math.min

class MessagesAdapter(private val moshi: Moshi) : ListAdapter<Message, MessagesAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.messageId == newItem.messageId
            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
        }
        private val selection: androidx.collection.ArrayMap<String, Boolean> = androidx.collection.ArrayMap()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_payload, parent, false)
        return ViewHolder(view, ::toggle)
    }

    private fun toggle(message: Message, viewHolder: ViewHolder) {
        selection[message.messageId] = !(selection[message.messageId] ?: false)
        notifyItemChanged(viewHolder.adapterPosition)
    }

    public override fun getItem(position: Int): Message = super.getItem(position)

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) = viewHolder.onBind(getItem(position), selection[getItem(position).messageId] ?: false)

    override fun onViewRecycled(holder: ViewHolder) = holder.onUnbind()

    enum class Action {
        PRIMARY, SECONDARY
    }

    inner class ViewHolder(itemView: View, private val listener: (Message, ViewHolder) -> Unit) : RecyclerView.ViewHolder(itemView) {

        private var message: Message? = null
        private var selected = false

        private val icon: ImageView = itemView.findViewById(R.id.item_icon)
        private val timestamp: TimeAgoTextView = itemView.findViewById(R.id.item_timestamp)
        private val raw: TextView = itemView.findViewById(R.id.item_raw)
        private val text: TextView = itemView.findViewById(R.id.item_text)
        private val button1: Button = itemView.findViewById(R.id.item_btn_1)
        private val button2: Button = itemView.findViewById(R.id.item_btn_2)
        private val selector: View = itemView.findViewById(R.id.item_selector)

        init {
            button1.setOnClickListener { execute(PRIMARY, payload()) }
            button2.setOnClickListener { execute(SECONDARY, payload()) }
            itemView.setOnClickListener { message?.let { listener(it, this) } }
            itemView.setOnLongClickListener { message?.let { listener(it, this) }.let { true } }
        }

        private fun payload(): Payload? = message?.payload

        private fun execute(action: Action, payload: Payload?) {
            val context = itemView.context
            when (payload) {
                is Payload.App -> {
                    when (action) {
                        PRIMARY -> context.safeStartActivity(payload.playStore())
                        SECONDARY -> context.safeStartActivity(payload.uninstall())
                    }
                }
                is Payload.Link -> {
                    if (action == PRIMARY) {
                        context.safeStartActivity(payload.intent())
                    }
                }
                is Payload.Text -> {
                    if (action == PRIMARY) {
                        context.copyToClipboard(payload.text)
                    }
                }
            }
        }

        fun onBind(message: Message, selected: Boolean) {
            this.message = message
            this.selected = selected
            icon.setImageResource(message.payload?.icon() ?: 0)
            timestamp.timestamp = min(message.sentTime, System.currentTimeMillis())
            renderContent()
            renderButtons()
        }

        private fun renderContent() {
            selector.isActivated = selected
            if (selected) {
                text.text = null
                text.visibility = GONE
                val data: Map<*, *>? = message?.data
                val display = if (data != null) moshi.adapter(Message::class.java).indent("  ").toJson(message) else null
                raw.text = display
                raw.visibility = if (TextUtils.isEmpty(display)) GONE else VISIBLE
            } else {
                val payload = payload()
                val display = payload?.display()
                text.text = display
                text.visibility = if (TextUtils.isEmpty(display)) GONE else VISIBLE
                raw.text = null
                raw.visibility = GONE
            }
        }

        private fun renderButtons() {
            mapOf(
                    PRIMARY to button1,
                    SECONDARY to button2
            ).forEach { (action, button) ->
                render(action, button, message?.payload)
            }
        }

        private fun render(action: Action, button: Button, payload: Payload?) {
            if (selected) {
                button.visibility = GONE
                return
            }
            when (payload) {
                is Payload.App -> {
                    when (action) {
                        PRIMARY -> {
                            button.visibility = VISIBLE
                            button.setText(R.string.payload_app_store)
                            return
                        }
                        SECONDARY -> {
                            button.setText(R.string.payload_app_uninstall)
                            button.visibility = if (payload.isInstalled(itemView.context)) VISIBLE else GONE
                            return
                        }
                    }
                }
                is Payload.Link -> {
                    if (action == PRIMARY) {
                        button.visibility = VISIBLE
                        button.setText(R.string.payload_link_open)
                        return
                    }
                }
                is Payload.Text -> {
                    if (action == PRIMARY) {
                        button.visibility = VISIBLE
                        button.setText(R.string.payload_text_copy)
                        return
                    }
                }
            }
            button.visibility = GONE
            button.text = null
        }

        fun onUnbind() {
            message = null
            timestamp.timestamp = TimeAgoTextView.NO_TIMESTAMP
        }
    }

}