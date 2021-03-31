/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual val context: Any = Unit

actual fun runTest(test: suspend () -> Unit) = GlobalScope
    .promise {
        try {
            test()
        } catch (e: dynamic) {
            e.log()
            throw e
        }
    }.asDynamic()

internal fun Throwable.log() {
    console.error(this)
    cause?.let {
        console.error("Caused by:")
        it.log()
    }
}