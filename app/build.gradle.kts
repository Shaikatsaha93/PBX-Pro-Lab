import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.example.ctbd.firebase"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // R8: strips unused code (e.g. unused Material icons) and resources
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      // Real phones only: 64-bit ARM (almost every phone) and 32-bit ARM (old phones).
      // Linphone ships ~30 MB of native code per ABI, so x86/x86_64 would double the APK.
      ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }
    debug {
      // Separate app ID so the debug build installs next to the existing release app
      applicationIdSuffix = ".pbxprolab"
      signingConfig = signingConfigs.getByName("debugConfig")
      // This build is the one installed on phones, so shrink it like release:
      // without R8 the unused Compose/Material icon code alone is ~20 MB of dex.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Phones only (64-bit ARM). Add "x86_64" back here to run on an x86_64 emulator.
      ndk { abiFilters += listOf("arm64-v8a") }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  // One APK per CPU type for release (app-arm64-v8a-release.apk, app-armeabi-v7a-release.apk),
  // so each phone only downloads the native code it can run.
  splits {
    abi {
      isEnable = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
      reset()
      include("arm64-v8a", "armeabi-v7a")
      isUniversalApk = false
    }
  }
  androidResources {
    // Linphone sounds the app never plays (~8 MB). Kept: the default ringtone
    // (notes_of_the_optimistic.mkv), ringback.wav and toy-mono.wav (hold music, see SipManager).
    ignoreAssetsPatterns += listOf(
      "dont_wait_too_long.mkv",
      "its_a_game.mkv",
      "soft_as_snow.mkv",
      "house_keeping.mkv",
      "four_hands_together.mkv",
      "leaving_dreams.mkv",
      "oldphone-mono.wav",
      "hello16000.wav",
      "hello8000.wav",
      "incoming_chat.wav"
    )
  }
  packaging {
    jniLibs {
      excludes += "**/libmsandroidcamera2.so"
      // Linphone's own test suites; nothing in the app loads them
      excludes += "**/liblinphonetester.so"
      excludes += "**/libbctoolbox-tester.so"
      // Store native libs compressed in the APK (about half the download size);
      // Android extracts them once at install time.
      useLegacyPackaging = true
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed
// (Retrofit/Moshi/OkHttp, Firebase AI and App Check were never used and only added APK size).
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation("org.linphone:linphone-sdk-android:5.2.+")
  implementation(libs.androidx.media)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.firebase.messaging)
  // GoogleApiAvailability for the FCM Play Services check (came in via App Check before)
  implementation("com.google.android.gms:play-services-base:18.9.0")
  // Uncomment these three (plus libs.firebase.auth) to use Google Sign-In via Credential Manager:
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
