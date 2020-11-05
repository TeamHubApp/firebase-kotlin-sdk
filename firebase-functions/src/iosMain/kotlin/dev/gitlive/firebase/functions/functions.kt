/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.functions

import cocoapods.FirebaseFunctions.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.decode
import dev.gitlive.firebase.encode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import platform.Foundation.*

actual val Firebase.functions
    get() = FirebaseFunctions(FIRFunctions.functions())

actual fun Firebase.functions(region: String) =
    FirebaseFunctions(FIRFunctions.functionsForRegion(region))

actual fun Firebase.functions(app: FirebaseApp) =
    FirebaseFunctions(FIRFunctions.functionsForApp(app.ios))

actual fun Firebase.functions(app: FirebaseApp, region: String) =
    FirebaseFunctions(FIRFunctions.functionsForApp(app.ios, region))

actual class FirebaseFunctions internal constructor(val ios: FIRFunctions) {
    actual fun httpsCallable(name: String, timeout: Long?) =
        HttpsCallableReference(ios.HTTPSCallableWithName(name).apply { timeout?.let { setTimeoutInterval(it/1000.0) } })

    actual fun useFunctionsEmulator(origin: String) = ios.useFunctionsEmulatorOrigin(origin)
}

actual class HttpsCallableReference internal constructor(val ios: FIRHTTPSCallable) {
    actual suspend operator fun invoke() = HttpsCallableResult(ios.awaitResult { callWithCompletion(it) })

    actual suspend inline operator fun <reified T> invoke(data: T, encodeDefaults: Boolean) =
        HttpsCallableResult(ios.awaitResult { callWithObject(encode(data, encodeDefaults), it) })

    actual suspend operator fun <T> invoke(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        HttpsCallableResult(ios.awaitResult { callWithObject(encode(strategy, data, encodeDefaults), it) })
}

actual class HttpsCallableResult constructor(val ios: FIRHTTPSCallableResult) {

    actual inline fun <reified T> data() =
        decode<T>(value = ios.data)

    actual fun <T> data(strategy: DeserializationStrategy<T>) =
        decode(strategy, ios.data)
}

actual class FirebaseFunctionsException(message: String): FirebaseException(message)

suspend inline fun <T> T.await(function: T.(callback: (NSError?) -> Unit) -> Unit) {
    val job = CompletableDeferred<Unit>()
    function { error ->
        if(error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(FirebaseFunctionsException(error.toString()))
        }
    }
    job.await()
}

suspend inline fun <T, reified R> T.awaitResult(function: T.(callback: (R?, NSError?) -> Unit) -> Unit): R {
    val job = CompletableDeferred<R?>()
    function { result, error ->
        if(error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(FirebaseFunctionsException(error.toString()))
        }
    }
    return job.await() as R
}