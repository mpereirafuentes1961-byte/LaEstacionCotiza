plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
dependencies { implementation("androidx.core:core:1.15.0") }
android { namespace = "cl.laestacion.cotiza"; compileSdk = 35
 defaultConfig { applicationId = "cl.laestacion.cotiza"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
