import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    `maven-publish`
    signing
}

group = "com.varabyte.kotterx"
version = "0.9.9-SNAPSHOT"

fun shouldSign() = (findProperty("kotter.sign") as? String).toBoolean()
fun shouldPublishToGCloud(): Boolean {
    return (findProperty("kotter.gcloud.publish") as? String).toBoolean()
            && findProperty("gcloud.artifact.registry.secret") != null
}

val VARABYTE_REPO_URL = uri("https://us-central1-maven.pkg.dev/varabyte-repos/public")
fun MavenArtifactRepository.gcloudAuth() {
    url = VARABYTE_REPO_URL
    credentials {
        username = "_json_key_base64"
        password = findProperty("gcloud.artifact.registry.secret") as String
    }
    authentication {
        create<BasicAuthentication>("basic")
    }
}

repositories {
    mavenCentral()
    if (shouldPublishToGCloud()) {
        maven { gcloudAuth() }
    }
}

object Versions {
    object Kotlin {
        const val Couroutines = "1.6.0"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Kotlin.Couroutines}")
    implementation(compose.desktop.common)

    implementation(project(":kotter"))
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

publishing {
    publications {
        if (shouldPublishToGCloud()) {
            repositories {
                maven { gcloudAuth() }
            }
        }

        create<MavenPublication>("kotterx-compose") {
            from(components["java"])
            pom {
                description.set("A virtual terminal implemented with Compose for Desktop.")
                artifactId = "kotterx-compose"
                url.set("https://github.com/varabyte/kotter")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}

if (shouldSign()) {
    signing {
        // Signing requires following steps at https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
        // and adding singatory properties somewhere reachable, e.g. ~/.gradle/gradle.properties
        sign(publishing.publications["kotterx-compose"])
    }
}