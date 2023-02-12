import java.io.ByteArrayOutputStream
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

val log4jVersion = "2.19.0"
val ktorVersion = "2.2.2"
val reflectionsVersion = "0.10.2"
var commonsCodecVersion = "1.15"

group = "at.rueckgr.kotlin.rocketbot"
version = "0.1.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
    `java-library`
    `maven-publish`
    id("com.github.ben-manes.versions") version "0.44.0"
    id("app.cash.licensee") version "1.6.0"
    id("maven-publish")
    id("signing")
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    gradleReleaseChannel = "current"
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.toLowerCase().contains("alpha") || candidate.version.toLowerCase().contains("beta")
    }
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    main {
        resources {
            srcDirs("build/generated/resources")
        }
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")

    implementation("org.slf4j:slf4j-api:2.0.6")
    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-codec:commons-codec:$commonsCodecVersion")
    implementation("org.reflections:reflections:$reflectionsVersion")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.apache.commons:commons-math3:3.6.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

publishing {
    publications {
        create<MavenPublication>("kotlin-rocket-lib") {
            from(components["java"])

            pom {
                name.set("kotlin-rocket-lib")
                description.set("Library for creating Rocket.Chat bots in Kotlin")
                url.set("https://github.com/paulchen/kotlin-rocket-lib")
                licenses {
                    license {
                        name.set("GPL-3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("paulchen")
                        name.set("Paul Staroch")
                        email.set("paul@staroch.name")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/paulchen/kotlin-rocket-lib.git")
                    url.set("https://github.com/paulchen/kotlin-rocket-lib")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = if (version.toString().endsWith("SNAPSHOT")) {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            }
            else {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
            credentials {
                username = project.property("OSSRH_USERNAME").toString()
                password = project.property("OSSRH_PASSWORD").toString()
            }
        }
    }
}

tasks.create("createVersionFile") {
    doLast {
        val file = File("build/generated/resources/library-git-revision")
        file.parentFile.parentFile.mkdir()
        file.parentFile.mkdir()
        file.delete()

        file.appendText(String.format("revision = %s\n", runGit("git", "rev-parse", "--short", "HEAD")))
        file.appendText(String.format("commitMessage = %s\n", runGit("git", "log", "-1", "--pretty=%B")))
    }
}

fun runGit(vararg args: String): String {
    val outputStream = ByteArrayOutputStream()
    project.exec {
        commandLine(*args)
        standardOutput = outputStream
    }
    return outputStream.toString().split("\n")[0].trim()
}

tasks.processResources {
    dependsOn("createVersionFile")
}

tasks.publishToMavenLocal {
    // necessary to build sources jar
    dependsOn("build")
}

licensee {
    allow("Apache-2.0")
    allow("MIT")

    allowUrl("http://www.gnu.org/licenses/lgpl-2.1.html")
}

signing {
    sign(publishing.publications["kotlin-rocket-lib"])
}
