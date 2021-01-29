package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.FirebaseCompositeEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class FirebaseBaseTimestampSerializer<T> : KSerializer<T> {

    override val descriptor = object : SerialDescriptor {
        val keys = listOf("seconds", "nanoseconds")
        override val kind = StructureKind.OBJECT
        override val serialName = "Timestamp"
        override val elementsCount get() = 2
        override fun getElementIndex(name: String) = keys.indexOf(name)
        override fun getElementName(index: Int) = keys[index]
        override fun getElementAnnotations(index: Int) = emptyList<Annotation>()
        override fun getElementDescriptor(index: Int) = throw NotImplementedError()
        override fun isElementOptional(index: Int) = false
    }

    override fun serialize(encoder: Encoder, value: T) {
        val objectEncoder = encoder.beginStructure(descriptor) as FirebaseCompositeEncoder
        objectEncoder.encodeObject(descriptor, 0, value)
        objectEncoder.endStructure(descriptor)
    }

    abstract override fun deserialize(decoder: Decoder): T
}
