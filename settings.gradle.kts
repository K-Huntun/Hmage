pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Hmage"
include(":androidApp")
include(":hmage")
includeBuild("convention-plugins")
include(":commonui")
