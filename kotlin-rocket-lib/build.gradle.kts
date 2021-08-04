val log4jVersion = "2.14.1"
val ktorVersion = "1.6.1"
val reflectionsVersion = "0.9.12"
var commonsCodecVersion = "1.15"

group = "at.rueckgr.kotlin.rocketbot"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.5.21"
    `java-library`
    `maven-publish`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.4")

    implementation("org.slf4j:slf4j-api:1.7.32")
    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-codec:commons-codec:${commonsCodecVersion}")
    implementation("org.reflections:reflections:$reflectionsVersion")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.apache.commons:commons-math3:3.6.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
publishing {
    publications {
        create<MavenPublication>("kotlin-rocket-lib") {
            from(components["kotlin"])
        }
    }
//
//    repositories {
//        maven {
//            name = "myRepo"
//            url = uri(layout.buildDirectory.dir("repo"))
//        }
//    }
}
