plugins {
    idea
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.hytale-modding.info/releases") {
            name = "HytaleModdingReleases"
        }
        // Official Hytale Maven repositories (for Server API / build info)
        maven("https://maven.hytale.com/release") {
            name = "hytale-release"
        }
        maven("https://maven.hytale.com/pre-release") {
            name = "hytale-pre-release"
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
