package com.example.data.repository

import com.example.data.model.PhoneContact
import com.example.data.model.PushMessageItem
import kotlinx.coroutines.flow.StateFlow

/*
 * Platform services the shared UI talks to. Each app provides its own implementation:
 * Android uses the Firebase SDKs and the phone's contacts, Windows has its own versions.
 */

/** Address book: the phone's contacts on Android, none on Windows (yet). */
interface ContactsSource {
    val contacts: StateFlow<List<PhoneContact>>
    val isLoading: StateFlow<Boolean>
    fun hasContactsPermission(): Boolean
    suspend fun loadContacts()
    fun findContactName(rawNumberOrUri: String): String?
}

/** Push token (FCM on Android) and the list of received pushes shown in Settings. */
interface PushSource {
    val fcmToken: StateFlow<String?>
    val tokenStatus: StateFlow<String>
    val receivedPushes: StateFlow<List<PushMessageItem>>
    fun refreshToken()
    fun clearPushHistory()
    fun recordPushMessage(item: PushMessageItem)
}

/** Shows a system notification for a new chat message; opening it opens [conversationId]. */
fun interface ChatNotifier {
    fun notify(conversationId: String, title: String, text: String)
}

/** Small persistent key-value storage (SharedPreferences on Android, Preferences on Windows). */
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getLong(key: String, default: Long): Long
    fun getString(key: String, default: String?): String?
    fun putBoolean(key: String, value: Boolean)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
    fun putString(key: String, value: String?)
    fun remove(key: String)
}
