/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import kotlinx.coroutines.*
import platform.Foundation.*

actual val emulatorHost: String = "localhost"

actual val context: Any = Unit

actual val currentPlatform: Platform = Platform.IOS

actual fun runTest(skip: Boolean, test: suspend () -> Unit) = runBlocking {
    if (skip) {
        NSLog("Skip the test.")
        return@runBlocking
    }

    val testRun = MainScope().async { test() }
    while (testRun.isActive) {
        NSRunLoop.mainRunLoop.runMode(
            NSDefaultRunLoopMode,
            beforeDate = NSDate.create(timeInterval = 1.0, sinceDate = NSDate())
        )
        yield()
    }
    testRun.await()
}
