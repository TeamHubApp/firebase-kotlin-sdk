/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlin.js.Promise

@JsModule("firebase/functions")
external object functions

@JsModule("firebase/auth")
external object auth

@JsModule("firebase/database")
external object database

@JsModule("firebase/firestore")
external object firestore

typealias SnapshotCallback = (data: firebase.database.DataSnapshot, b: String?) -> Unit

operator fun firebase.functions.HttpsCallable.invoke() = asDynamic()() as Promise<firebase.functions.HttpsCallableResult>
operator fun firebase.functions.HttpsCallable.invoke(data: Any?) = asDynamic()(data) as Promise<firebase.functions.HttpsCallableResult>

