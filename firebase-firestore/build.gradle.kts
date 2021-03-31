/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

version = project.property("firebase-firestore.version") as String

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.31"
}

android {
    compileSdkVersion(property("targetSdkVersion") as Int)
    defaultConfig {
        minSdkVersion(property("minSdkVersion") as Int)
        targetSdkVersion(property("targetSdkVersion") as Int)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
        getByName("androidTest"){
            java.srcDir(file("src/androidAndroidTest/kotlin"))
            manifest.srcFile("src/androidAndroidTest/AndroidManifest.xml")
        }
    }
    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }
    packagingOptions {
        pickFirst("META-INF/kotlinx-serialization-core.kotlin_module")
        pickFirst("META-INF/AL2.0")
        pickFirst("META-INF/LGPL2.1")
        pickFirst("androidsupportmultidexversion.txt")
    }
    lintOptions {
        isAbortOnError = false
    }
}

kotlin {
    js("browser", IR) {
        useCommonJs()
        browser()
    }
    js("node", LEGACY) {
        useCommonJs()
        nodejs()
        binaries.executable()
    }
    android {
        publishAllLibraryVariants()
    }

    fun nativeTargetConfig(): KotlinNativeTarget.() -> Unit = {
        val nativeFrameworkPaths = listOf(
            rootProject.project("firebase-app").projectDir.resolve("src/nativeInterop/cinterop/Carthage/Build/iOS"),
            projectDir.resolve("src/nativeInterop/cinterop/Carthage/Build/iOS")
        )

        binaries {
            getTest("DEBUG").apply {
                linkerOpts(nativeFrameworkPaths.map { "-F$it" })
                linkerOpts("-ObjC")
            }
        }

        compilations.getByName("main") {
            cinterops.create("FirebaseFirestore") {
                compilerOpts(nativeFrameworkPaths.map { "-F$it" })
                extraOpts("-verbose")
            }
        }
    }
    jvm {
        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    if (project.extra["ideaActive"] as Boolean) {
        iosX64("ios", nativeTargetConfig())
    } else {
        ios(configure = nativeTargetConfig())
    }

    sourceSets {
        all {
            languageSettings.apply {
                apiVersion = "1.4"
                languageVersion = "1.4"
                progressiveMode = true
                useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
                useExperimentalAnnotation("kotlinx.serialization.InternalSerializationApi")
            }
        }

        val commonMain by getting {
            dependencies {
                api(project(":firebase-app"))
                implementation(project(":firebase-common"))
            }
        }

        val androidMain by getting {
            dependencies {
                api("com.google.firebase:firebase-firestore:22.1.1")
            }
        }

        val jvmMain by getting {
            kotlin.srcDir("src/androidMain/kotlin")
        }

        val iosMain by getting

        val jsMain by creating

        val browserMain by getting {
            dependsOn(jsMain)
        }
        val nodeMain by getting {
            dependsOn(jsMain)
        }
    }
}
signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
