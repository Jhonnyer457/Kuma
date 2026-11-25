plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.metro)
}

if (gradle.startParameter.taskNames.any { it.contains("google", true) }) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// --- Version e identidad de firma controladas por CI (GitHub Actions) ---
// CI pasa -PappVersionName y -PappVersionCode; en local usa valores de desarrollo.
val appVersionName: String = (findProperty("appVersionName") as String?) ?: "0.0.0-dev"
val appVersionCode: Int = (findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
// Firma: variables de entorno inyectadas por el workflow desde los GitHub Secrets.
val ciKeystorePath: String? = System.getenv("CI_KEYSTORE_PATH")

fun computeGitCommitHash(): String {
    val envHash = System.getenv("COMMIT_HASH")
    if (!envHash.isNullOrBlank()) {
        return envHash.take(7)
    }
    val gitHash = try {
        providers.exec {
            commandLine("git", "rev-parse", "HEAD")
        }.standardOutput.asText.get().trim().take(7)
    } catch (e: Exception) {
        try {
            providers.exec {
                commandLine("git", "rev-parse", "--verify", "--short=7", "HEAD")
            }.standardOutput.asText.get().trim()
        } catch (e2: Exception) {
            ""
        }
    }
    if (gitHash.isNotEmpty()) {
        return gitHash
    }
    val fallbackHash = System.getenv("GITHUB_SHA")
    if (!fallbackHash.isNullOrBlank()) {
        return fallbackHash.take(7)
    }
    return ""
}

val gitCommitHash = computeGitCommitHash()

android {
    namespace = "ani.dantotsu"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "ani.dantotsu"
        minSdk = 26
        targetSdk = 36

        versionName = appVersionName
        versionCode = appVersionCode

        signingConfig = signingConfigs.getByName("debug")
    }

    signingConfigs {
        if (!ciKeystorePath.isNullOrBlank()) {
            create("ci") {
                storeFile = file(ciKeystorePath)
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    flavorDimensions += "store"

    productFlavors {
        create("fdroid") {
            dimension = "store"
            versionNameSuffix = "-fdroid"
        }
        create("google") {
            dimension = "store"
            isDefault = true
        }
    }

    buildTypes {
        create("alpha") {
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-alpha01"
            manifestPlaceholders["icon_placeholder"] = "@mipmap/ic_launcher_alpha"
            manifestPlaceholders["icon_placeholder_round"] = "@mipmap/ic_launcher_alpha_round"
            isDebuggable = true
            isJniDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            isDefault = true
        }

        getByName("debug") {
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta01"
            manifestPlaceholders["icon_placeholder"] = "@mipmap/ic_launcher_beta"
            manifestPlaceholders["icon_placeholder_round"] = "@mipmap/ic_launcher_beta_round"
            isDebuggable = false
        }

        getByName("release") {
            signingConfig = signingConfigs.findByName("ci") ?: signingConfigs.getByName("debug")
            manifestPlaceholders["icon_placeholder"] = "@mipmap/ic_launcher"
            manifestPlaceholders["icon_placeholder_round"] = "@mipmap/ic_launcher_round"
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts.add("**/libavcodec.so")
            pickFirsts.add("**/libavdevice.so")
            pickFirsts.add("**/libavfilter.so")
            pickFirsts.add("**/libavformat.so")
            pickFirsts.add("**/libavutil.so")
            pickFirsts.add("**/libswresample.so")
            pickFirsts.add("**/libswscale.so")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-XXLanguage:+ContextParameters",
            "-Xmulti-platform"
        )
    }
}

configurations.all {
    exclude(group = "org.json", module = "json")
}

dependencies {
    // ffmpeg-kit for NativeVideoDownloader
    implementation(libs.ffmpeg.kit)

    // Media3 & decoders
    implementation(libs.bundles.media3)
    implementation(libs.bundles.subtitles)
    implementation(libs.mediarouter)
    // HTTP/3 (QUIC) — media3-datasource-cronet:1.11.1 API surface:
    // Tier 1 (GMS devices): CronetDataSource via Play Services CronetProvider — HTTP/3 + HTTP/2.
    // Tier 2 (fallback / F-Droid): OkHttp — HTTP/2. CronetProvider absent → caught → falls through.
    implementation(libs.media3.cronet)
    // GMS Cronet provider — google flavor only; absent from F-Droid APK
    add("googleImplementation", libs.play.services.cronet)

    // Firebase
    add("googleImplementation", platform(libs.firebase.bom))
    add("googleImplementation", libs.bundles.firebase)

    // AndroidX
    implementation(libs.bundles.androidx)
    implementation(libs.androidx.webkit)

    // Kotlin
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib)

    // Core libs
    implementation(libs.bundles.misc)
    implementation(libs.metro.runtime)
    implementation(libs.bundles.sqldelight)
    implementation(libs.androidx.profileInstaller)

    // Shizuku
    implementation(libs.bundles.shizuku)

    // Glide
    implementation(libs.bundles.glide)
    ksp(libs.glide.ksp)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // UI
    implementation(libs.material)
    implementation(libs.materialKolor)
    implementation(files("libs/AnimatedBottomBar-7fcb9af.aar"))
    implementation(libs.flexbox)
    implementation(libs.kenburns)
    implementation(libs.subsampling)
    implementation(libs.gesture)
    implementation(libs.webgpuviewer)
    implementation(libs.image.decoder)
    implementation(libs.androidx.compose.ui)
    implementation(libs.mlkit.text.japanese)
    implementation(libs.mlkit.text)
    implementation(libs.play.services.base)
    // Readium Kotlin Toolkit
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.readium.navigator)
    implementation(libs.readium.navigator.media.tts)
    implementation(libs.readium.adapter.pdfium.document)
    implementation(libs.readium.adapter.pdfium.navigator)
    implementation(libs.dialogs)
    implementation(libs.charts)

    implementation(libs.bundles.markwon)
    implementation(libs.bundles.groupie)
    implementation(libs.bundles.rx)
    implementation(libs.bundles.okhttp)
    implementation(libs.okio)

    // Archive support (local source)
    implementation(libs.libarchive)
    implementation(libs.xmlutil.core)
    implementation(libs.xmlutil.serialization)

    // libtorrent
    implementation(libs.libtorrent4j)
    implementation(libs.libtorrent4j.android.arm)
    implementation(libs.libtorrent4j.android.arm64)
    implementation(libs.libtorrent4j.android.x86)
    implementation(libs.libtorrent4j.android.x86.x64)

    // LeakCanary & Plumber (Active in Debug, Alpha, and Release builds for memory leak diagnosis)
    implementation(libs.leakcanary.android)
    implementation(libs.leakcanary.plumber)
}
