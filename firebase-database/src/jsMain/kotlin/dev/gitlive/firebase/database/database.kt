/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.database

import dev.gitlive.firebase.*
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

fun encode(value: Any?, shouldEncodeElementDefault: Boolean) =
    encode(value, shouldEncodeElementDefault, firebase.database.ServerValue.TIMESTAMP)
fun <T> encode(strategy: SerializationStrategy<T>, value: T, shouldEncodeElementDefault: Boolean): Any? =
    encode(strategy, value, shouldEncodeElementDefault, firebase.database.ServerValue.TIMESTAMP)


actual val Firebase.database
    get() = rethrow { dev.gitlive.firebase.database; FirebaseDatabase(firebase.database()) }

actual fun Firebase.database(app: FirebaseApp) =
    rethrow { dev.gitlive.firebase.database; FirebaseDatabase(firebase.database(app.js)) }

actual fun Firebase.database(url: String) =
    rethrow { dev.gitlive.firebase.database; FirebaseDatabase(firebase.app().database(url)) }

actual fun Firebase.database(app: FirebaseApp, url: String) =
    rethrow { dev.gitlive.firebase.database; FirebaseDatabase(app.js.database(url)) }

actual class FirebaseDatabase internal constructor(val js: firebase.database.Database) {
    actual fun reference(path: String) = rethrow { DatabaseReference(js.ref(path)) }
    actual fun setPersistenceEnabled(enabled: Boolean) {}
    actual fun setLoggingEnabled(enabled: Boolean) = rethrow { firebase.database.enableLogging(enabled) }
}

actual open class Query internal constructor(open val js: firebase.database.Query) {

    actual fun orderByKey() = Query(js.orderByKey())
    actual fun orderByChild(path: String) = Query(js.orderByChild(path))

    actual val valueEvents get() = callbackFlow<DataSnapshot> {
        val listener = rethrow {
            js.on(
                "value",
                { it, _ -> safeOffer(DataSnapshot(it)) },
                { close(DatabaseException(it)).run { Unit } }
            )
        }
        awaitClose { rethrow { js.off("value", listener) } }
    }

    actual fun childEvents(vararg types: ChildEvent.Type) = callbackFlow<ChildEvent> {
        val listeners = rethrow {
            types.map { type ->
                "child_${type.name.toLowerCase()}".let { eventType ->
                    eventType to js.on(
                        eventType,
                        { snapshot, previousChildName ->
                            safeOffer(
                                ChildEvent(
                                    DataSnapshot(snapshot),
                                    type,
                                    previousChildName
                                )
                            )
                        },
                        { close(DatabaseException(it)).run { Unit } }
                    )
                }
            }
        }
        awaitClose { rethrow { listeners.forEach { (eventType, listener) -> js.off(eventType, listener) } } }
    }

    actual fun startAt(value: String, key: String?) = Query(js.startAt(value, key ?: undefined))

    actual fun startAt(value: Double, key: String?) = Query(js.startAt(value, key ?: undefined))

    actual fun startAt(value: Boolean, key: String?) = Query(js.startAt(value, key ?: undefined))

    override fun toString() = js.toString()

}

actual class DatabaseReference internal constructor(override val js: firebase.database.Reference): Query(js) {

    actual val key get() = rethrow { js.key }
    actual fun push() = rethrow { DatabaseReference(js.push()) }
    actual fun child(path: String) = rethrow { DatabaseReference(js.child(path)) }

    actual fun onDisconnect() = rethrow { OnDisconnect(js.onDisconnect()) }

    actual suspend fun updateChildren(update: Map<String, Any?>, encodeDefaults: Boolean) =
        rethrow { js.update(encode(update, encodeDefaults)).await() }

    actual suspend fun removeValue() = rethrow { js.remove().await() }

    actual suspend inline fun <reified T> setValue(value: T?, encodeDefaults: Boolean) = rethrow {
        js.set(encode(value, encodeDefaults)).await()
    }

    actual suspend fun <T> setValue(strategy: SerializationStrategy<T>, value: T, encodeDefaults: Boolean) =
        rethrow { js.set(encode(strategy, value, encodeDefaults)).await() }
}

actual class DataSnapshot internal constructor(val js: firebase.database.DataSnapshot) {

    actual inline fun <reified T> value() =
        rethrow { decode<T>(value = js.`val`()) }

    actual fun <T> value(strategy: DeserializationStrategy<T>) =
        rethrow { decode(strategy, js.`val`()) }

    actual val exists get() = rethrow { js.exists() }
    actual val key get() = rethrow { js.key }
    actual fun child(path: String) = DataSnapshot(js.child(path))

    actual val children: Iterable<DataSnapshot> = rethrow {
        ArrayList<DataSnapshot>(js.numChildren()).also {
            js.forEach { snapshot -> it.add(DataSnapshot(snapshot)) }
        }
    }

}

actual class OnDisconnect internal constructor(val js: firebase.database.OnDisconnect) {

    actual suspend fun removeValue() = rethrow { js.remove().await() }
    actual suspend fun cancel() =  rethrow { js.cancel().await() }

    actual suspend fun updateChildren(update: Map<String, Any?>, encodeDefaults: Boolean) =
        rethrow { js.update(encode(update, encodeDefaults)).await() }

    actual suspend inline fun <reified T> setValue(value: T, encodeDefaults: Boolean) =
        rethrow { js.set(encode(value, encodeDefaults)).await() }

    actual suspend fun <T> setValue(strategy: SerializationStrategy<T>, value: T, encodeDefaults: Boolean) =
        rethrow { js.set(encode(strategy, value, encodeDefaults)).await() }
}

actual class DatabaseException(error: dynamic) :
    RuntimeException("${error.code ?: "UNKNOWN"}: ${error.message}", error.unsafeCast<Throwable>())

inline fun <T, R> T.rethrow(function: T.() -> R): R = dev.gitlive.firebase.database.rethrow { function() }

inline fun <R> rethrow(function: () -> R): R {
    try {
        return function()
    } catch (e: Exception) {
        throw e
    } catch(e: dynamic) {
        throw DatabaseException(e)
    }
}
