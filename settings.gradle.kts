pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    
}
rootProject.name = "esp-idf-provisioning"

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    val kotlinVersion = "1.5.21"
    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
        classpath("com.android.tools.build:gradle:7.0.3")
    }
}
