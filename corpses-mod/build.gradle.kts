import org.gradle.api.file.DuplicatesStrategy

plugins {
    `maven-publish`
    id("hytale-mod") version "0.+"
}

group = "dev.chasem.hg"
version = findProperty("plugin_version")?.toString() ?: "0.1.0"
val javaVersion = 25

dependencies {
    // Hytale Server API (from https://maven.hytale.com/release)
    compileOnly(libs.hytale.server)

    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.jspecify)
    implementation(libs.gson)

    // Optional: preview mod icon via /modlist
    runtimeOnly(libs.bettermodlist)

    // Testing
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    withSourcesJar()
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "plugin_group" to findProperty("plugin_group"),
        "plugin_maven_group" to project.group,
        "plugin_name" to project.name,
        "plugin_version" to project.version,
        "server_version" to findProperty("server_version"),

        "plugin_description" to findProperty("plugin_description"),
        "plugin_website" to findProperty("plugin_website"),

        "plugin_main_entrypoint" to findProperty("plugin_main_entrypoint"),
        "plugin_author" to findProperty("plugin_author")
    )

    filesMatching("manifest.json") {
        expand(replaceProperties)
    }
    inputs.properties(replaceProperties)
}

tasks.withType<Jar> {
    manifest {
        attributes["Specification-Title"] = rootProject.name
        attributes["Specification-Version"] = version
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = version
    }
}

// Fat jar - exclude org.bson since Hytale provides it in the app classloader
val fatJar by tasks.registering(Jar::class) {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true

    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") && !it.path.contains("HytaleServer") }
            .map { zipTree(it) }
    }) {
        exclude("org/bson/**")
    }
}

tasks.named("build") {
    dependsOn(fatJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

