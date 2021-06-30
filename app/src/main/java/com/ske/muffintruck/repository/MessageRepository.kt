package com.ske.muffintruck.data.repository

import com.ske.muffintruck.data.db.MessageDao
import com.ske.muffintruck.data.model.Message

class MessageRepository(private val dao: MessageDao) {

    fun get() = dao.get()

    suspend fun insert(vararg messages: Message) = dao.insert(*messages)

    suspend fun delete(vararg messages: Message) = dao.delete(*messages)

}