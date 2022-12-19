val coroutinesVersion = "1.6.4"
val serializationVersion = "1.4.0"

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

version = "0.1"

kotlin {
    android()
    macosX64()
    iosX64()
    iosArm64()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
                implementation("io.github.aakira:napier:2.6.1")
                implementation("com.juul.kable:core:0.19.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${coroutinesVersion}")
                implementation("com.google.crypto.tink:tink-android:1.6.1")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
            }
        }
        val appleMain by creating {
            dependsOn(commonMain)
        }
        val appleTest by creating
        val macosX64Main by getting {
            dependsOn(appleMain)
        }
        val macosX64Test by getting {
            dependsOn(appleTest)
        }
        val iosX64Main by getting {
            dependsOn(appleMain)
        }
        val iosX64Test by getting {
            dependsOn(appleTest)
        }
        val iosArm64Main by getting {
            dependsOn(appleMain)
        }
        val iosArm64Test by getting {
            dependsOn(appleTest)
        }
    }
}

android {
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }
}
