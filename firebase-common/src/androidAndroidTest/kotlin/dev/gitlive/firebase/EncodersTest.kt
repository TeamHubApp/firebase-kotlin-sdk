/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

actual fun nativeMapOf(vararg pairs: Pair<String, Any?>): Any = mapOf(*pairs)
actual fun nativeListOf(vararg elements: Any): Any = listOf(*elements)
actual fun nativeAssertEquals(expected: Any?, actual: Any?)  = kotlin.test.assertEquals(expected, actual)
