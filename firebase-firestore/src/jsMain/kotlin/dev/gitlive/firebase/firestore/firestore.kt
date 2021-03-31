/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.promise
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlin.js.json

@PublishedApi
internal inline fun <reified T> decode(value: Any?): T =
    decode(value) { it.takeIf { it.asDynamic().toMillis != undefined }?.asDynamic().toMillis() as? Double }

internal fun <T> decode(strategy: DeserializationStrategy<T>, value: Any?): T =
    decode(strategy, value) { it.takeIf { it.asDynamic().toMillis != undefined }?.asDynamic().toMillis() as? Double }

@PublishedApi
internal inline fun <reified T> encode(value: T, shouldEncodeElementDefault: Boolean) =
    encode(value, shouldEncodeElementDefault, firebase.firestore.FieldValue.serverTimestamp())

private fun <T> encode(strategy: SerializationStrategy<T> , value: T, shouldEncodeElementDefault: Boolean): Any? =
    encode(strategy, value, shouldEncodeElementDefault, firebase.firestore.FieldValue.serverTimestamp())

actual val Firebase.firestore get() =
    rethrow { dev.gitlive.firebase.firestore; FirebaseFirestore(firebase.firestore()) }

actual fun Firebase.firestore(app: FirebaseApp) =
    rethrow { dev.gitlive.firebase.firestore; FirebaseFirestore(firebase.app().firestore()) }

actual class FirebaseFirestore(val js: firebase.firestore.Firestore) {

    actual fun collection(collectionPath: String) = rethrow { CollectionReference(js.collection(collectionPath)) }

    actual fun collectionGroup(collectionId: String) = Query(js.collectionGroup(collectionId))

    actual fun document(documentPath: String) = rethrow { DocumentReference(js.doc(documentPath)) }

    actual fun batch() = rethrow { WriteBatch(js.batch()) }

    actual fun setLoggingEnabled(loggingEnabled: Boolean) =
        rethrow { firebase.firestore.setLogLevel( if(loggingEnabled) "error" else "silent") }

    actual suspend fun <T> runTransaction(func: suspend Transaction.() -> T) =
        rethrow { js.runTransaction { GlobalScope.promise { Transaction(it).func() } }.await() }

    actual suspend fun clearPersistence() =
        rethrow { js.clearPersistence().await() }

    actual fun useEmulator(host: String, port: Int) = rethrow { js.useEmulator(host, port) }

    actual fun setSettings(persistenceEnabled: Boolean?, sslEnabled: Boolean?, host: String?, cacheSizeBytes: Long?) {
        if(persistenceEnabled == true) js.enablePersistence()

        js.settings(json().apply {
            sslEnabled?.let { set("ssl", it) }
            host?.let { set("host", it) }
            cacheSizeBytes?.let { set("cacheSizeBytes", it) }
        })
    }

    actual suspend fun disableNetwork() {
        rethrow { js.disableNetwork().await() }
    }

    actual suspend fun enableNetwork() {
        rethrow { js.enableNetwork().await() }
    }
}

