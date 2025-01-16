import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.reporting.ReportGenerator
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val log4jVersion = "2.24.3"
val ktorVersion = "3.0.3"
val kotlinVersion = "2.1.0"
val reflectionsVersion = "0.10.2"
val commonsCodecVersion = "1.17.2"
val jacksonVersion = "2.18.2"
val nettyVersion = "4.1.117.Final"

group = "at.rueckgr.kotlin.rocketbot"
version = "0.1.7-SNAPSHOT"

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    `java-library`
    `maven-publish`
    id("com.github.ben-manes.versions") version "0.51.0"
    id("app.cash.licensee") version "1.12.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("signing")
    id("org.owasp.dependencycheck") version "12.0.0"
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    gradleReleaseChannel = "current"
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.lowercase().contains("alpha") ||
                candidate.version.lowercase().contains("beta") ||
                candidate.version.lowercase().contains("rc")
    }
}

configure<DependencyCheckExtension> {
    format = ReportGenerator.Format.ALL.toString()
    analyzers.assemblyEnabled = false
    failBuildOnCVSS = 7f
    suppressionFile = file("$rootDir/cve-suppressions.xml").toString()
    nvd.apiKey = System.getenv("NVD_API_KEY")
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
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    // CVE-2023-34462
    implementation("io.netty:netty-codec-http2:$nettyVersion")
    implementation("io.netty:netty-transport-native-kqueue:$nettyVersion")
    implementation("io.netty:netty-transport-native-epoll:$nettyVersion")

    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    implementation("org.slf4j:slf4j-api:2.0.16")
    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")

    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("commons-codec:commons-codec:$commonsCodecVersion")
    implementation("org.reflections:reflections:$reflectionsVersion")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.apache.commons:commons-math3:3.6.1")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
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
}

nexusPublishing {
    repositories {
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}

tasks.register("createVersionFile") {
    doLast {
        val file = File("build/generated/resources/library-git-revision")
        file.parentFile.parentFile.mkdir()
        file.parentFile.mkdir()
        file.delete()

        file.appendText(String.format("revision = %s\n", runGit("git", "rev-parse", "--short", "HEAD")))
        file.appendText(String.format("commitMessage = %s\n", runGit("git", "log", "-1", "--pretty=%B")))
    }
}

fun runGit(vararg args: String) =
    project
        .providers.exec {
            commandLine(*args)
        }
        .standardOutput.asText.get()
        .split("\n")[0].trim()

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
    val signingKey = findProperty("signingKey").toString()
    val signingPassword = findProperty("signingPassword").toString()
    useInMemoryPgpKeys(signingKey, signingPassword)

    sign(publishing.publications["kotlin-rocket-lib"])
}

tasks.withType<Sign>().configureEach {
    onlyIf { !gradle.startParameter.taskNames.contains("publishToMavenLocal") }
}

