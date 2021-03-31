/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.*
import platform.Foundation.NSURL

actual class FirebaseUser internal constructor(val ios: FIRUser) {
    actual val uid: String
        get() = ios.uid
    actual val displayName: String?
        get() = ios.displayName
    actual val email: String?
        get() = ios.email
    actual val phoneNumber: String?
        get() = ios.phoneNumber
    actual val photoURL: String?
        get() = ios.photoURL?.absoluteString
    actual val isAnonymous: Boolean
        get() = ios.anonymous
    actual val isEmailVerified: Boolean
        get() = ios.emailVerified
    actual val metaData: UserMetaData?
        get() = UserMetaData(ios.metadata)
    actual val multiFactor: MultiFactor
        get() = MultiFactor(ios.multiFactor)
    actual val providerData: List<UserInfo>
        get() = ios.providerData.mapNotNull { provider -> (provider as? FIRUserInfoProtocol)?.let { UserInfo(it) } }
    actual val providerId: String
        get() = ios.providerID

    actual suspend fun delete() = ios.await { deleteWithCompletion(it) }.run { Unit }

    actual suspend fun reload() = ios.await { reloadWithCompletion(it) }.run { Unit }

    actual suspend fun getIdToken(forceRefresh: Boolean): String? =
        ios.awaitResult { getIDTokenForcingRefresh(forceRefresh, it) }

    actual suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult =
        AuthTokenResult(ios.awaitResult { getIDTokenResultForcingRefresh(forceRefresh, it) })

    actual suspend fun linkWithCredential(credential: AuthCredential): AuthResult =
        AuthResult(ios.awaitResult { linkWithCredential(credential.ios, it) })

    actual suspend fun reauthenticate(credential: AuthCredential) =
        ios.awaitResult<FIRUser, FIRAuthDataResult?> { reauthenticateWithCredential(credential.ios, it) }.run { Unit }

    actual suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult =
        AuthResult(ios.awaitResult { reauthenticateWithCredential(credential.ios, it) })

    actual suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?) = ios.await {
        actionCodeSettings?.let { settings -> sendEmailVerificationWithActionCodeSettings(settings.toIos(), it) }
            ?: sendEmailVerificationWithCompletion(it)
    }

    actual suspend fun unlink(provider: String): FirebaseUser? {
        val user: FIRUser? = ios.awaitResult { unlinkFromProvider(provider, it) }
        return user?.let {
            FirebaseUser(it)
        }
    }
    actual suspend fun updateEmail(email: String) = ios.await { updateEmail(email, it) }.run { Unit }
    actual suspend fun updatePassword(password: String) = ios.await { updatePassword(password, it) }.run { Unit }
    actual suspend fun updatePhoneNumber(credential: PhoneAuthCredential) = ios.await { updatePhoneNumberCredential(credential.ios, it) }.run { Unit }
    actual suspend fun updateProfile(displayName: String?, photoUrl: String?) {
        val request = ios.profileChangeRequest().apply {
            this.displayName = displayName
            this.photoURL = photoUrl?.let { NSURL.URLWithString(it) }
        }
        ios.await { request.commitChangesWithCompletion(it) }
    }
    actual suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?) = ios.await {
        actionCodeSettings?.let { actionSettings -> sendEmailVerificationBeforeUpdatingEmail(newEmail, actionSettings.toIos(), it) } ?: sendEmailVerificationBeforeUpdatingEmail(newEmail, it)
    }.run { Unit }
}

actual class UserInfo(val ios: FIRUserInfoProtocol) {
    actual val displayName: String?
        get() = ios.displayName
    actual val email: String?
        get() = ios.email
    actual val phoneNumber: String?
        get() = ios.phoneNumber
    actual val photoURL: String?
        get() = ios.photoURL?.absoluteString
    actual val providerId: String
        get() = ios.providerID
    actual val uid: String
        get() = ios.uid
}

actual class UserMetaData(val ios: FIRUserMetadata) {
    actual val creationTime: Double?
        get() = ios.creationDate?.timeIntervalSinceReferenceDate?.toDouble()
    actual val lastSignInTime: Double?
        get() = ios.lastSignInDate?.timeIntervalSinceReferenceDate?.toDouble()
}
