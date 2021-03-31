/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.database

import cocoapods.FirebaseDatabase.*
import cocoapods.FirebaseDatabase.FIRDataEventType.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.database.ChildEvent.Type
import dev.gitlive.firebase.database.ChildEvent.Type.*
import dev.gitlive.firebase.decode
import dev.gitlive.firebase.safeOffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.select
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import platform.Foundation.*
import kotlin.collections.component1
import kotlin.collections.component2

@PublishedApi
internal inline fun <reified T> encode(value: T, shouldEncodeElementDefault: Boolean) =
    dev.gitlive.firebase.encode(value, shouldEncodeElementDefault, FIRServerValue.timestamp())

internal fun <T> encode(strategy: SerializationStrategy<T> , value: T, shouldEncodeElementDefault: Boolean): Any? =
    dev.gitlive.firebase.encode(strategy, value, shouldEncodeElementDefault, FIRServerValue.timestamp())

actual val Firebase.database
        by lazy { FirebaseDatabase(FIRDatabase.database()) }

actual fun Firebase.database(url: String) =
    FirebaseDatabase(FIRDatabase.databaseWithURL(url))

actual fun Firebase.database(app: FirebaseApp) =
    FirebaseDatabase(FIRDatabase.databaseForApp(app.ios))

actual fun Firebase.database(app: FirebaseApp, url: String) =
    FirebaseDatabase(FIRDatabase.databaseForApp(app.ios, url))

actual class FirebaseDatabase internal constructor(val ios: FIRDatabase) {

    actual fun reference(path: String) =
        DatabaseReference(ios.referenceWithPath(path), ios.persistenceEnabled)

    actual fun setPersistenceEnabled(enabled: Boolean) {
        ios.persistenceEnabled = enabled
    }

    actual fun setLoggingEnabled(enabled: Boolean) =
        FIRDatabase.setLoggingEnabled(enabled)

    actual fun useEmulator(host: String, port: Int) =
        ios.useEmulatorWithHost(host, port.toLong())
}

fun Type.toEventType() = when(this) {
    ADDED -> FIRDataEventTypeChildAdded
    CHANGED -> FIRDataEventTypeChildChanged
    MOVED -> FIRDataEventTypeChildMoved
    REMOVED -> FIRDataEventTypeChildRemoved
}

actual open class Query internal constructor(
    open val ios: FIRDatabaseQuery,
    val persistenceEnabled: Boolean
) {
    actual fun orderByKey() = Query(ios.queryOrderedByKey(), persistenceEnabled)

    actual fun orderByValue() = Query(ios.queryOrderedByValue(), persistenceEnabled)

    actual fun orderByChild(path: String) = Query(ios.queryOrderedByChild(path), persistenceEnabled)

    actual fun startAt(value: String, key: String?) = Query(ios.queryStartingAtValue(value, key), persistenceEnabled)

    actual fun startAt(value: Double, key: String?) = Query(ios.queryStartingAtValue(value, key), persistenceEnabled)

    actual fun startAt(value: Boolean, key: String?) = Query(ios.queryStartingAtValue(value, key), persistenceEnabled)

    actual fun endAt(value: String, key: String?) = Query(ios.queryEndingAtValue(value, key), persistenceEnabled)

    actual fun endAt(value: Double, key: String?) = Query(ios.queryEndingAtValue(value, key), persistenceEnabled)

    actual fun endAt(value: Boolean, key: String?) = Query(ios.queryEndingAtValue(value, key), persistenceEnabled)

    actual fun limitToFirst(limit: Int) = Query(ios.queryLimitedToFirst(limit.toULong()), persistenceEnabled)

    actual fun limitToLast(limit: Int) = Query(ios.queryLimitedToLast(limit.toULong()), persistenceEnabled)

    actual fun equalTo(value: String, key: String?) = Query(ios.queryEqualToValue(value, key), persistenceEnabled)

    actual fun equalTo(value: Double, key: String?) = Query(ios.queryEqualToValue(value, key), persistenceEnabled)

    actual fun equalTo(value: Boolean, key: String?) = Query(ios.queryEqualToValue(value, key), persistenceEnabled)

    actual val valueEvents get() = callbackFlow<DataSnapshot> {
        val handle = ios.observeEventType(
            FIRDataEventTypeValue,
            withBlock = { snapShot ->
                safeOffer(DataSnapshot(snapShot!!, persistenceEnabled))
            }
        ) { close(DatabaseException(it.toString())) }
        awaitClose { ios.removeObserverWithHandle(handle) }
    }

    actual fun childEvents(vararg types: Type) = callbackFlow<ChildEvent> {
        val handles = types.map { type ->
            ios.observeEventType(
                type.toEventType(),
                andPreviousSiblingKeyWithBlock = { snapShot, key ->
                    safeOffer(ChildEvent(DataSnapshot(snapShot!!, persistenceEnabled), type, key))
                }
            ) { close(DatabaseException(it.toString())) }
        }
        awaitClose {
            handles.forEach { ios.removeObserverWithHandle(it) }
        }
    }

    override fun toString() = ios.toString()
}

