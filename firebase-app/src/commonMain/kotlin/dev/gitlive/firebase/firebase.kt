/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmMultifileClass
@file:JvmName("CommonKt")
package dev.gitlive.firebase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Single access point to all firebase sdks from Kotlin.
 *
 * <p>Acts as a target for extension methods provided by sdks.
 */
object Firebase

expect class FirebaseApp {
    val name: String
    val options: FirebaseOptions
}

/** Returns the default firebase app instance. */
expect val Firebase.app: FirebaseApp

/** Returns a named firebase app instance. */
expect fun Firebase.app(name: String): FirebaseApp

/** Returns all firebase app instances. */
expect fun Firebase.apps(context: Any? = null): List<FirebaseApp>

/** Initializes and returns a FirebaseApp. */
expect fun Firebase.initialize(context: Any? = null): FirebaseApp?

/** Initializes and returns a FirebaseApp. */
expect fun Firebase.initialize(context: Any? = null, options: FirebaseOptions): FirebaseApp

/** Initializes and returns a FirebaseApp. */
expect fun Firebase.initialize(context: Any? = null, options: FirebaseOptions, name: String): FirebaseApp

/** Returns options of default FirebaseApp */
val Firebase.options: FirebaseOptions
    get() = Firebase.app.options

expect class FirebaseOptions(
    applicationId: String,
    apiKey: String,
    databaseUrl: String? = null,
    gaTrackingId: String? = null,
    storageBucket: String? = null,
    projectId: String? = null,
    gcmSenderId: String? = null
) {
    val applicationId: String
    val apiKey: String
    val databaseUrl: String?
    val gaTrackingId: String?
    val storageBucket: String?
    val projectId: String?
    val gcmSenderId: String?

    companion object {
        fun withContext(context: Any): FirebaseOptions?
    }
}

expect open class FirebaseException : Exception

expect class FirebaseNetworkException : FirebaseException

expect open class FirebaseTooManyRequestsException : FirebaseException

expect open class FirebaseApiNotAvailableException : FirebaseException

