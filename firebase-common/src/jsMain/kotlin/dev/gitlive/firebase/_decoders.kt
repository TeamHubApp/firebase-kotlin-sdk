/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlin.js.Json

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
actual fun FirebaseDecoder.structureDecoder(descriptor: SerialDescriptor, decodeDouble: (value: Any?) -> Double?): CompositeDecoder = when(descriptor.kind as StructureKind) {
    StructureKind.CLASS, StructureKind.OBJECT -> (value as Json).let { json ->
        FirebaseClassDecoder(decodeDouble, js("Object").keys(value).length as Int, { json[it] != undefined }) {
                desc, index -> json[desc.getElementName(index)]
        }
    }
    StructureKind.LIST -> (value as Array<*>).let {
        FirebaseCompositeDecoder(decodeDouble, it.size) { _, index -> it[index] }
    }
    StructureKind.MAP -> (js("Object").entries(value) as Array<Array<Any>>).let {
        FirebaseCompositeDecoder(decodeDouble, it.size) { _, index -> it[index/2].run { if(index % 2 == 0) get(0) else get(1) } }
    }
}