actual class WriteBatch(val js: firebase.firestore.WriteBatch) {

    actual inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, merge: Boolean) =
        rethrow { js.set(documentRef.js, encode(data, encodeDefaults)!!, json("merge" to merge)) }
            .let { this }

    actual inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        rethrow { js.set(documentRef.js, encode(data, encodeDefaults)!!, json("mergeFields" to mergeFields)) }
            .let { this }

    actual inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        rethrow { js.set(documentRef.js, encode(data, encodeDefaults)!!, json("mergeFields" to mergeFieldPaths.map { it.js }.toTypedArray())) }
            .let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean) =
        rethrow { js.set(documentRef.js, encode(strategy, data, encodeDefaults)!!, json("merge" to merge)) }
            .let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        rethrow { js.set(documentRef.js, encode(strategy, data, encodeDefaults)!!, json("mergeFields" to mergeFields)) }
            .let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        rethrow { js.set(documentRef.js, encode(strategy, data, encodeDefaults)!!, json("mergeFields" to mergeFieldPaths.map { it.js }.toTypedArray())) }
            .let { this }

    actual inline fun <reified T> update(documentRef: DocumentReference, data: T, encodeDefaults: Boolean) =
        rethrow { js.update(documentRef.js, encode(data, encodeDefaults)!!) }
            .let { this }

    actual fun <T> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        rethrow { js.update(documentRef.js, encode(strategy, data, encodeDefaults)!!) }
            .let { this }

    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>) = rethrow {
        js.takeUnless { fieldsAndValues.isEmpty() }
            ?.update(
                documentRef.js,
                fieldsAndValues[0].first,
                fieldsAndValues[0].second,
                *fieldsAndValues.drop(1).flatMap { (field, value) ->
                    listOf(field, value?.let { encode(value, true) })
                }.toTypedArray()
            )
    }.let { this }

    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>) = rethrow {
        js.takeUnless { fieldsAndValues.isEmpty() }
            ?.update(
                documentRef.js,
                fieldsAndValues[0].first.js,
                fieldsAndValues[0].second,
                *fieldsAndValues.flatMap { (field, value) ->
                    listOf(field.js, value?.let { encode(value, true) })
                }.toTypedArray()
            )
    }.let { this }

    actual fun delete(documentRef: DocumentReference) =
        rethrow { js.delete(documentRef.js) }
            .let { this }

    actual suspend fun commit() = rethrow { js.commit().await() }

}

actual class Transaction(val js: firebase.firestore.Transaction) {

    actual fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, merge: Boolean) =
        rethrow { js.set(documentRef.js, encode(data, encodeDefaults)!!, json("merge" to merge)) }
            .let { this }

    actual fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, vararg mergeFields: String) =
        rethrow { js.set(documentRef.js, encode(data, encodeDefaults)!!, json("mergeFields" to mergeFields)) }
            .let { this }

    actual fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        rethrow { js.set(documentRef.js, encode(data, encodeDefaults)!!, json("mergeFields" to mergeFieldPaths.map { it.js }.toTypedArray())) }
            .let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean) =
        rethrow { js.set(documentRef.js, encode(strategy, data, encodeDefaults)!!, json("merge" to merge)) }
            .let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        rethrow { js.set(documentRef.js, encode(strategy, data, encodeDefaults)!!, json("mergeFields" to mergeFields)) }
            .let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        rethrow { js.set(documentRef.js, encode(strategy, data, encodeDefaults)!!, json("mergeFields" to mergeFieldPaths.map { it.js }.toTypedArray())) }
            .let { this }

    actual fun update(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean) =
        rethrow { js.update(documentRef.js, encode(data, encodeDefaults)!!) }
            .let { this }

    actual fun <T> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        rethrow { js.update(documentRef.js, encode(strategy, data, encodeDefaults)!!) }
            .let { this }

    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>) = rethrow {
        js.takeUnless { fieldsAndValues.isEmpty() }
            ?.update(
                documentRef.js,
                fieldsAndValues[0].first,
                fieldsAndValues[0].second,
                *fieldsAndValues.drop(1).flatMap { (field, value) ->
                    listOf(field, value?.let { encode(it, true) })
                }.toTypedArray()
            )
    }.let { this }

    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>) = rethrow {
        js.takeUnless { fieldsAndValues.isEmpty() }
            ?.update(
                documentRef.js,
                fieldsAndValues[0].first.js,
                fieldsAndValues[0].second,
                *fieldsAndValues.flatMap { (field, value) ->
                    listOf(field.js, value?.let { encode(it, true)!! })
                }.toTypedArray()
            )
    }.let { this }

    actual fun delete(documentRef: DocumentReference) =
        rethrow { js.delete(documentRef.js) }
            .let { this }

    actual suspend fun get(documentRef: DocumentReference) =
        rethrow { DocumentSnapshot(js.get(documentRef.js).await()) }
}

