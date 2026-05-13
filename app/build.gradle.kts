import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    kotlin("kapt")
    alias(libs.plugins.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: ""
val supabaseKey = localProperties.getProperty("SUPABASE_KEY") ?: ""


android {
    namespace = "com.example.fencing_project"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fencing_project"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }


        //buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        //buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "SUPABASE_URL", "\"https://ozvbygwhnlxzkayahndl.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im96dmJ5Z3dobmx4emtheWFobmRsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njk1OTM1NTUsImV4cCI6MjA4NTE2OTU1NX0.c9nffFzJSh2a-Bg_vJVN1IV64BfdLKLgr-0dg4l8oVc\"")
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
        buildConfig=true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.lifecycle.viewmodel.compose)

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.firestore.ktx)

    //navigation
    implementation(libs.androidx.navigation.compose)

    //Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    // Play Services
    implementation(libs.play.services.auth)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Supabase Storage
    implementation("io.github.jan-tennert.supabase:storage-kt:2.0.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")

    // Для работы с изображениями
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil:2.5.0")

    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")

    // Для выбора изображений
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui:1.5.4")

    implementation("com.vanniktech:android-image-cropper:4.7.0")

    // Для работы с Excel
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
// Для разрешений на Android
    implementation("androidx.core:core-ktx:1.12.0")




    val work_version = "2.11.1"

    // (Java only)
    implementation("androidx.work:work-runtime:$work_version")

    // Kotlin + coroutines
    implementation("androidx.work:work-runtime-ktx:$work_version")

    // Hilt для WorkManager
    implementation ("androidx.hilt:hilt-work:1.1.0")
    kapt ("androidx.hilt:hilt-compiler:1.1.0")

    // Для работы с файлами
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("androidx.appcompat:appcompat:1.6.1")

    dependencies {
        val room_version = "2.8.4"

        implementation("androidx.room:room-runtime:$room_version")

        // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
        // See Add the KSP plugin to your project
        //ksp("androidx.room:room-compiler:$room_version")

        // If this project only uses Java source, use the Java annotationProcessor
        // No additional plugins are necessary
        kapt("androidx.room:room-compiler:$room_version")

        // optional - Kotlin Extensions and Coroutines support for Room
        implementation("androidx.room:room-ktx:$room_version")

        // optional - RxJava2 support for Room
        implementation("androidx.room:room-rxjava2:$room_version")

        // optional - RxJava3 support for Room
        implementation("androidx.room:room-rxjava3:$room_version")

        // optional - Guava support for Room, including Optional and ListenableFuture
        implementation("androidx.room:room-guava:$room_version")

        // optional - Test helpers
        testImplementation("androidx.room:room-testing:$room_version")

        // optional - Paging 3 Integration
        implementation("androidx.room:room-paging:$room_version")

        implementation("androidx.core:core-splashscreen:1.2.0")

        val work_version = "2.11.1"

        // (Java only)
        implementation("androidx.work:work-runtime:$work_version")

        // Kotlin + coroutines
        implementation("androidx.work:work-runtime-ktx:$work_version")

        // optional - RxJava2 support
        implementation("androidx.work:work-rxjava2:$work_version")

        // optional - GCMNetworkManager support
        implementation("androidx.work:work-gcm:$work_version")

        // optional - Test helpers
        androidTestImplementation("androidx.work:work-testing:$work_version")

        // optional - Multiprocess support
        implementation("androidx.work:work-multiprocess:$work_version")
    }




}
kapt {
    correctErrorTypes = true
}

