/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import platform.Foundation.*
import platform.darwin.NSObject

actual fun FirebaseDecoder.structureDecoder(descriptor: SerialDescriptor): CompositeDecoder = when(descriptor.kind) {
    StructureKind.CLASS, StructureKind.OBJECT -> when {
        value is Map<*, *> ->
            FirebaseClassDecoder(value.size, { value.containsKey(it) }) { desc, index ->
                value[desc.getElementName(index)]
            }
        value is NSObject && NSClassFromString("FIRTimestamp") == value.`class`() -> {
            makeFIRTimestampDecoder(value)
        }
        else -> FirebaseEmptyCompositeDecoder()
    }
    StructureKind.LIST, is PolymorphicKind -> (value as List<*>).let {
        FirebaseCompositeDecoder(it.size) { _, index -> it[index] }
    }
    StructureKind.MAP -> (value as Map<*, *>).entries.toList().let {
        FirebaseCompositeDecoder(it.size) { _, index -> it[index/2].run { if(index % 2 == 0) key else value }  }
    }
    else -> TODO("Not implemented ${descriptor.kind}")
}

private val timestampKeys = setOf("seconds", "nanoseconds")
private fun makeFIRTimestampDecoder(objcObj: NSObject) = FirebaseClassDecoder(
    size = 2,
    containsKey = { timestampKeys.contains(it) }
) { descriptor, index ->
    objcObj.valueForKeyPath(descriptor.getElementName(index))
}
