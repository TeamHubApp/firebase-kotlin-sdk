/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.safeOffer
import dev.gitlive.firebase.auth.ActionCodeResult.*
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.*


actual val Firebase.auth
    get() = FirebaseAuth(FIRAuth.auth())

actual fun Firebase.auth(app: FirebaseApp) =
    FirebaseAuth(FIRAuth.authWithApp(app.ios))

actual class FirebaseAuth internal constructor(val ios: FIRAuth) {

    actual val currentUser: FirebaseUser?
        get() = ios.currentUser?.let { FirebaseUser(it) }

    actual val authStateChanged get() = callbackFlow<FirebaseUser?> {
        val handle = ios.addAuthStateDidChangeListener { _, user -> safeOffer(user?.let { FirebaseUser(it) }) }
        awaitClose { ios.removeAuthStateDidChangeListener(handle) }
    }

    actual val idTokenChanged get() = callbackFlow<FirebaseUser?> {
        val handle = ios.addIDTokenDidChangeListener { _, user -> safeOffer(user?.let { FirebaseUser(it) }) }
        awaitClose { ios.removeIDTokenDidChangeListener(handle) }
    }

    actual var languageCode: String
        get() = ios.languageCode ?: ""
        set(value) { ios.setLanguageCode(value) }

    actual suspend fun applyActionCode(code: String) = ios.await { applyActionCode(code, it) }.run { Unit }
    actual suspend fun confirmPasswordReset(code: String, newPassword: String) = ios.await { confirmPasswordResetWithCode(code, newPassword, it) }.run { Unit }

    actual suspend fun createUserWithEmailAndPassword(email: String, password: String) =
        AuthResult(ios.awaitResult { createUserWithEmail(email = email, password = password, completion = it) })

    @Suppress("UNCHECKED_CAST")
    actual suspend fun fetchSignInMethodsForEmail(email: String) =
        ios.awaitResult<FIRAuth, List<*>?> { fetchSignInMethodsForEmail(email, it) }.orEmpty() as List<String>

    actual suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings?) {
        ios.await { actionCodeSettings?.let { actionSettings -> sendPasswordResetWithEmail(email, actionSettings.toIos(), it) } ?: sendPasswordResetWithEmail(email = email, completion = it) }
    }

    actual suspend fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings) = ios.await { sendSignInLinkToEmail(email, actionCodeSettings.toIos(), it) }.run { Unit }

    actual suspend fun signInWithEmailAndPassword(email: String, password: String) =
        AuthResult(ios.awaitResult { signInWithEmail(email = email, password = password, completion = it) })

    actual suspend fun signInWithCustomToken(token: String) =
        AuthResult(ios.awaitResult { signInWithCustomToken(token, it) })

    actual suspend fun signInAnonymously() =
        AuthResult(ios.awaitResult { signInAnonymouslyWithCompletion(it) })

    actual suspend fun signInWithCredential(authCredential: AuthCredential) =
        AuthResult(ios.awaitResult { signInWithCredential(authCredential.ios, it) })

    actual suspend fun signOut() = ios.throwError { signOut(it) }.run { Unit }

    actual suspend fun updateCurrentUser(user: FirebaseUser) = ios.await { updateCurrentUser(user.ios, it) }.run { Unit }
    actual suspend fun verifyPasswordResetCode(code: String): String = ios.awaitResult { verifyPasswordResetCode(code, it) }

    actual suspend fun <T : ActionCodeResult> checkActionCode(code: String): T {
        val result: FIRActionCodeInfo = ios.awaitResult { checkActionCode(code, it) }
        @Suppress("UNCHECKED_CAST")
        return when(result.operation) {
            FIRActionCodeOperationEmailLink -> SignInWithEmailLink
            FIRActionCodeOperationVerifyEmail -> VerifyEmail(result.email!!)
            FIRActionCodeOperationPasswordReset -> PasswordReset(result.email!!)
            FIRActionCodeOperationRecoverEmail -> RecoverEmail(result.email!!, result.previousEmail!!)
            FIRActionCodeOperationVerifyAndChangeEmail -> VerifyBeforeChangeEmail(result.email!!, result.previousEmail!!)
            FIRActionCodeOperationRevertSecondFactorAddition -> RevertSecondFactorAddition(result.email!!, null)
            FIRActionCodeOperationUnknown -> throw UnsupportedOperationException(result.operation.toString())
            else -> throw UnsupportedOperationException(result.operation.toString())
        } as T
    }

    actual fun useEmulator(host: String, port: Int) = ios.useEmulatorWithHost(host, port.toLong())
}

