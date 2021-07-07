/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("tests")
package dev.gitlive.firebase.auth

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking

actual val emulatorHost: String = "10.0.2.2"

actual val context: Any = InstrumentationRegistry.getInstrumentation().targetContext

actual val currentPlatform: Platform = Platform.Android

actual fun runTest(skip: Boolean, test: suspend () -> Unit) = runBlocking {
    if (skip) {
        Log.w("Test", "Skip the test.")
        return@runBlocking
    }

    test()
}

