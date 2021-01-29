/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import cocoapods.FirebaseCore.*

actual open class FirebaseException(message: String) : Exception(message)
actual open class FirebaseNetworkException(message: String) : FirebaseException(message)
actual open class FirebaseTooManyRequestsException(message: String) : FirebaseException(message)
actual open class FirebaseApiNotAvailableException(message: String) : FirebaseException(message)

actual val Firebase.app: FirebaseApp
    get() = FirebaseApp(FIRApp.defaultApp()!!)

actual fun Firebase.app(name: String): FirebaseApp =
    FirebaseApp(FIRApp.appNamed(name)!!)

actual fun Firebase.initialize(context: Any?): FirebaseApp? =
    FIRApp.configure().let { app }

actual fun Firebase.initialize(context: Any?, options: FirebaseOptions, name: String): FirebaseApp =
    FIRApp.configureWithName(name, options.toIos()).let { app(name) }

actual fun Firebase.initialize(context: Any?, options: FirebaseOptions) =
    FIRApp.configureWithOptions(options.toIos()).let { app }

actual class FirebaseApp internal constructor(val ios: FIRApp) {
    actual val name: String
        get() = ios.name
    actual val options: FirebaseOptions
        get() = ios.options.run { FirebaseOptions(bundleID, APIKey!!, databaseURL!!, trackingID, storageBucket, projectID, GCMSenderID) }
}

actual fun Firebase.apps(context: Any?) = FIRApp.allApps()
    .orEmpty()
    .values
    .map { FirebaseApp(it as FIRApp) }

actual class FirebaseOptions actual constructor(
    actual val applicationId: String,
    actual val apiKey: String,
    actual val databaseUrl: String?,
    actual val gaTrackingId: String?,
    actual val storageBucket: String?,
    actual val projectId: String?,
    actual val gcmSenderId: String?
) {
    actual companion object {
        actual fun withContext(context: Any): FirebaseOptions? {
            return when (context) {
                is String -> FIROptions(contentsOfFile = context)
                else -> FIROptions.defaultOptions()
            }?.run {
                FirebaseOptions(googleAppID!!, APIKey!!, databaseURL, trackingID, storageBucket, projectID, GCMSenderID)
            }
        }
    }
}

private fun FirebaseOptions.toIos() = FIROptions(this@toIos.applicationId, this@toIos.gcmSenderId ?: "").apply {
        APIKey = this@toIos.apiKey
        databaseURL = this@toIos.databaseUrl
        trackingID = this@toIos.gaTrackingId
        storageBucket = this@toIos.storageBucket
        projectID = this@toIos.projectId
    }
