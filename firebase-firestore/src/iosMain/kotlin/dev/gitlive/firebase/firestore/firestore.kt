/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore

import cocoapods.FirebaseFirestore.*
import cocoapods.FirebaseFirestore.FIRDocumentChangeType.*
import dev.gitlive.firebase.*
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import platform.Foundation.NSError

actual val Firebase.firestore get() =
    FirebaseFirestore(FIRFirestore.firestore())

actual fun Firebase.firestore(app: FirebaseApp): FirebaseFirestore {
    return FirebaseFirestore(FIRFirestore.firestoreForApp(app.ios))
}

@Suppress("UNCHECKED_CAST")
actual class FirebaseFirestore(val ios: FIRFirestore) {

    actual fun collection(collectionPath: String) = CollectionReference(ios.collectionWithPath(collectionPath))

    actual fun document(documentPath: String) = DocumentReference(ios.documentWithPath(documentPath))

    actual fun collectionGroup(collectionId: String) = Query(ios.collectionGroupWithID(collectionId))

    actual fun batch() = WriteBatch(ios.batch())

    actual fun setLoggingEnabled(loggingEnabled: Boolean): Unit =
        FIRFirestore.enableLogging(loggingEnabled)

    actual suspend fun <T> runTransaction(func: suspend Transaction.() -> T) =
        awaitResult<Any?> { ios.runTransactionWithBlock({ transaction, error -> runBlocking { Transaction(transaction!!).func() } }, it) } as T

    actual suspend fun clearPersistence() =
        await { ios.clearPersistenceWithCompletion(it) }

    actual fun useEmulator(host: String, port: Int) {
        ios.settings = ios.settings.apply {
            this.host = "$host:$port"
            persistenceEnabled = false
            sslEnabled = false
        }
    }
}

