/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import android.net.Uri
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

actual class FirebaseUser internal constructor(val android: com.google.firebase.auth.FirebaseUser) {
    actual val uid: String
        get() = android.uid
    actual val displayName: String?
        get() = android.displayName
    actual val email: String?
        get() = android.email
    actual val phoneNumber: String?
        get() = android.phoneNumber
    actual val photoURL: String?
        get() = android.photoUrl?.toString()
    actual val isAnonymous: Boolean
        get() = android.isAnonymous
    actual val isEmailVerified: Boolean
        get() = android.isEmailVerified
    actual val metaData: UserMetaData?
        get() = android.metadata?.let{ UserMetaData(it) }
    actual val multiFactor: MultiFactor
        get() = MultiFactor(android.multiFactor)
    actual val providerData: List<UserInfo>
        get() = android.providerData.map { UserInfo(it) }
    actual val providerId: String
        get() = android.providerId
    actual suspend fun delete() = android.delete().await().run { Unit }
    actual suspend fun reload() = android.reload().await().run { Unit }
    actual suspend fun getIdToken(forceRefresh: Boolean): String? = android.getIdToken(forceRefresh).await().token
    actual suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult = android.getIdToken(forceRefresh).await().run { AuthTokenResult(this) }
    actual suspend fun linkWithCredential(credential: AuthCredential): AuthResult = AuthResult(android.linkWithCredential(credential.android).await())
    actual suspend fun reauthenticate(credential: AuthCredential) = android.reauthenticate(credential.android).await().run { Unit }
    actual suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult = AuthResult(android.reauthenticateAndRetrieveData(credential.android).await())
    actual suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?) {
        val request = actionCodeSettings?.let { android.sendEmailVerification(it.toAndroid()) } ?: android.sendEmailVerification()
        request.await()
    }
    actual suspend fun unlink(provider: String): FirebaseUser? = android.unlink(provider).await().user?.let { FirebaseUser(it) }
    actual suspend fun updateEmail(email: String) = android.updateEmail(email).await().run { Unit }
    actual suspend fun updatePassword(password: String) = android.updatePassword(password).await().run { Unit }
    actual suspend fun updatePhoneNumber(credential: PhoneAuthCredential) = android.updatePhoneNumber(credential.android).await().run { Unit }
    actual suspend fun updateProfile(displayName: String?, photoUrl: String?) {
        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .setPhotoUri(photoUrl?.let { Uri.parse(it) })
            .build()
        android.updateProfile(request).await()
    }
    actual suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?) =
        android.verifyBeforeUpdateEmail(newEmail, actionCodeSettings?.toAndroid()).await().run { Unit }
}

actual class UserInfo(val android: com.google.firebase.auth.UserInfo) {
    actual val displayName: String?
        get() = android.displayName
    actual val email: String?
        get() = android.email
    actual val phoneNumber: String?
        get() = android.phoneNumber
    actual val photoURL: String?
        get() = android.photoUrl?.toString()
    actual val providerId: String
        get() = android.providerId
    actual val uid: String
        get() = android.uid
}

actual class UserMetaData(val android: com.google.firebase.auth.FirebaseUserMetadata) {
    actual val creationTime: Double?
        get() = android.creationTimestamp.toDouble()
    actual val lastSignInTime: Double?
        get() = android.lastSignInTimestamp.toDouble()
}