actual class DocumentReference(val js: firebase.firestore.DocumentReference) {

    actual val id: String
        get() = rethrow { js.id }

    actual val path: String
        get() = rethrow { js.path }

    actual fun collection(collectionPath: String) = rethrow { CollectionReference(js.collection(collectionPath)) }

    actual suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean, merge: Boolean) =
        rethrow { js.set(encode(data, encodeDefaults)!!, json("merge" to merge)).await() }

    actual suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        rethrow { js.set(encode(data, encodeDefaults)!!, json("mergeFields" to mergeFields)).await() }

    actual suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        rethrow { js.set(encode(data, encodeDefaults)!!, json("mergeFields" to mergeFieldPaths.map { it.js }.toTypedArray())).await() }

    actual suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean) =
        rethrow { js.set(encode(strategy, data, encodeDefaults)!!, json("merge" to merge)).await() }

    actual suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        rethrow { js.set(encode(strategy, data, encodeDefaults)!!, json("mergeFields" to mergeFields)).await() }

    actual suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        rethrow { js.set(encode(strategy, data, encodeDefaults)!!, json("mergeFields" to mergeFieldPaths.map { it.js }.toTypedArray())).await() }

    actual suspend inline fun <reified T> update(data: T, encodeDefaults: Boolean) =
        rethrow { js.update(encode(data, encodeDefaults)!!).await() }

    actual suspend fun <T> update(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        rethrow { js.update(encode(strategy, data, encodeDefaults)!!).await() }

    actual suspend fun update(vararg fieldsAndValues: Pair<String, Any?>) = rethrow {
        js.takeUnless { fieldsAndValues.isEmpty() }
            ?.update(
                fieldsAndValues[0].first,
                fieldsAndValues[0].second,
                *fieldsAndValues.drop(1).flatMap { (field, value) ->
                    listOf(field, value?.let { encode(it, true) })
                }.toTypedArray()
            )
            ?.await()
    }.run { Unit }

    actual suspend fun update(vararg fieldsAndValues: Pair<FieldPath, Any?>) = rethrow {
        js.takeUnless { fieldsAndValues.isEmpty() }
            ?.update(
                fieldsAndValues[0].first.js,
                fieldsAndValues[0].second,
                *fieldsAndValues.flatMap { (field, value) ->
                    listOf(field.js, value?.let { encode(it, true)!! })
                }.toTypedArray()
            )
            ?.await()
    }.run { Unit }

    actual suspend fun delete() = rethrow { js.delete().await() }

    actual suspend fun get() = rethrow { DocumentSnapshot(js.get().await()) }

    actual val snapshots get() = callbackFlow<DocumentSnapshot> {
        val unsubscribe = js.onSnapshot(
            { safeOffer(DocumentSnapshot(it)) },
            { close(errorToException(it)) }
        )
        awaitClose { unsubscribe() }
    }
}

actual open class Query(open val js: firebase.firestore.Query) {

    actual suspend fun get() =  rethrow { QuerySnapshot(js.get().await()) }

    actual fun limit(limit: Number) = Query(js.limit(limit.toDouble()))

    internal actual fun _where(field: String, equalTo: Any?) = rethrow { Query(js.where(field, "==", equalTo)) }
    internal actual fun _where(path: FieldPath, equalTo: Any?) = rethrow { Query(js.where(path.js, "==", equalTo)) }

    internal actual fun _where(field: String, lessThan: Any?, greaterThan: Any?, arrayContains: Any?) = rethrow {
        Query(
            (lessThan?.let {js.where(field, "<", it) } ?: js).let { js2 ->
                (greaterThan?.let { js2.where(field, ">", it) } ?: js2).let { js3 ->
                    arrayContains?.let { js3.where(field, "array-contains", it) } ?: js3
                }
            }
        )
    }

    internal actual fun _where(path: FieldPath, lessThan: Any?, greaterThan: Any?, arrayContains: Any?) = rethrow {
        Query(
            (lessThan?.let {js.where(path.js, "<", it) } ?: js).let { js2 ->
                (greaterThan?.let { js2.where(path.js, ">", it) } ?: js2).let { js3 ->
                    arrayContains?.let { js3.where(path.js, "array-contains", it) } ?: js3
                }
            }
        )
    }

    internal actual fun _where(field: String, inArray: List<Any>?, arrayContainsAny: List<Any>?) = Query(
        (inArray?.let { js.where(field, "in", it.toTypedArray()) } ?: js).let { js2 ->
            arrayContainsAny?.let { js2.where(field, "array-contains-any", it.toTypedArray()) } ?: js2
        }
    )

    internal actual fun _where(path: FieldPath, inArray: List<Any>?, arrayContainsAny: List<Any>?) = Query(
        (inArray?.let { js.where(path.js, "in", it.toTypedArray()) } ?: js).let { js2 ->
            arrayContainsAny?.let { js2.where(path.js, "array-contains-any", it.toTypedArray()) } ?: js2
        }
    )

    internal actual fun _orderBy(field: String, direction: Direction) = rethrow {
        Query(js.orderBy(field, direction.jsString))
    }

    internal actual fun _orderBy(field: FieldPath, direction: Direction) = rethrow {
        Query(js.orderBy(field.js, direction.jsString))
    }

    actual val snapshots get() = callbackFlow<QuerySnapshot> {
        val unsubscribe = rethrow {
            js.onSnapshot(
                { safeOffer(QuerySnapshot(it)) },
                { close(errorToException(it)) }
            )
        }
        awaitClose { rethrow { unsubscribe() } }
    }
}

