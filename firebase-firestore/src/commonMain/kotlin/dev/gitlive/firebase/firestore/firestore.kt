/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

/** Returns the [FirebaseFirestore] instance of the default [FirebaseApp]. */
expect val Firebase.firestore: FirebaseFirestore

/** Returns the [FirebaseFirestore] instance of a given [FirebaseApp]. */
expect fun Firebase.firestore(app: FirebaseApp): FirebaseFirestore

expect class FirebaseFirestore {
//    var settings: FirebaseFirestoreSettings
    fun collection(collectionPath: String): CollectionReference
    fun document(documentPath: String): DocumentReference
    fun batch(): WriteBatch
    fun setLoggingEnabled(loggingEnabled: Boolean)
    suspend fun clearPersistence()
    suspend fun <T> runTransaction(func: suspend Transaction.() -> T): T
}

expect class Transaction {

    fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean = true, merge: Boolean = false): Transaction
    fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean = true, vararg mergeFields: String): Transaction
    fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean = true, vararg mergeFieldPaths: FieldPath): Transaction

    fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true, merge: Boolean = false): Transaction
    fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true, vararg mergeFields: String): Transaction
    fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true, vararg mergeFieldPaths: FieldPath): Transaction

    fun update(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean = true): Transaction
    fun <T> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true): Transaction

    fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>): Transaction
    fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>): Transaction

    fun delete(documentRef: DocumentReference): Transaction
    suspend fun get(documentRef: DocumentReference): DocumentSnapshot
}

//data class FirebaseFirestoreSettings(
//    val persistenceEnabled: Boolean = true
//)

expect open class Query {
    fun limit(limit: Number): Query
    val snapshots: Flow<QuerySnapshot>
    suspend fun get(): QuerySnapshot
    internal fun _where(field: String, equalTo: Any?): Query
    internal fun _where(path: FieldPath, equalTo: Any?): Query
    internal fun _where(field: String, lessThan: Any? = null, greaterThan: Any? = null, arrayContains: Any? = null): Query
    internal fun _where(path: FieldPath, lessThan: Any? = null, greaterThan: Any? = null, arrayContains: Any? = null): Query
    internal fun _where(field: String, inArray: List<Any>? = null, arrayContainsAny: List<Any>? = null): Query
    internal fun _where(path: FieldPath, inArray: List<Any>? = null, arrayContainsAny: List<Any>? = null): Query
}

fun Query.where(field: String, equalTo: Any?) = _where(field, equalTo)
fun Query.where(path: FieldPath, equalTo: Any?) = _where(path, equalTo)
fun Query.where(field: String, lessThan: Any? = null, greaterThan: Any? = null, arrayContains: Any? = null) = _where(field, lessThan, greaterThan, arrayContains)
fun Query.where(path: FieldPath, lessThan: Any? = null, greaterThan: Any? = null, arrayContains: Any? = null) = _where(path, lessThan, greaterThan, arrayContains)
fun Query.where(field: String, inArray: List<Any>? = null, arrayContainsAny: List<Any>? = null) = _where(field, inArray, arrayContainsAny)
fun Query.where(path: FieldPath, inArray: List<Any>? = null, arrayContainsAny: List<Any>? = null) = _where(path, inArray, arrayContainsAny)


expect class WriteBatch {
    inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean = true, merge: Boolean = false): WriteBatch
    inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean = true, vararg mergeFields: String): WriteBatch
    inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean = true, vararg mergeFieldPaths: FieldPath): WriteBatch

    fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true, merge: Boolean = false): WriteBatch
    fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true, vararg mergeFields: String): WriteBatch
    fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true, vararg mergeFieldPaths: FieldPath): WriteBatch

    inline fun <reified T> update(documentRef: DocumentReference, data: T, encodeDefaults: Boolean = true): WriteBatch
    fun <T> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true): WriteBatch

    fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>): WriteBatch
    fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>): WriteBatch

    fun delete(documentRef: DocumentReference): WriteBatch
    suspend fun commit()
}

expect class DocumentReference {

    val id: String
    val path: String
    val snapshots: Flow<DocumentSnapshot>
    suspend fun get(): DocumentSnapshot

    suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean = true, merge: Boolean = false)
    suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean = true, vararg mergeFields: String)
    suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean = true, vararg mergeFieldPaths: FieldPath)

    suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true, merge: Boolean = false)
    suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true, vararg mergeFields: String)
    suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true, vararg mergeFieldPaths: FieldPath)

    suspend inline fun <reified T> update(data: T, encodeDefaults: Boolean = true)
    suspend fun <T> update(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true)

    suspend fun update(vararg fieldsAndValues: Pair<String, Any?>)
    suspend fun update(vararg fieldsAndValues: Pair<FieldPath, Any?>)

    suspend fun delete()
}

expect class CollectionReference : Query {
    val path: String
    suspend inline fun <reified T> add(data: T, encodeDefaults: Boolean = true): DocumentReference
    suspend fun <T> add(data: T, strategy: SerializationStrategy<T>, encodeDefaults: Boolean = true): DocumentReference
}

expect class FirebaseFirestoreException : FirebaseException

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
expect val FirebaseFirestoreException.code: FirestoreExceptionCode

expect enum class FirestoreExceptionCode {
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

expect class QuerySnapshot {
    val documents: List<DocumentSnapshot>
}

expect class DocumentSnapshot {

    inline fun <reified T> get(field: String): T
    fun <T> get(field: String, strategy: DeserializationStrategy<T>): T

    fun contains(field: String): Boolean

    inline fun <reified T: Any> data(): T
    fun <T> data(strategy: DeserializationStrategy<T>): T

    val exists: Boolean
    val id: String
    val reference: DocumentReference
}

expect class FieldPath

expect fun FieldPath(vararg fieldNames: String): FieldPath

expect object FieldValue {
    fun delete(): Any
    fun arrayUnion(vararg elements: Any): Any
    fun arrayRemove(vararg elements: Any): Any
}