actual class AuthResult internal constructor(val ios: FIRAuthDataResult) {
    actual val user: FirebaseUser?
        get() = FirebaseUser(ios.user)
}

actual class AuthTokenResult(val ios: FIRAuthTokenResult) {
//    actual val authTimestamp: Long
//        get() = ios.authDate
    actual val claims: Map<String, Any>
        get() = ios.claims.map { it.key.toString() to it.value as Any }.toMap()
//    actual val expirationTimestamp: Long
//        get() = ios.expirationDate
//    actual val issuedAtTimestamp: Long
//        get() = ios.issuedAtDate
    actual val signInProvider: String?
        get() = ios.signInProvider
    actual val token: String?
        get() = ios.token
}

internal fun ActionCodeSettings.toIos() = FIRActionCodeSettings().also {
    it.URL =  NSURL.URLWithString(url)
    androidPackageName?.run { it.setAndroidPackageName(packageName, installIfNotAvailable, minimumVersion) }
    it.dynamicLinkDomain = dynamicLinkDomain
    it.handleCodeInApp = canHandleCodeInApp
    iOSBundleId?.run { it.setIOSBundleID(this) }
}

actual open class FirebaseAuthException(message: String): FirebaseException(message)
actual open class FirebaseAuthActionCodeException(message: String): FirebaseAuthException(message)
actual open class FirebaseAuthEmailException(message: String): FirebaseAuthException(message)
actual open class FirebaseAuthInvalidCredentialsException(message: String): FirebaseAuthException(message)
actual open class FirebaseAuthInvalidUserException(message: String): FirebaseAuthException(message)
actual open class FirebaseAuthMultiFactorException(message: String): FirebaseAuthException(message)
actual open class FirebaseAuthRecentLoginRequiredException(message: String): FirebaseAuthException(message)
actual open class FirebaseAuthUserCollisionException(message: String): FirebaseAuthException(message)
actual open class FirebaseAuthWebException(message: String): FirebaseAuthException(message)

internal fun <T, R> T.throwError(block: T.(errorPointer: CPointer<ObjCObjectVar<NSError?>>) -> R): R {
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

internal suspend inline fun <T, reified R> T.awaitResult(function: T.(callback: (R?, NSError?) -> Unit) -> Unit): R {
    val job = CompletableDeferred<R?>()
    function { result, error ->
        if(error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    return job.await() as R
}

internal suspend inline fun <T> T.await(function: T.(callback: (NSError?) -> Unit) -> Unit) {
    val job = CompletableDeferred<Unit>()
    function { error ->
        if(error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    job.await()
}

private fun NSError.toException() = when(domain) {
    FIRAuthErrorDomain -> when(code) {
        FIRAuthErrorCodeInvalidActionCode,
        FIRAuthErrorCodeExpiredActionCode -> FirebaseAuthActionCodeException(toString())

        FIRAuthErrorCodeInvalidEmail -> FirebaseAuthEmailException(toString())

        FIRAuthErrorCodeCaptchaCheckFailed,
        FIRAuthErrorCodeInvalidPhoneNumber,
        FIRAuthErrorCodeMissingPhoneNumber,
        FIRAuthErrorCodeInvalidVerificationID,
        FIRAuthErrorCodeInvalidVerificationCode,
        FIRAuthErrorCodeMissingVerificationID,
        FIRAuthErrorCodeMissingVerificationCode,
        FIRAuthErrorCodeWeakPassword,
        FIRAuthErrorCodeInvalidCredential -> FirebaseAuthInvalidCredentialsException(toString())

        FIRAuthErrorCodeInvalidUserToken -> FirebaseAuthInvalidUserException(toString())

        FIRAuthErrorCodeRequiresRecentLogin -> FirebaseAuthRecentLoginRequiredException(toString())

        FIRAuthErrorCodeSecondFactorAlreadyEnrolled,
        FIRAuthErrorCodeSecondFactorRequired,
        FIRAuthErrorCodeMaximumSecondFactorCountExceeded,
        FIRAuthErrorCodeMultiFactorInfoNotFound -> FirebaseAuthMultiFactorException(toString())

        FIRAuthErrorCodeEmailAlreadyInUse,
        FIRAuthErrorCodeAccountExistsWithDifferentCredential,
        FIRAuthErrorCodeCredentialAlreadyInUse -> FirebaseAuthUserCollisionException(toString())

        FIRAuthErrorCodeWebContextAlreadyPresented,
        FIRAuthErrorCodeWebContextCancelled,
        FIRAuthErrorCodeWebInternalError -> FirebaseAuthWebException(toString())

        else -> FirebaseAuthException(toString())
    }
    else -> FirebaseAuthException(toString())
}
