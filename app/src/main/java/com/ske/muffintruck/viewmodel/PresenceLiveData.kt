package com.ske.muffintruck.viewmodel

import android.app.Application
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.ske.muffintruck.data.model.Presence
import com.ske.muffintruck.utils.Singleton
import com.ske.muffintruck.utils.uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale.ROOT

class PresenceLiveData(application: Application) : LiveData<Presence>(Presence()), ValueEventListener {

    private val instanceId = FirebaseInstanceId.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val presenceRef: DatabaseReference = database.getReference(".info/connected")
    private val connectionRef: DatabaseReference = database.getReference("devices/${uuid(application)}")

    init {
        connectionRef.onDisconnect().removeValue()
    }

    companion object : Singleton<PresenceLiveData, Application>(::PresenceLiveData)

    override fun getValue(): Presence = super.getValue() ?: Presence()

    fun fetchToken() = GlobalScope.launch(Dispatchers.Main) {
        val token = withContext(Dispatchers.IO) {
            try {
                instanceId.instanceId.await().token
            } catch (e: Exception) {
                e.message
            }
        }
        value = value.copy(token = token)
        token?.let {
            connectionRef.setValue(payload(it))
        }
    }

    fun resetToken() = GlobalScope.launch(Dispatchers.Main) {
        value = value.copy(token = null)
        connectionRef.removeValue()
        withContext(Dispatchers.IO) {
            try {
                instanceId.deleteInstanceId()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Wait for FirebaseMessagingService.onNewToken()
        }
    }

    override fun onActive() {
        fetchToken()
        presenceRef.addValueEventListener(this)
        DatabaseReference.goOnline()
    }

    override fun onInactive() {
        connectionRef.removeValue()
        DatabaseReference.goOffline()
        presenceRef.removeEventListener(this)
        value = value.copy(connected = false)
    }

    override fun onCancelled(error: DatabaseError) {
        value = value.copy(connected = false)
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        value = value.copy(connected = snapshot.getValue(Boolean::class.java) ?: false)
        fetchToken()
    }

    private fun payload(token: String) = mapOf(
            "name" to if (MODEL.toLowerCase(ROOT).startsWith(MANUFACTURER.toLowerCase(ROOT))) MODEL else MANUFACTURER.toUpperCase(ROOT) + " " + MODEL,
            "token" to token,
            "timestamp" to ServerValue.TIMESTAMP
    )

}