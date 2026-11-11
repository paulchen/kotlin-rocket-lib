import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import java.nio.file.Files
import java.net.URI
import java.security.MessageDigest

val log4jVersion = "2.26.1"
val ktorVersion = "3.6.1"
val kotlinVersion = "2.4.21"
val reflectionsVersion = "0.10.2"
val commonsCodecVersion = "1.23.0"
val jacksonVersion = "2.23.0"
val nettyVersion = "4.2.20.Final"

group = "at.rueckgr.kotlin.rocketbot"
version = "0.1.9-SNAPSHOT"

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    `java-library`
    `maven-publish`
    id("io.github.ben-manes.versions") version "0.67.0"
    id("app.cash.licensee") version "1.14.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("signing")
    id("org.openapi.generator") version "7.27.0"
}

dependencyLocking {
    lockAllConfigurations()
}

// Update the commit and checksums together when upgrading the Rocket.Chat API definitions.
val rocketChatOpenApiCommit = "f920bb9c701aa12ea03e040a50566e3689c63d59"
val rocketChatOpenApiChecksums = mapOf(
    "user-management.yaml" to "c1d3d49b18972eed5b7a4942dd9fdbf763f9ef4a9c6d674e56f6a733deca62eb",
    "statistics.yaml" to "edb959e0b056597ecf0bc168659a6b38bab27a18fab3555c13dc1f43df937a7f",
    "messaging.yaml" to "0aca8e78413cdc1c26c962a97c92098a48acf79353dfcacc3a6bd786bc51c0e3",
)

val downloadRocketChatSpecTasks = rocketChatOpenApiChecksums.map { (filename, checksum) ->
    val specUrl = "https://raw.githubusercontent.com/RocketChat/Rocket.Chat-Open-API/$rocketChatOpenApiCommit/$filename"
    val destination = layout.buildDirectory.file("openapi/$filename")
    tasks.register("downloadRocketChatSpec" + filename.removeSuffix(".yaml")
        .split('-').joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }) {
        group = "openapi"
        description = "Downloads and verifies Rocket.Chat's $filename."
        inputs.property("url", specUrl)
        inputs.property("sha256", checksum)
        outputs.file(destination)

        doLast {
            val connection = URI(specUrl).toURL().openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 60_000
            }
            val bytes = connection.getInputStream().use { it.readBytes() }
            val actualChecksum = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            check(actualChecksum == checksum) {
                "SHA-256 mismatch for $filename: expected $checksum, got $actualChecksum"
            }
            val target = destination.get().asFile
            Files.createDirectories(target.parentFile.toPath())
            target.writeBytes(bytes)
        }
    }
}

val downloadRocketChatSpecs = tasks.register("downloadRocketChatSpecs") {
    group = "openapi"
    description = "Downloads the pinned Rocket.Chat OpenAPI definitions and verifies their SHA-256 checksums."
    dependsOn(downloadRocketChatSpecTasks)
}

tasks.named("build") {
    dependsOn(downloadRocketChatSpecs)
}

val generateRocketChatClients = rocketChatOpenApiChecksums.keys.map { filename ->
    val name = filename.removeSuffix(".yaml").split('-')
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    val packageRoot = "at.rueckgr.kotlin.rocketbot.generated." +
        filename.removeSuffix(".yaml").replace("-", "")
    val generatedDirectory = layout.buildDirectory.dir("generated/openapi/${filename.removeSuffix(".yaml")}")
    kotlin.sourceSets.named("main") {
        kotlin.srcDir(generatedDirectory.map { it.dir("src/main/kotlin") })
    }
    tasks.register<GenerateTask>("generateRocketChat${name}Client") {
        group = "openapi"
        dependsOn(downloadRocketChatSpecs)
        generatorName.set("kotlin")
        library.set("jvm-ktor")
        typeMappings.put("integer", "kotlin.Long")
        inputSpec.set(layout.buildDirectory.file("openapi/$filename"))
        outputDir.set(generatedDirectory)
        packageName.set(packageRoot)
        apiPackage.set("$packageRoot.api")
        modelPackage.set("$packageRoot.model")
        cleanupOutput.set(true)
        // Multipart models may still be imported by APIs that also accept JSON.
        globalProperties.put("skipFormModel", "false")
        if (filename == "user-management.yaml") {
            // The full API still generates invalid selector iteration in users.autocomplete.
            openapiNormalizer.set(mapOf(
                "FILTER" to "operationId:post-api-v1-users.setStatus|get-api-v1-roles.getUsersInRole",
            ))
        }
        if (filename == "messaging.yaml") {
            openapiNormalizer.set(mapOf("FILTER" to "operationId:post-api-v1-chat.sendMessage"))
        }
        configOptions.set(mapOf(
            "serializationLibrary" to "jackson",
            "nonPublicApi" to "true",
        ))
        generateApiTests.set(false)
        generateModelTests.set(false)
        generateApiDocumentation.set(false)
        generateModelDocumentation.set(false)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateRocketChatClients)
}

tasks.matching { it.name == "sourcesJar" }.configureEach {
    dependsOn(generateRocketChatClients)
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    gradleReleaseChannel = "current"
    checkConstraints = false
    checkBuildEnvironmentConstraints = false
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.lowercase().contains("alpha") ||
                candidate.version.lowercase().contains("beta") ||
                candidate.version.lowercase().contains("rc")
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
//    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
//    implementation("org.jetbrains.kotlin:kotlin-reflect")

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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("org.slf4j:slf4j-api:2.0.20")
    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")

    implementation("org.apache.commons:commons-lang3:3.21.0")
    implementation("commons-codec:commons-codec:$commonsCodecVersion")
    implementation("org.reflections:reflections:$reflectionsVersion")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")

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
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

tasks.register("createVersionFile") {
    doLast {
        val file = file("build/generated/resources/library-git-revision")
        Files.createDirectories(file.parentFile.toPath())
        Files.deleteIfExists(file.toPath())

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

    allowUrl("http://www.gnu.org/licenses/lgpl-2.1.html")
    allowUrl("https://opensource.org/license/mit")
}

signing {
    isRequired = !gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal") }

    if (isRequired) {
        val signingKey = findProperty("signingKey").toString()
        val signingPassword = findProperty("signingPassword").toString()
        useInMemoryPgpKeys(signingKey, signingPassword)

        sign(publishing.publications["kotlin-rocket-lib"])
    }
}
