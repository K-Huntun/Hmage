import org.gradle.configurationcache.extensions.capitalized

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlinx.atomicfun)
    id("convention.publication")
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }

        publishLibraryVariants("release", "debug")
    }
    ios()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "hmage"
        }
        extraSpecAttributes["resources"] = "['src/commonMain/resources/**', 'src/iosMain/resources/**']"
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.animation)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                implementation(libs.okio)
                implementation(libs.ktor.client.core)
                implementation(libs.hog)
                implementation(libs.uuid)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.atomicfun)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.okio.fakefilesystem)
                implementation(libs.kotlinx.datetime)
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.bundles.test.jvm)
            }
        }

        val androidTest by creating {
            dependsOn(androidMain)
            dependencies {
                implementation(libs.bundles.test.android)
            }
        }

        val iosMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Test by getting
        val iosTest by getting {
            iosSimulatorArm64Test.dependsOn(this)
            dependencies {
                implementation(libs.bundles.test.ios)
            }
        }
    }
}

android {
    namespace = "top.heiha.huntun.hmage"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.md"
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources", "src/androidMain/resources")
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
//    sourceSets["unitTest"].resources.srcDirs("src/commonTest/resources", "src/androidUnitTest/resources")
//    sourceSets["androidTest"].resources.srcDirs("src/commonTest/resources", "src/androidInstrumentedTest/resources")

}

group = "top.heiha.huntun.hmage"
version = "0.0.1-dev"

afterEvaluate {
    listOf("debug", "release").forEach { variant ->
        val copyTaskName = "copyResource${variant.capitalized()}UnitTest"
        tasks.register<Copy>(copyTaskName) {
            from("$projectDir/src/commonTest/resources")
            into("$buildDir/tmp/kotlin-classes/${variant}UnitTest")
            mustRunAfter(tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>())
        }
        tasks.getByName("test${variant.capitalized()}UnitTest") {
            dependsOn(copyTaskName)
        }
    }
}