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
package com.ske.muffintruck.data.services

import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ske.muffintruck.data.model.Message
import com.ske.muffintruck.data.model.Payload
import com.ske.muffintruck.data.repository.MessageRepository
import com.ske.muffintruck.utils.Notifications
import com.ske.muffintruck.utils.asMessage
import com.ske.muffintruck.utils.copyToClipboard
import com.ske.muffintruck.utils.uiHandler
import com.ske.muffintruck.viewmodel.PresenceLiveData
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.lang.Boolean.parseBoolean

class FcmService : FirebaseMessagingService() {

    private val repository: MessageRepository by inject()

    override fun onNewToken(token: String) {
        PresenceLiveData.instance(application).fetchToken()
    }

    @WorkerThread
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val message = remoteMessage.asMessage()
        val notification = remoteMessage.notification
        Log.i("tkstone", "Fcm notification - title : ${notification?.title}, body : ${notification?.body}, image : ${notification?.imageUrl}")
        Log.i("tkstone", "Fcm message : ${message.toString()}")
        runBlocking { repository.insert(message) }
        uiHandler.post { notifyAndExecute(message) }
    }

    @UiThread
    private fun notifyAndExecute(message: Message) {
        if (!parseBoolean(message.data["hide"])) {
            Notifications.show(this, message)
        }
        val payload = message.payload
        when {
            payload is Payload.Text && payload.clipboard -> applicationContext.copyToClipboard(payload.text)
//            payload is Payload.Text && payload.clipboard -> applicationContext.copyToClipboard("Test data")
        }
    }
}