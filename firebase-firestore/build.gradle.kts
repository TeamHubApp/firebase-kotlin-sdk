import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

version = project.property("firebase-firestore.version") as String

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.21"
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
    js {
        useCommonJs()
        nodejs()
        browser()
    }
    android {
        publishLibraryVariants("release", "debug")
    }
    val iosArm64 = iosArm64()
    val iosX64 = iosX64("ios") {
        binaries {
            getTest("DEBUG").apply {
                linkerOpts(
                    "-F${rootProject.projectDir}/firebase-app/src/iosMain/c_interop/Carthage/Build/iOS/",
                    "-F$projectDir/src/iosMain/c_interop/Carthage/Build/iOS/"
                )
                linkerOpts("-ObjC")
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xuse-experimental=kotlin.Experimental",
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xuse-experimental=kotlinx.serialization.InternalSerializationApi"
        )
    }

    if (findProperty("firebase-kotlin-sdk.firestore.useIR")?.toString()?.equals("true", ignoreCase = true) == true) {
        logger.info("Using IR compilation for firebase-firestore module")
        targets.getByName<KotlinAndroidTarget>("android").compilations.all {
            kotlinOptions {
                useIR = true
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":firebase-app"))
                implementation(project(":firebase-common"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("com.google.firebase:firebase-firestore:22.0.0")
                implementation("com.android.support:multidex:1.0.3")
            }
        }

        val jsMain by getting {}
        val iosMain by getting {}

        configure(listOf(iosArm64, iosX64)) {
            compilations.getByName("main") {
                source(sourceSets.get("iosMain"))
                val firebasefirestore by cinterops.creating {
                    packageName("cocoapods.FirebaseFirestore")
                    defFile(file("$projectDir/src/iosMain/c_interop/FirebaseFirestore.def"))
                    compilerOpts("-F$projectDir/src/iosMain/c_interop/Carthage/Build/iOS/")
                }
            }
        }
    }
}
signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
