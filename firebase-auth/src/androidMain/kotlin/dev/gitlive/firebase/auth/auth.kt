/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("android")
package dev.gitlive.firebase.auth

import com.google.firebase.auth.ActionCodeEmailInfo
import com.google.firebase.auth.ActionCodeMultiFactorInfo
import com.google.firebase.auth.ActionCodeResult.*
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.safeOffer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual val Firebase.auth
    get() = FirebaseAuth(com.google.firebase.auth.FirebaseAuth.getInstance())

actual fun Firebase.auth(app: FirebaseApp) =
    FirebaseAuth(com.google.firebase.auth.FirebaseAuth.getInstance(app.android))

actual class FirebaseAuth internal constructor(val android: com.google.firebase.auth.FirebaseAuth) {
    actual val currentUser: FirebaseUser?
        get() = android.currentUser?.let { FirebaseUser(it) }

    actual val authStateChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val listener = object : AuthStateListener {
            override fun onAuthStateChanged(auth: com.google.firebase.auth.FirebaseAuth) {
                safeOffer(auth.currentUser?.let { FirebaseUser(it) })
            }
        }
        android.addAuthStateListener(listener)
        awaitClose { android.removeAuthStateListener(listener) }
    }

    actual val idTokenChanged get(): Flow<FirebaseUser?> = callbackFlow {
        val listener = object : com.google.firebase.auth.FirebaseAuth.IdTokenListener {
            override fun onIdTokenChanged(auth: com.google.firebase.auth.FirebaseAuth) {
                safeOffer(auth.currentUser?.let { FirebaseUser(it) })
            }
        }
        android.addIdTokenListener(listener)
        awaitClose { android.removeIdTokenListener(listener) }
    }

    actual var languageCode: String
        get() = android.languageCode ?: ""
        set(value) { android.setLanguageCode(value) }


    actual suspend fun applyActionCode(code: String) = android.applyActionCode(code).await().run { Unit }
    actual suspend fun confirmPasswordReset(code: String, newPassword: String) = android.confirmPasswordReset(code, newPassword).await().run { Unit }

    actual suspend fun createUserWithEmailAndPassword(email: String, password: String) =
        AuthResult(android.createUserWithEmailAndPassword(email, password).await())

    actual suspend fun fetchSignInMethodsForEmail(email: String): List<String> = android.fetchSignInMethodsForEmail(email).await().signInMethods.orEmpty()

    actual suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings?) {
        android.sendPasswordResetEmail(email, actionCodeSettings?.toAndroid()).await()
    }

    actual suspend fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings) = android.sendSignInLinkToEmail(email, actionCodeSettings.toAndroid()).await().run { Unit }

    actual suspend fun signInWithEmailAndPassword(email: String, password: String) =
        AuthResult(android.signInWithEmailAndPassword(email, password).await())

    actual suspend fun signInWithCustomToken(token: String) =
        AuthResult(android.signInWithCustomToken(token).await())

    actual suspend fun signInAnonymously() = AuthResult(android.signInAnonymously().await())

    actual suspend fun signInWithCredential(authCredential: AuthCredential) =
        AuthResult(android.signInWithCredential(authCredential.android).await())

    actual suspend fun signOut() = android.signOut()

    actual suspend fun updateCurrentUser(user: FirebaseUser) = android.updateCurrentUser(user.android).await().run { Unit }
    actual suspend fun verifyPasswordResetCode(code: String): String = android.verifyPasswordResetCode(code).await()

    actual suspend fun <T : ActionCodeResult> checkActionCode(code: String): T {
        val result = android.checkActionCode(code).await()
        @Suppress("UNCHECKED_CAST")
        return when(result.operation) {
            SIGN_IN_WITH_EMAIL_LINK -> ActionCodeResult.SignInWithEmailLink
            VERIFY_EMAIL -> ActionCodeResult.VerifyEmail(result.info!!.email)
            PASSWORD_RESET -> ActionCodeResult.PasswordReset(result.info!!.email)
            RECOVER_EMAIL -> (result.info as ActionCodeEmailInfo).run {
                ActionCodeResult.RecoverEmail(email, previousEmail)
            }
            VERIFY_BEFORE_CHANGE_EMAIL -> (result.info as ActionCodeEmailInfo).run {
                ActionCodeResult.VerifyBeforeChangeEmail(email, previousEmail)
            }
            REVERT_SECOND_FACTOR_ADDITION -> (result.info as ActionCodeMultiFactorInfo).run {
                ActionCodeResult.RevertSecondFactorAddition(email, MultiFactorInfo(multiFactorInfo))
            }
            ERROR -> throw UnsupportedOperationException(result.operation.toString())
            else -> throw UnsupportedOperationException(result.operation.toString())
        } as T
    }

//    actual fun useEmulator(host: String, port: Int) = android.useEmulator(host, port)
    actual fun useEmulator(host: String, port: Int) = Unit
}

actual class AuthResult internal constructor(val android: com.google.firebase.auth.AuthResult) {
    actual val user: FirebaseUser?
        get() = android.user?.let { FirebaseUser(it) }
}

actual class AuthTokenResult(val android: com.google.firebase.auth.GetTokenResult) {
//    actual val authTimestamp: Long
//        get() = android.authTimestamp
    actual val claims: Map<String, Any>
        get() = android.claims
//    actual val expirationTimestamp: Long
//        get() = android.expirationTimestamp
//    actual val issuedAtTimestamp: Long
//        get() = android.issuedAtTimestamp
    actual val signInProvider: String?
        get() = android.signInProvider
    actual val token: String?
        get() = android.token
}

internal fun ActionCodeSettings.toAndroid() = com.google.firebase.auth.ActionCodeSettings.newBuilder()
    .setUrl(url)
    .also { androidPackageName?.run { it.setAndroidPackageName(packageName, installIfNotAvailable, minimumVersion) } }
    .also { dynamicLinkDomain?.run { it.setDynamicLinkDomain(this) } }
    .setHandleCodeInApp(canHandleCodeInApp)
    .also { iOSBundleId?.run { it.setIOSBundleId(this) } }
    .build()

actual typealias FirebaseAuthException = com.google.firebase.auth.FirebaseAuthException
actual typealias FirebaseAuthActionCodeException = com.google.firebase.auth.FirebaseAuthActionCodeException
actual typealias FirebaseAuthEmailException = com.google.firebase.auth.FirebaseAuthEmailException
actual typealias FirebaseAuthInvalidCredentialsException = com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
actual typealias FirebaseAuthInvalidUserException = com.google.firebase.auth.FirebaseAuthInvalidUserException
actual typealias FirebaseAuthMultiFactorException = com.google.firebase.auth.FirebaseAuthMultiFactorException
actual typealias FirebaseAuthRecentLoginRequiredException = com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
actual typealias FirebaseAuthUserCollisionException = com.google.firebase.auth.FirebaseAuthUserCollisionException
actual typealias FirebaseAuthWebException = com.google.firebase.auth.FirebaseAuthWebException