@Suppress("UNCHECKED_CAST")
actual class WriteBatch(val ios: FIRWriteBatch) {

    actual inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, merge: Boolean) =
        ios.setData(encode(data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, merge).let { this }

    actual inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        ios.setData(encode(data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, mergeFields.asList()).let { this }

    actual inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        ios.setData(encode(data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, mergeFieldPaths.map { it.ios }).let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean) =
        ios.setData(encode(strategy, data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, merge).let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        ios.setData(encode(strategy, data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, mergeFields.asList()).let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        ios.setData(encode(strategy, data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, mergeFieldPaths.map { it.ios }).let { this }

    actual inline fun <reified T> update(documentRef: DocumentReference, data: T, encodeDefaults: Boolean) =
        ios.updateData(encode(data, encodeDefaults) as Map<Any?, *>, documentRef.ios).let { this }

    actual fun <T> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        ios.updateData(encode(strategy, data, encodeDefaults) as Map<Any?, *>, documentRef.ios).let { this }

    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>) =
            ios.updateData(fieldsAndValues.associate { it }, documentRef.ios).let { this }

    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>) =
        ios.updateData(fieldsAndValues.associate { (path, value) -> path.ios to value }, documentRef.ios).let { this }

    actual fun delete(documentRef: DocumentReference) =
        ios.deleteDocument(documentRef.ios).let { this }

    actual suspend fun commit() = await { ios.commitWithCompletion(it) }

}

@Suppress("UNCHECKED_CAST")
actual class Transaction(val ios: FIRTransaction) {

    actual fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, merge: Boolean) =
        ios.setData(encode(data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, merge).let { this }

    actual fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, vararg mergeFields: String) =
        ios.setData(encode(data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, mergeFields.asList()).let { this }

    actual fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        ios.setData(encode(data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, mergeFieldPaths.map { it.ios }).let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean) =
        ios.setData(encode(strategy, data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, merge).let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        ios.setData(encode(strategy, data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, mergeFields.asList()).let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        ios.setData(encode(strategy, data, encodeDefaults)!! as Map<Any?, *>, documentRef.ios, mergeFieldPaths.map { it.ios }).let { this }

    actual fun update(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean) =
        ios.updateData(encode(data, encodeDefaults) as Map<Any?, *>, documentRef.ios).let { this }

    actual fun <T> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        ios.updateData(encode(strategy, data, encodeDefaults) as Map<Any?, *>, documentRef.ios).let { this }

    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>) =
        ios.updateData(fieldsAndValues.associate { it }, documentRef.ios).let { this }

    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>) =
        ios.updateData(fieldsAndValues.associate { (path, value) -> path.ios to value }, documentRef.ios).let { this }

    actual fun delete(documentRef: DocumentReference) =
        ios.deleteDocument(documentRef.ios).let { this }

    actual suspend fun get(documentRef: DocumentReference) =
        throwError { DocumentSnapshot(ios.getDocument(documentRef.ios, it)!!) }

}

@Suppress("UNCHECKED_CAST")
actual class DocumentReference(val ios: FIRDocumentReference) {

    actual val id: String
        get() = ios.documentID

    actual val path: String
        get() = ios.path

    actual fun collection(collectionPath: String) = CollectionReference(ios.collectionWithPath(collectionPath))

    actual suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean, merge: Boolean) =
        await { ios.setData(encode(data, encodeDefaults)!! as Map<Any?, *>, merge, it) }

    actual suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        await { ios.setData(encode(data, encodeDefaults)!! as Map<Any?, *>, mergeFields.asList(), it) }

    actual suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        await { ios.setData(encode(data, encodeDefaults)!! as Map<Any?, *>, mergeFieldPaths.map { it.ios }, it) }

    actual suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean) =
        await { ios.setData(encode(strategy, data, encodeDefaults)!! as Map<Any?, *>, merge, it) }

    actual suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        await { ios.setData(encode(strategy, data, encodeDefaults)!! as Map<Any?, *>, mergeFields.asList(), it) }

    actual suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        await { ios.setData(encode(strategy, data, encodeDefaults)!! as Map<Any?, *>, mergeFieldPaths.map { it.ios }, it) }

    actual suspend inline fun <reified T> update(data: T, encodeDefaults: Boolean) =
        await { ios.updateData(encode(data, encodeDefaults) as Map<Any?, *>, it) }

    actual suspend fun <T> update(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        await { ios.updateData(encode(strategy, data, encodeDefaults) as Map<Any?, *>, it) }

    actual suspend fun update(vararg fieldsAndValues: Pair<String, Any?>) =
        await { block -> ios.updateData(fieldsAndValues.associate { it }, block) }

    actual suspend fun update(vararg fieldsAndValues: Pair<FieldPath, Any?>) =
        await { block -> ios.updateData(fieldsAndValues.associate { (path, value) -> path.ios to value }, block) }

    actual suspend fun delete() =
        await { ios.deleteDocumentWithCompletion(it) }

    actual suspend fun get() =
        DocumentSnapshot(awaitResult { ios.getDocumentWithCompletion(it) })

    actual val snapshots get() = callbackFlow<DocumentSnapshot> {
        val listener = ios.addSnapshotListener { snapshot, error ->
            snapshot?.let { safeOffer(DocumentSnapshot(snapshot)) }
            error?.let { close(error.toException()) }
        }
        awaitClose { listener.remove() }
    }
}

actual open class Query(open val ios: FIRQuery) {

    actual suspend fun get() = QuerySnapshot(awaitResult { ios.getDocumentsWithCompletion(it) })

    actual fun limit(limit: Number) = Query(ios.queryLimitedTo(limit.toLong()))

    actual val snapshots get() = callbackFlow<QuerySnapshot> {
        val listener = ios.addSnapshotListener { snapshot, error ->
            snapshot?.let { safeOffer(QuerySnapshot(snapshot)) }
            error?.let { close(error.toException()) }
        }
        awaitClose { listener.remove() }
    }

    internal actual fun _where(field: String, equalTo: Any?) = Query(ios.queryWhereField(field, isEqualTo = equalTo!!))
    internal actual fun _where(path: FieldPath, equalTo: Any?) = Query(ios.queryWhereFieldPath(path.ios, isEqualTo = equalTo!!))

    internal actual fun _where(field: String, lessThan: Any?, greaterThan: Any?, arrayContains: Any?) = Query(
        (lessThan?.let { ios.queryWhereField(field, isLessThan = it) } ?: ios).let { ios2 ->
            (greaterThan?.let { ios2.queryWhereField(field, isGreaterThan = it) } ?: ios2).let { ios3 ->
                arrayContains?.let { ios3.queryWhereField(field, arrayContains = it) } ?: ios3
            }
        }
    )

    internal actual fun _where(path: FieldPath, lessThan: Any?, greaterThan: Any?, arrayContains: Any?) = Query(
        (lessThan?.let { ios.queryWhereFieldPath(path.ios, isLessThan = it) } ?: ios).let { ios2 ->
            (greaterThan?.let { ios2.queryWhereFieldPath(path.ios, isGreaterThan = it) } ?: ios2).let { ios3 ->
                arrayContains?.let { ios3.queryWhereFieldPath(path.ios, arrayContains = it) } ?: ios3
            }
        }
    )

    internal actual fun _where(field: String, inArray: List<Any>?, arrayContainsAny: List<Any>?) = Query(
        (inArray?.let { ios.queryWhereField(field, `in` = it) } ?: ios).let { ios2 ->
            arrayContainsAny?.let { ios2.queryWhereField(field, arrayContainsAny = arrayContainsAny) } ?: ios2
        }
    )

    internal actual fun _where(path: FieldPath, inArray: List<Any>?, arrayContainsAny: List<Any>?) = Query(
        (inArray?.let { ios.queryWhereFieldPath(path.ios, `in` = it) } ?: ios).let { ios2 ->
            arrayContainsAny?.let { ios2.queryWhereFieldPath(path.ios, arrayContainsAny = arrayContainsAny) } ?: ios2
        }
    )

    internal actual fun _orderBy(field: String, direction: Direction) = Query(ios.queryOrderedByField(field, direction == Direction.DESCENDING))
    internal actual fun _orderBy(field: FieldPath, direction: Direction) = Query(ios.queryOrderedByFieldPath(field.ios, direction == Direction.DESCENDING))
}

@Suppress("UNCHECKED_CAST")
actual class CollectionReference(override val ios: FIRCollectionReference) : Query(ios) {

    actual val path: String
        get() = ios.path

    actual fun document(documentPath: String) = DocumentReference(ios.documentWithPath(documentPath))

    actual suspend inline fun <reified T> add(data: T, encodeDefaults: Boolean) =
        DocumentReference(await { ios.addDocumentWithData(encode(data, encodeDefaults) as Map<Any?, *>, it) })

    actual suspend fun <T> add(data: T, strategy: SerializationStrategy<T>, encodeDefaults: Boolean) =
        DocumentReference(await { ios.addDocumentWithData(encode(strategy, data, encodeDefaults) as Map<Any?, *>) })
    actual suspend fun <T> add(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        DocumentReference(await { ios.addDocumentWithData(encode(strategy, data, encodeDefaults) as Map<Any?, *>) })
}

actual class FirebaseFirestoreException(message: String, val code: FirestoreExceptionCode) : FirebaseException(message)

actual val FirebaseFirestoreException.code: FirestoreExceptionCode get() = code

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

actual enum class Direction {
    ASCENDING,
    DESCENDING
}

actual enum class ChangeType(internal val ios: FIRDocumentChangeType) {
    ADDED(FIRDocumentChangeTypeAdded),
    MODIFIED(FIRDocumentChangeTypeModified),
    REMOVED(FIRDocumentChangeTypeRemoved)
}

fun NSError.toException() = when(domain) {
    FIRFirestoreErrorDomain -> when(code) {
        FIRFirestoreErrorCodeOK -> FirestoreExceptionCode.OK
        FIRFirestoreErrorCodeCancelled -> FirestoreExceptionCode.CANCELLED
        FIRFirestoreErrorCodeUnknown -> FirestoreExceptionCode.UNKNOWN
        FIRFirestoreErrorCodeInvalidArgument -> FirestoreExceptionCode.INVALID_ARGUMENT
        FIRFirestoreErrorCodeDeadlineExceeded -> FirestoreExceptionCode.DEADLINE_EXCEEDED
        FIRFirestoreErrorCodeNotFound -> FirestoreExceptionCode.NOT_FOUND
        FIRFirestoreErrorCodeAlreadyExists -> FirestoreExceptionCode.ALREADY_EXISTS
        FIRFirestoreErrorCodePermissionDenied -> FirestoreExceptionCode.PERMISSION_DENIED
        FIRFirestoreErrorCodeResourceExhausted -> FirestoreExceptionCode.RESOURCE_EXHAUSTED
        FIRFirestoreErrorCodeFailedPrecondition -> FirestoreExceptionCode.FAILED_PRECONDITION
        FIRFirestoreErrorCodeAborted -> FirestoreExceptionCode.ABORTED
        FIRFirestoreErrorCodeOutOfRange -> FirestoreExceptionCode.OUT_OF_RANGE
        FIRFirestoreErrorCodeUnimplemented -> FirestoreExceptionCode.UNIMPLEMENTED
        FIRFirestoreErrorCodeInternal -> FirestoreExceptionCode.INTERNAL
        FIRFirestoreErrorCodeUnavailable -> FirestoreExceptionCode.UNAVAILABLE
        FIRFirestoreErrorCodeDataLoss -> FirestoreExceptionCode.DATA_LOSS
        FIRFirestoreErrorCodeUnauthenticated -> FirestoreExceptionCode.UNAUTHENTICATED
        else -> FirestoreExceptionCode.UNKNOWN
    }
    else -> FirestoreExceptionCode.UNKNOWN
}.let { FirebaseFirestoreException(description!!, it) }

actual class QuerySnapshot(val ios: FIRQuerySnapshot) {
    actual val documentChanges
        get() = ios.documentChanges.map { DocumentChange(it as FIRDocumentChange) }
    actual val metadata: SnapshotMetadata get() = SnapshotMetadata(ios.metadata)
}

actual class DocumentChange(val ios: FIRDocumentChange) {
    actual val document: DocumentSnapshot
        get() = DocumentSnapshot(ios.document)
    actual val newIndex: Int
        get() = ios.newIndex.toInt()
    actual val oldIndex: Int
        get() = ios.oldIndex.toInt()
    actual val type: ChangeType
        get() = ChangeType.values().first { it.ios == ios.type }
}

@Suppress("UNCHECKED_CAST")
actual class DocumentSnapshot(val ios: FIRDocumentSnapshot) {

    actual val id get() = ios.documentID

    actual val reference get() = DocumentReference(ios.reference)

    actual inline fun <reified T: Any> data() = decode<T>(value = ios.data())

    actual fun <T> data(strategy: DeserializationStrategy<T>) = decode(strategy, ios.data())

    actual inline fun <reified T> get(field: String) = decode<T>(value = ios.valueForField(field))

    actual fun <T> get(field: String, strategy: DeserializationStrategy<T>) =
        decode(strategy, ios.valueForField(field))

    actual fun contains(field: String) = ios.valueForField(field) != null

    actual val exists get() = ios.exists

    actual val metadata: SnapshotMetadata get() = SnapshotMetadata(ios.metadata)
}

actual class SnapshotMetadata(val ios: FIRSnapshotMetadata) {
    actual val hasPendingWrites: Boolean get() = ios.pendingWrites
    actual val isFromCache: Boolean get() = ios.fromCache
}

actual class FieldPath private constructor(val ios: FIRFieldPath) {
    actual constructor(vararg fieldNames: String) : this(FIRFieldPath(fieldNames.asList()))
    actual val documentId: FieldPath get() = FieldPath(FIRFieldPath.documentID())
}

actual object FieldValue {
    actual val delete: Any get() = FIRFieldValue.fieldValueForDelete()
    actual fun arrayUnion(vararg elements: Any): Any = FIRFieldValue.fieldValueForArrayUnion(elements.asList())
    actual fun arrayRemove(vararg elements: Any): Any = FIRFieldValue.fieldValueForArrayUnion(elements.asList())
    actual fun serverTimestamp(): Any = FIRFieldValue.fieldValueForServerTimestamp()
    actual fun delete(): Any = delete
}

private fun <T, R> T.throwError(block: T.(errorPointer: CPointer<ObjCObjectVar<NSError?>>) -> R): R {
    memScoped {
        val errorPointer: CPointer<ObjCObjectVar<NSError?>> = alloc<ObjCObjectVar<NSError?>>().ptr
        val result = block(errorPointer)
        val error: NSError? = errorPointer.pointed.value
        if (error != null) {
            throw error.toException()
        }
        return result
    }
}

suspend inline fun <reified T> awaitResult(function: (callback: (T?, NSError?) -> Unit) -> Unit): T {
    val job = CompletableDeferred<T?>()
    function { result, error ->
         if(error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    return job.await() as T
}

suspend inline fun <T> await(function: (callback: (NSError?) -> Unit) -> T): T {
    val job = CompletableDeferred<Unit>()
    val result = function { error ->
        if(error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    job.await()
    return result
}
