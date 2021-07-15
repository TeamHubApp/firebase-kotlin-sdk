/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.database

import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.database.ServerValue
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.database.ChildEvent.Type
import dev.gitlive.firebase.decode
import dev.gitlive.firebase.safeOffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy

@PublishedApi
internal inline fun <reified T> encode(value: T, shouldEncodeElementDefault: Boolean) =
    dev.gitlive.firebase.encode(value, shouldEncodeElementDefault, ServerValue.TIMESTAMP)

internal fun <T> encode(strategy: SerializationStrategy<T> , value: T, shouldEncodeElementDefault: Boolean): Any? =
    dev.gitlive.firebase.encode(strategy, value, shouldEncodeElementDefault, ServerValue.TIMESTAMP)

suspend fun <T> Task<T>.awaitWhileOnline(): T = coroutineScope {

    val notConnected = Firebase.database
        .reference(".info/connected")
        .valueEvents
        .filter { !it.value<Boolean>() }
        .produceIn(this)

    select<T> {
        asDeferred().onAwait { it.also { notConnected.cancel() } }
        notConnected.onReceive { throw DatabaseException("Database not connected", null) }
    }
}

actual val Firebase.database
        by lazy { FirebaseDatabase(com.google.firebase.database.FirebaseDatabase.getInstance()) }

actual fun Firebase.database(url: String) =
    FirebaseDatabase(com.google.firebase.database.FirebaseDatabase.getInstance(url))

actual fun Firebase.database(app: FirebaseApp) =
    FirebaseDatabase(com.google.firebase.database.FirebaseDatabase.getInstance(app.android))

actual fun Firebase.database(app: FirebaseApp, url: String) =
    FirebaseDatabase(com.google.firebase.database.FirebaseDatabase.getInstance(app.android, url))

actual class FirebaseDatabase internal constructor(val android: com.google.firebase.database.FirebaseDatabase) {

    private var persistenceEnabled = true

    actual fun reference(path: String) =
        DatabaseReference(android.getReference(path), persistenceEnabled)

    actual fun setPersistenceEnabled(enabled: Boolean) =
        android.setPersistenceEnabled(enabled).also { persistenceEnabled = enabled }

    actual fun setLoggingEnabled(enabled: Boolean) =
        android.setLogLevel(Logger.Level.DEBUG.takeIf { enabled } ?: Logger.Level.NONE)

    actual fun useEmulator(host: String, port: Int) =
        android.useEmulator(host, port)
}

actual open class Query internal constructor(
    open val android: com.google.firebase.database.Query,
    val persistenceEnabled: Boolean
) {
    actual fun orderByKey() = Query(android.orderByKey(), persistenceEnabled)

    actual fun orderByValue() = Query(android.orderByValue(), persistenceEnabled)

    actual fun orderByChild(path: String) = Query(android.orderByChild(path), persistenceEnabled)

    actual fun startAt(value: String, key: String?) = Query(android.startAt(value, key), persistenceEnabled)

    actual fun startAt(value: Double, key: String?) = Query(android.startAt(value, key), persistenceEnabled)

    actual fun startAt(value: Boolean, key: String?) = Query(android.startAt(value, key), persistenceEnabled)

    actual fun endAt(value: String, key: String?) = Query(android.endAt(value, key), persistenceEnabled)

    actual fun endAt(value: Double, key: String?) = Query(android.endAt(value, key), persistenceEnabled)

    actual fun endAt(value: Boolean, key: String?) = Query(android.endAt(value, key), persistenceEnabled)

    actual fun limitToFirst(limit: Int) = Query(android.limitToFirst(limit), persistenceEnabled)

    actual fun limitToLast(limit: Int) = Query(android.limitToLast(limit), persistenceEnabled)

    actual fun equalTo(value: String, key: String?) = Query(android.equalTo(value, key), persistenceEnabled)

    actual fun equalTo(value: Double, key: String?) = Query(android.equalTo(value, key), persistenceEnabled)

    actual fun equalTo(value: Boolean, key: String?) = Query(android.equalTo(value, key), persistenceEnabled)

    actual val valueEvents: Flow<DataSnapshot>
        get() = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                safeOffer(DataSnapshot(snapshot))
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(error.toException())
            }
        }
        android.addValueEventListener(listener)
        awaitClose { android.removeEventListener(listener) }
    }

    actual fun childEvents(vararg types: Type): Flow<ChildEvent> = callbackFlow {
        val listener = object : ChildEventListener {

            val moved by lazy { types.contains(Type.MOVED) }
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                if(moved) safeOffer(ChildEvent(DataSnapshot(snapshot), Type.MOVED, previousChildName))
            }

            val changed by lazy { types.contains(Type.CHANGED) }
            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                if(changed) safeOffer(ChildEvent(DataSnapshot(snapshot), Type.CHANGED, previousChildName))
            }

            val added by lazy { types.contains(Type.ADDED) }
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                if(added) safeOffer(ChildEvent(DataSnapshot(snapshot), Type.ADDED, previousChildName))
            }

            val removed by lazy { types.contains(Type.REMOVED) }
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {
                if(removed) safeOffer(ChildEvent(DataSnapshot(snapshot), Type.REMOVED, null))
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(error.toException())
            }
        }
        android.addChildEventListener(listener)
        awaitClose { android.removeEventListener(listener) }
    }

    override fun toString() = android.toString()
}

