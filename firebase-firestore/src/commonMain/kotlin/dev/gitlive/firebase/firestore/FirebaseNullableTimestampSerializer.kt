package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.FirebaseCompositeDecoder
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder

class FirebaseNullableTimestampSerializer :FirebaseBaseTimestampSerializer<Timestamp?>() {

    override fun deserialize(decoder: Decoder): Timestamp? {
        val objectDecoder = decoder.beginStructure(descriptor) as FirebaseCompositeDecoder
        return try {
            val seconds = objectDecoder.decodeLongElement(descriptor, 0)
            val nanoseconds = objectDecoder.decodeIntElement(descriptor, 1)
            objectDecoder.endStructure(descriptor)
            timestampWith(seconds, nanoseconds)
        } catch (exception: SerializationException) {
            objectDecoder.endStructure(descriptor)
            null
        }
    }
}
