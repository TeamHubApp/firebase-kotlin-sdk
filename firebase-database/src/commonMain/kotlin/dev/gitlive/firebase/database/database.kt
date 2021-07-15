/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.database

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.database.ChildEvent.Type.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy

/** Returns the [FirebaseDatabase] instance of the default [FirebaseApp]. */
expect val Firebase.database: FirebaseDatabase

/** Returns the [FirebaseDatabase] instance for the specified [url]. */
expect fun Firebase.database(url: String): FirebaseDatabase

/** Returns the [FirebaseDatabase] instance of the given [FirebaseApp]. */
expect fun Firebase.database(app: FirebaseApp): FirebaseDatabase

/** Returns the [FirebaseDatabase] instance of the given [FirebaseApp] and [url]. */
expect fun Firebase.database(app: FirebaseApp, url: String): FirebaseDatabase

expect class FirebaseDatabase {
    fun reference(path: String): DatabaseReference
    fun setPersistenceEnabled(enabled: Boolean)
    fun setLoggingEnabled(enabled: Boolean)
    fun useEmulator(host: String, port: Int)
}

data class ChildEvent internal constructor(
    val snapshot: DataSnapshot,
    val type: Type,
    val previousChildName: String?
) {
    enum class Type {
        ADDED,
        CHANGED,
        MOVED,
        REMOVED
    }
}

expect open class Query {
    val valueEvents: Flow<DataSnapshot>
    fun childEvents(vararg types: ChildEvent.Type = arrayOf(ADDED, CHANGED, MOVED, REMOVED)): Flow<ChildEvent>
    fun orderByKey(): Query
    fun orderByValue(): Query
    fun orderByChild(path: String): Query
    fun startAt(value: String, key: String? = null): Query
    fun startAt(value: Double, key: String? = null): Query
    fun startAt(value: Boolean, key: String? = null): Query
    fun endAt(value: String, key: String? = null): Query
    fun endAt(value: Double, key: String? = null): Query
    fun endAt(value: Boolean, key: String? = null): Query
    fun limitToFirst(limit: Int): Query
    fun limitToLast(limit: Int): Query
    fun equalTo(value: String, key: String? = null): Query
    fun equalTo(value: Double, key: String? = null): Query
    fun equalTo(value: Boolean, key: String? = null): Query
}

expect class DatabaseReference : Query {
    val key: String?
    fun push(): DatabaseReference
    fun child(path: String): DatabaseReference
    fun onDisconnect(): OnDisconnect
    suspend inline fun <reified T> setValue(value: T?, encodeDefaults: Boolean = true)
    suspend fun <T> setValue(strategy: SerializationStrategy<T>, value: T, encodeDefaults: Boolean = true)
    suspend fun updateChildren(update: Map<String, Any?>, encodeDefaults: Boolean = true)
    suspend fun removeValue()

    suspend fun <T> runTransaction(strategy: KSerializer<T>, transactionUpdate: (currentData: T) -> T): DataSnapshot
}

expect class DataSnapshot {
    val exists: Boolean
    val key: String?
    inline fun <reified T> value(): T
    fun <T> value(strategy: DeserializationStrategy<T>): T
    fun child(path: String): DataSnapshot
    val children: Iterable<DataSnapshot>
}

object ServerValue {
    val TIMESTAMP = Double.POSITIVE_INFINITY
}

expect class DatabaseException(message: String?, cause: Throwable?) : RuntimeException

expect class OnDisconnect {
    suspend fun removeValue()
    suspend fun cancel()
    suspend inline fun <reified T> setValue(value: T, encodeDefaults: Boolean = true)
    suspend fun <T> setValue(strategy: SerializationStrategy<T>, value: T, encodeDefaults: Boolean = true)
    suspend fun updateChildren(update: Map<String, Any?>, encodeDefaults: Boolean = true)
}

