/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import android.content.Context

actual typealias FirebaseException = com.google.firebase.FirebaseException

actual typealias FirebaseNetworkException = com.google.firebase.FirebaseNetworkException

actual typealias FirebaseTooManyRequestsException = com.google.firebase.FirebaseTooManyRequestsException

actual typealias FirebaseApiNotAvailableException = com.google.firebase.FirebaseApiNotAvailableException

actual val Firebase.app: FirebaseApp
    get() = FirebaseApp(com.google.firebase.FirebaseApp.getInstance())

actual fun Firebase.app(name: String): FirebaseApp =
    FirebaseApp(com.google.firebase.FirebaseApp.getInstance(name))

actual fun Firebase.initialize(context: Any?): FirebaseApp? =
    com.google.firebase.FirebaseApp.initializeApp(context as Context)?.let { FirebaseApp(it) }

actual fun Firebase.initialize(context: Any?, options: FirebaseOptions, name: String): FirebaseApp =
    FirebaseApp(com.google.firebase.FirebaseApp.initializeApp(context as Context, options.toAndroid(), name))

actual fun Firebase.initialize(context: Any?, options: FirebaseOptions) =
    FirebaseApp(com.google.firebase.FirebaseApp.initializeApp(context as Context, options.toAndroid()))

actual class FirebaseApp internal constructor(val android: com.google.firebase.FirebaseApp) {
    actual val name: String
        get() = android.name
    actual val options: FirebaseOptions
        get() = android.options.run { FirebaseOptions(applicationId, apiKey, databaseUrl, gaTrackingId, storageBucket, projectId, gcmSenderId) }
}

actual fun Firebase.apps(context: Any?) = com.google.firebase.FirebaseApp.getApps(context as Context)
    .map { FirebaseApp(it) }

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
                is Context -> com.google.firebase.FirebaseOptions.fromResource(context)
                else -> com.google.firebase.FirebaseOptions.Builder().build()
            }?.run {
                FirebaseOptions(applicationId, apiKey, databaseUrl, gaTrackingId, storageBucket, projectId, gcmSenderId)
            }
        }
    }
}

private fun FirebaseOptions.toAndroid() = com.google.firebase.FirebaseOptions.Builder()
    .setApplicationId(applicationId)
    .setApiKey(apiKey)
    .setDatabaseUrl(databaseUrl)
    .setGaTrackingId(gaTrackingId)
    .setStorageBucket(storageBucket)
    .setProjectId(projectId)
    .setGcmSenderId(gcmSenderId)
    .build()
