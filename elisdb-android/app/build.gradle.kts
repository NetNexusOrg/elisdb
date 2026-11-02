import java.io.File
import java.io.InputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.elisdb"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.example.elisdb"
        minSdk = 24
        targetSdk = 37
        versionCode = (providers.gradleProperty("app.versionCode").getOrElse("1")).toInt()
        versionName = providers.gradleProperty("app.versionName").getOrElse("1.0")

        val serverBaseUrl = providers.environmentVariable("ELISDB_SERVER_URL").orNull ?: ""
        buildConfigField("String", "SERVER_BASE_URL", "\"$serverBaseUrl\"")
    }

    signingConfigs {
        // Keystore details come from keystore.properties (git-ignored) or, failing
        // that, from env vars. Resolution is centralised so the release build fails
        // fast when credentials are missing instead of producing an unsigned artifact.
        fun loadKeystoreProperties(): Properties? {
            val keystoreFile = File(rootProject.rootDir, "keystore.properties")
            if (!keystoreFile.exists()) return null
            val result = Properties()
            val stream: InputStream = keystoreFile.inputStream()
            stream.use { result.load(it) }
            return result
        }

        val props = loadKeystoreProperties()
        val keystorePath = props?.getProperty("keystore.path")
            ?: providers.environmentVariable("KEYSTORE_PATH").orNull
        val keystorePassword = props?.getProperty("keystore.password")
            ?: providers.environmentVariable("KEYSTORE_PASSWORD").orNull
        val aliasName = props?.getProperty("key.alias")
            ?: providers.environmentVariable("KEY_ALIAS").orNull
        val aliasPassword = props?.getProperty("key.password")
            ?: providers.environmentVariable("KEY_PASSWORD").orNull
        if (
          keystorePath != null &&
          keystorePassword != null &&
          aliasName != null &&
          aliasPassword != null
        ) {
          create("release") {
            storeFile = file(keystorePath)
            storePassword = keystorePassword
            keyAlias = aliasName
            keyPassword = aliasPassword
          }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    // Fail fast when a release build is requested without signing credentials,
    // instead of silently producing an unsigned artifact.
    val releaseSigningConfigured = signingConfigs.findByName("release") != null
    val buildsRelease =
        gradle.startParameter.taskNames.any {
            it.contains("Release", ignoreCase = true) || it.contains("release", ignoreCase = true)
        }
    if (!releaseSigningConfigured && buildsRelease) {
        throw GradleException(
            "Release signing not configured. Provide keystore.properties (see scripts/build-release.sh) " +
                "or set KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD env vars."
        )
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // CameraX
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)

  // Maps
  implementation(libs.maplibre.android)

  // Location
  implementation(libs.play.services.location)

  // Image loading
  implementation(libs.coil.compose)

  // Networking: Ktor client + kotlinx-serialization
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.android)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.kotlinx.serialization.json)
  // Ed25519 signing (net.i2p.crypto:eddsa)
  implementation(libs.eddsa)
  testImplementation(libs.ktor.client.mock)
  // EncryptedSharedPreferences for the server signing key at rest
  implementation(libs.androidx.security.crypto)
  // EXIF orientation for photo re-encoding
  implementation(libs.androidx.exifinterface)
  // Backports java.util.Base64 (and other JDK APIs) to API < 26
  coreLibraryDesugaring(libs.desugar.jdk.libs)
}