actual class CollectionReference(override val js: firebase.firestore.CollectionReference) : Query(js) {

    actual val path: String
        get() =  rethrow { js.path }

    actual fun document(documentPath: String) = rethrow { DocumentReference(js.doc(documentPath)) }

    actual suspend inline fun <reified T> add(data: T, encodeDefaults: Boolean) =
        rethrow { DocumentReference(js.add(encode(data, encodeDefaults)!!).await()) }

    actual suspend fun <T> add(data: T, strategy: SerializationStrategy<T>, encodeDefaults: Boolean) =
        rethrow { DocumentReference(js.add(encode(strategy, data, encodeDefaults)!!).await()) }
    actual suspend fun <T> add(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        rethrow { DocumentReference(js.add(encode(strategy, data, encodeDefaults)!!).await()) }
}

actual class FirebaseFirestoreException(cause: Throwable, val code: FirestoreExceptionCode) : FirebaseException(code.toString(), cause)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual val FirebaseFirestoreException.code: FirestoreExceptionCode get() = code

actual class QuerySnapshot(val js: firebase.firestore.QuerySnapshot) {
    actual val documents
        get() = js.docs.map { DocumentSnapshot(it) }
    actual val documentChanges
        get() = js.docChanges().map { DocumentChange(it) }
    actual val metadata: SnapshotMetadata get() = SnapshotMetadata(js.metadata)
}

actual class DocumentChange(val js: firebase.firestore.DocumentChange) {
    actual val document: DocumentSnapshot
        get() = DocumentSnapshot(js.doc)
    actual val newIndex: Int
        get() = js.newIndex
    actual val oldIndex: Int
        get() = js.oldIndex
    actual val type: ChangeType
        get() = ChangeType.values().first { it.jsString == js.type }
}

actual class DocumentSnapshot(val js: firebase.firestore.DocumentSnapshot) {

    actual val id get() = rethrow { js.id }
    actual val reference get() = rethrow { DocumentReference(js.ref) }

    actual inline fun <reified T : Any> data(): T =
        rethrow { decode<T>(value = js.data()) }

    actual fun <T> data(strategy: DeserializationStrategy<T>): T =
        rethrow { decode(strategy, js.data()) }

    actual inline fun <reified T> get(field: String) =
        rethrow { decode<T>(value = js.get(field)) }

    actual fun <T> get(field: String, strategy: DeserializationStrategy<T>) =
        rethrow { decode(strategy, js.get(field)) }

    actual fun contains(field: String) = rethrow { js.get(field) != undefined }
    actual val exists get() = rethrow { js.exists }
    actual val metadata: SnapshotMetadata get() = SnapshotMetadata(js.metadata)
}

actual class SnapshotMetadata(val js: firebase.firestore.SnapshotMetadata) {
    actual val hasPendingWrites: Boolean get() = js.hasPendingWrites
    actual val isFromCache: Boolean get() = js.fromCache
}

actual class FieldPath private constructor(val js: firebase.firestore.FieldPath) {
    actual constructor(vararg fieldNames: String) : this(dev.gitlive.firebase.firestore.rethrow {
        js("Reflect").construct(firebase.firestore.FieldPath, fieldNames).unsafeCast<firebase.firestore.FieldPath>()
    })
    actual val documentId: FieldPath get() = FieldPath(firebase.firestore.FieldPath.documentId)
}

actual object FieldValue {
    actual val serverTimestamp = Double.POSITIVE_INFINITY
    actual val delete: Any get() = rethrow { firebase.firestore.FieldValue.delete() }
    actual fun arrayUnion(vararg elements: Any): Any = rethrow { firebase.firestore.FieldValue.arrayUnion(*elements) }
    actual fun arrayRemove(vararg elements: Any): Any = rethrow { firebase.firestore.FieldValue.arrayRemove(*elements) }
    @JsName("deprecatedDelete")
    actual fun delete(): Any = delete
}

//actual data class FirebaseFirestoreSettings internal constructor(
//    val cacheSizeBytes: Number? = undefined,
//    val host: String? = undefined,
//    val ssl: Boolean? = undefined,
//    var timestampsInSnapshots: Boolean? = undefined,
//    var enablePersistence: Boolean = false
//)

actual enum class FirestoreExceptionCode {
    OK,
    CANCELLED,
    UNKNOWN,
    INVALID_ARGUMENT,
    DEADLINE_EXCEEDED,
    NOT_FOUND,
    ALREADY_EXISTS,
    PERMISSION_DENIED,
    RESOURCE_EXHAUSTED,
    FAILED_PRECONDITION,
    ABORTED,
    OUT_OF_RANGE,
    UNIMPLEMENTED,
    INTERNAL,
    UNAVAILABLE,
    DATA_LOSS,
    UNAUTHENTICATED
}

actual enum class Direction(internal val jsString : String) {
    ASCENDING("asc"),
    DESCENDING("desc");
}

actual enum class ChangeType(internal val jsString : String) {
    ADDED("added"),
    MODIFIED("modified"),
    REMOVED("removed");
}

inline fun <T, R> T.rethrow(function: T.() -> R): R = dev.gitlive.firebase.firestore.rethrow { function() }

inline fun <R> rethrow(function: () -> R): R {
    try {
        return function()
    } catch (e: Exception) {
        throw e
    } catch(e: dynamic) {
        throw errorToException(e)
    }
}

fun errorToException(e: dynamic) = when(e?.code?.toString()?.toLowerCase()) {
    "cancelled" -> FirebaseFirestoreException(e, FirestoreExceptionCode.CANCELLED)
    "invalid-argument" -> FirebaseFirestoreException(e, FirestoreExceptionCode.INVALID_ARGUMENT)
    "deadline-exceeded" -> FirebaseFirestoreException(e, FirestoreExceptionCode.DEADLINE_EXCEEDED)
    "not-found" -> FirebaseFirestoreException(e, FirestoreExceptionCode.NOT_FOUND)
    "already-exists" -> FirebaseFirestoreException(e, FirestoreExceptionCode.ALREADY_EXISTS)
    "permission-denied" -> FirebaseFirestoreException(e, FirestoreExceptionCode.PERMISSION_DENIED)
    "resource-exhausted" -> FirebaseFirestoreException(e, FirestoreExceptionCode.RESOURCE_EXHAUSTED)
    "failed-precondition" -> FirebaseFirestoreException(e, FirestoreExceptionCode.FAILED_PRECONDITION)
    "aborted" -> FirebaseFirestoreException(e, FirestoreExceptionCode.ABORTED)
    "out-of-range" -> FirebaseFirestoreException(e, FirestoreExceptionCode.OUT_OF_RANGE)
    "unimplemented" -> FirebaseFirestoreException(e, FirestoreExceptionCode.UNIMPLEMENTED)
    "internal" -> FirebaseFirestoreException(e, FirestoreExceptionCode.INTERNAL)
    "unavailable" -> FirebaseFirestoreException(e, FirestoreExceptionCode.UNAVAILABLE)
    "data-loss" -> FirebaseFirestoreException(e, FirestoreExceptionCode.DATA_LOSS)
    "unauthenticated" -> FirebaseFirestoreException(e, FirestoreExceptionCode.UNAUTHENTICATED)
    "unknown" -> FirebaseFirestoreException(e, FirestoreExceptionCode.UNKNOWN)
    else -> {
        println("Unknown error code in ${JSON.stringify(e)}")
        FirebaseFirestoreException(e, FirestoreExceptionCode.UNKNOWN)
    }
}
