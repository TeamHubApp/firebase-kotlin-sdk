package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.FirebaseCompositeDecoder
import kotlinx.serialization.encoding.Decoder

class FirebaseTimestampSerializer : FirebaseBaseTimestampSerializer<Timestamp>() {

    override fun deserialize(decoder: Decoder): Timestamp {
        val objectDecoder = decoder.beginStructure(descriptor) as FirebaseCompositeDecoder
        val seconds = objectDecoder.decodeLongElement(descriptor, 0)
        val nanoseconds = objectDecoder.decodeIntElement(descriptor, 1)
        objectDecoder.endStructure(descriptor)
        return timestampWith(seconds, nanoseconds)
    }
}
