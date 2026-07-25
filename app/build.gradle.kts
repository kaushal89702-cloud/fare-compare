import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.family.farecompare"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.family.farecompare"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Optional. Never commit a real key: set PLACES_API_KEY in your
        // local.properties (which is gitignored) to enable live Google
        // Places autocomplete. Without it, the app falls back to manual
        // address entry everywhere Places would have been used.
        val placesApiKey = project.rootProject.file("local.properties")
            .takeIf { it.exists() }
            ?.let { file ->
                val properties = Properties()
                file.inputStream().use { properties.load(it) }
                properties.getProperty("PLACES_API_KEY", "")
            }
            .orEmpty()

        buildConfigField("String", "PLACES_API_KEY", "\"$placesApiKey\"")
        manifestPlaceholders["placesApiKey"] = placesApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.navigation.compose)
    implementation(libs.material.icons.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.material.icons.extended)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.play.services.location)
    implementation(libs.places)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
