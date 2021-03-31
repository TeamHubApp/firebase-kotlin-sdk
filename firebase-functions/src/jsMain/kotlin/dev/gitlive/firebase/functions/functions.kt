/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.functions

import dev.gitlive.firebase.*
import kotlinx.coroutines.await
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlin.js.json

actual val Firebase.functions: FirebaseFunctions
    get() = rethrow { dev.gitlive.firebase.functions; FirebaseFunctions(firebase.functions()) }

actual fun Firebase.functions(region: String) =
    rethrow { dev.gitlive.firebase.functions; FirebaseFunctions(firebase.app().functions(region)) }

actual fun Firebase.functions(app: FirebaseApp) =
    rethrow { dev.gitlive.firebase.functions; FirebaseFunctions(firebase.functions(app.js)) }

actual fun Firebase.functions(app: FirebaseApp, region: String) =
    rethrow { dev.gitlive.firebase.functions; FirebaseFunctions(app.js.functions(region)) }

actual class FirebaseFunctions internal constructor(val js: firebase.functions.Functions) {
    actual fun httpsCallable(name: String, timeout: Long?) =
        rethrow { HttpsCallableReference(js.httpsCallable(name, timeout?.let { json("timeout" to timeout.toDouble()) })) }

    actual fun useFunctionsEmulator(origin: String) = js.useFunctionsEmulator(origin)

    actual fun useEmulator(host: String, port: Int) = js.useEmulator(host, port)
}

@Suppress("UNCHECKED_CAST")
actual class HttpsCallableReference internal constructor(val js: firebase.functions.HttpsCallable) {

    actual suspend operator fun invoke() =
        rethrow { HttpsCallableResult(js().await()) }

    actual suspend inline operator fun <reified T> invoke(data: T, encodeDefaults: Boolean) =
        rethrow { HttpsCallableResult(js(encode(data, encodeDefaults)).await()) }

    actual suspend operator fun <T> invoke(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        rethrow { HttpsCallableResult(js(encode(strategy, data, encodeDefaults)).await()) }
}

actual class HttpsCallableResult constructor(val js: firebase.functions.HttpsCallableResult) {

    actual inline fun <reified T> data() =
        rethrow { decode<T>(value = js.data) }

    actual fun <T> data(strategy: DeserializationStrategy<T>) =
        rethrow { decode(strategy, js.data) }

}

actual open class FirebaseFunctionsException(code: String?, cause: Throwable): FirebaseException(code, cause)

inline fun <T, R> T.rethrow(function: T.() -> R): R = dev.gitlive.firebase.functions.rethrow { function() }

inline fun <R> rethrow(function: () -> R): R {
    try {
        return function()
    } catch (e: Exception) {
        throw e
    } catch(e: dynamic) {
        throw FirebaseFunctionsException(e.code as String?, e)
    }
}
