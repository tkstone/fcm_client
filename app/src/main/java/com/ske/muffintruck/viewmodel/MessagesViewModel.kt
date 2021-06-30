package com.ske.muffintruck.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ske.muffintruck.data.model.Message
import com.ske.muffintruck.data.repository.MessageRepository
import kotlinx.coroutines.launch

class MessagesViewModel(application: Application, private val repository: MessageRepository) : AndroidViewModel(application) {

    val presence = PresenceLiveData.instance(application)

    val messages = repository.get()

    fun insert(vararg messages: Message) = viewModelScope.launch { repository.insert(*messages) }

    fun delete(vararg messages: Message) = viewModelScope.launch { repository.delete(*messages) }

}