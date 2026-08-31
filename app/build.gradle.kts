import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.security.MessageDigest
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val tmdbApiToken = localProperties.getProperty("TMDB_API_TOKEN", "")
val torrServerVersion = "MatriX.144.1"
val torrServerBinaries = mapOf(
    "arm64-v8a" to ("TorrServer-android-arm64" to "bb7e9b4d0dc894f8da3e32496e7487be93b8f8b04ada549396a7ab4dc85ea63b"),
    "armeabi-v7a" to ("TorrServer-android-arm7" to "dd6c9dcfa11a450bff6ebaa8992b1823c32e3b9417657f93c8852271ded3949e"),
    "x86_64" to ("TorrServer-android-amd64" to "58f3152471d01a86454b74f49029e62cdc2b3844451151d950cb40130a81ccb3"),
)
val generatedTorrServerDir = layout.buildDirectory.get().asFile.resolve("generated/torrserver/jniLibs")
val prepareTorrServerBinaries = tasks.register("prepareTorrServerBinaries") {
    notCompatibleWithConfigurationCache("Downloads and verifies external native binaries")
    outputs.dir(generatedTorrServerDir)
    doLast {
        torrServerBinaries.forEach { (abi, binaryAndHash) ->
            val (binary, expectedHash) = binaryAndHash
            val output = generatedTorrServerDir.resolve("$abi/libtorrserver.so")
            output.parentFile.mkdirs()
            if (!output.exists() || output.inputStream().use { input ->
                    MessageDigest.getInstance("SHA-256").digest(input.readBytes()).joinToString("") { "%02x".format(it) }
                } != expectedHash) {
                val url = "https://github.com/YouROK/TorrServer/releases/download/$torrServerVersion/$binary"
                URI(url).toURL().openStream().use { input -> output.outputStream().use(input::copyTo) }
            }
            val actualHash = output.inputStream().use { input ->
                MessageDigest.getInstance("SHA-256").digest(input.readBytes()).joinToString("") { "%02x".format(it) }
            }
            check(actualHash == expectedHash) { "Invalid SHA-256 for $binary" }
        }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.shjamolov.mediastreamplayer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.shjamolov.mediastreamplayer"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TMDB_API_TOKEN", "\"${tmdbApiToken.replace("\"", "\\\"")}\"")
    }

    sourceSets.getByName("main").jniLibs.srcDir(generatedTorrServerDir)

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs.useLegacyPackaging = true
    }
}

tasks.configureEach {
    if (name.startsWith("merge") && (name.endsWith("NativeLibs") || name.endsWith("JniLibFolders"))) {
        dependsOn(prepareTorrServerBinaries)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.xmlpull)
    testImplementation(libs.kxml2)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