actual class DatabaseReference internal constructor(
    override val android: com.google.firebase.database.DatabaseReference,
    persistenceEnabled: Boolean
): Query(android, persistenceEnabled) {

    actual val key get() = android.key

    actual fun child(path: String) = DatabaseReference(android.child(path), persistenceEnabled)

    actual fun push() = DatabaseReference(android.push(), persistenceEnabled)
    actual fun onDisconnect() = OnDisconnect(android.onDisconnect(), persistenceEnabled)

    actual suspend inline fun <reified T> setValue(value: T?, encodeDefaults: Boolean) = android.setValue(encode(value, encodeDefaults))
        .run { if(persistenceEnabled) await() else awaitWhileOnline() }
        .run { Unit }

    actual suspend fun <T> setValue(strategy: SerializationStrategy<T>, value: T, encodeDefaults: Boolean) =
        android.setValue(encode(strategy, value, encodeDefaults))
            .run { if(persistenceEnabled) await() else awaitWhileOnline() }
            .run { Unit }

    @Suppress("UNCHECKED_CAST")
    actual suspend fun updateChildren(update: Map<String, Any?>, encodeDefaults: Boolean) =
        android.updateChildren(encode(update, encodeDefaults) as Map<String, Any?>)
            .run { if(persistenceEnabled) await() else awaitWhileOnline() }
            .run { Unit }

    actual suspend fun removeValue() = android.removeValue()
        .run { if(persistenceEnabled) await() else awaitWhileOnline() }
        .run { Unit }

    actual suspend fun <T> runTransaction(strategy: KSerializer<T>, transactionUpdate: (currentData: T) -> T): DataSnapshot {
        val deferred = CompletableDeferred<Result<DataSnapshot>>()
        android.runTransaction(object : Transaction.Handler {

            override fun doTransaction(currentData: MutableData) =
                Transaction.success(transactionUpdate(decode(strategy, currentData)) as MutableData)

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: com.google.firebase.database.DataSnapshot?
            ) {
                if (error == null && snapshot != null) {
                    deferred.complete(Result.success(DataSnapshot(snapshot)))
                } else {
                    deferred.complete(Result.failure(Throwable(error?.message)))
                }
            }

        })
        return deferred.await().getOrThrow()
    }
}
@Suppress("UNCHECKED_CAST")
actual class DataSnapshot internal constructor(val android: com.google.firebase.database.DataSnapshot) {

    actual val exists get() = android.exists()

    actual val key get() = android.key

    actual inline fun <reified T> value() =
        decode<T>(value = android.value)

    actual fun <T> value(strategy: DeserializationStrategy<T>) =
        decode(strategy, android.value)

    actual fun child(path: String) = DataSnapshot(android.child(path))
    actual val children: Iterable<DataSnapshot> get() = android.children.map { DataSnapshot(it) }
}

actual class OnDisconnect internal constructor(
    val android: com.google.firebase.database.OnDisconnect,
    val persistenceEnabled: Boolean
) {

    actual suspend fun removeValue() = android.removeValue()
        .run { if(persistenceEnabled) await() else awaitWhileOnline() }
        .run { Unit }

    actual suspend fun cancel() = android.cancel()
        .run { if(persistenceEnabled) await() else awaitWhileOnline() }
        .run { Unit }

    actual suspend inline fun <reified T> setValue(value: T, encodeDefaults: Boolean) =
        android.setValue(encode(value, encodeDefaults))
            .run { if(persistenceEnabled) await() else awaitWhileOnline() }
            .run { Unit }

    actual suspend fun <T> setValue(strategy: SerializationStrategy<T>, value: T, encodeDefaults: Boolean) =
        android.setValue(encode(strategy, value, encodeDefaults))
            .run { if(persistenceEnabled) await() else awaitWhileOnline() }
            .run { Unit}

    actual suspend fun updateChildren(update: Map<String, Any?>, encodeDefaults: Boolean) =
        android.updateChildren(update.mapValues { (_, it) -> encode(it, encodeDefaults) })
            .run { if(persistenceEnabled) await() else awaitWhileOnline() }
            .run { Unit }
}

actual typealias DatabaseException = com.google.firebase.database.DatabaseException