actual class DatabaseReference internal constructor(
    override val ios: FIRDatabaseReference,
    persistenceEnabled: Boolean
): Query(ios, persistenceEnabled) {

    actual val key get() = ios.key

    actual fun child(path: String) = DatabaseReference(ios.child(path), persistenceEnabled)

    actual fun push() = DatabaseReference(ios.childByAutoId(), persistenceEnabled)
    actual fun onDisconnect() = OnDisconnect(ios, persistenceEnabled)

    actual suspend inline fun <reified T> setValue(value: T?, encodeDefaults: Boolean) {
        ios.await(persistenceEnabled) { setValue(encode(value, encodeDefaults), it) }
    }

    actual suspend fun <T> setValue(strategy: SerializationStrategy<T>, value: T, encodeDefaults: Boolean) {
        ios.await(persistenceEnabled) { setValue(encode(strategy, value, encodeDefaults), it) }
    }

    @Suppress("UNCHECKED_CAST")
    actual suspend fun updateChildren(update: Map<String, Any?>, encodeDefaults: Boolean) {
        ios.await(persistenceEnabled) { updateChildValues(encode(update, encodeDefaults) as Map<Any?, *>, it) }
    }

    actual suspend fun removeValue() {
        ios.await(persistenceEnabled) { removeValueWithCompletionBlock(it) }
    }
}

@Suppress("UNCHECKED_CAST")
actual class DataSnapshot internal constructor(
    val ios: FIRDataSnapshot,
    private val persistenceEnabled: Boolean
) {

    actual val exists get() = ios.exists()

    actual val key: String? get() = ios.key

    actual val ref: DatabaseReference get() = DatabaseReference(ios.ref, persistenceEnabled)

    actual inline fun <reified T> value() =
        decode<T>(value = ios.value)

    actual fun <T> value(strategy: DeserializationStrategy<T>) =
        decode(strategy, ios.value)

    actual fun child(path: String) = DataSnapshot(ios.childSnapshotForPath(path), persistenceEnabled)
    actual val children: Iterable<DataSnapshot> get() = ios.children.allObjects.map { DataSnapshot(it as FIRDataSnapshot, persistenceEnabled) }
}

actual class OnDisconnect internal constructor(
    val ios: FIRDatabaseReference,
    val persistenceEnabled: Boolean
) {
    actual suspend fun removeValue() {
        ios.await(persistenceEnabled) { onDisconnectRemoveValueWithCompletionBlock(it) }
    }

    actual suspend fun cancel() {
        ios.await(persistenceEnabled) { cancelDisconnectOperationsWithCompletionBlock(it) }
    }

    actual suspend inline fun <reified T> setValue(value: T, encodeDefaults: Boolean) {
        ios.await(persistenceEnabled) { onDisconnectSetValue(encode(value, encodeDefaults), it) }
    }

    actual suspend fun <T> setValue(strategy: SerializationStrategy<T>, value: T, encodeDefaults: Boolean) {
        ios.await(persistenceEnabled) { onDisconnectSetValue(encode(strategy, value, encodeDefaults), it) }
    }

    @Suppress("UNCHECKED_CAST")
    actual suspend fun updateChildren(update: Map<String, Any?>, encodeDefaults: Boolean) {
        ios.await(persistenceEnabled) { onDisconnectUpdateChildValues(update.mapValues { (_, it) -> encode(it, encodeDefaults) } as Map<Any?, *>, it) }
    }
}

actual class DatabaseException(message: String) : RuntimeException(message)

private suspend inline fun <T, reified R> T.awaitResult(whileOnline: Boolean, function: T.(callback: (NSError?, R?) -> Unit) -> Unit): R {
    val job = CompletableDeferred<R?>()
    function { error, result ->
        if(error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(DatabaseException(error.toString()))
        }
    }
    return job.run { if(whileOnline) awaitWhileOnline() else await() } as R
}

suspend inline fun <T> T.await(whileOnline: Boolean, function: T.(callback: (NSError?, FIRDatabaseReference?) -> Unit) -> Unit) {
    val job = CompletableDeferred<Unit>()
    function { error, _ ->
        if(error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(DatabaseException(error.toString()))
        }
    }
    job.run { if(whileOnline) awaitWhileOnline() else await() }
}

@FlowPreview
suspend fun <T> CompletableDeferred<T>.awaitWhileOnline(): T = coroutineScope {

    val notConnected = Firebase.database
        .reference(".info/connected")
        .valueEvents
        .filter { !it.value<Boolean>() }
        .produceIn(this)

    select<T> {
        onAwait { it.also { notConnected.cancel() } }
        notConnected.onReceive { throw DatabaseException("Database not connected") }
    }
}
