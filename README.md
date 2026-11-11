# kotlin-rocket-lib

Library for creating Rocket.Chat bots in Kotlin

[![Build Status](https://github.com/paulchen/kotlin-rocket-lib/actions/workflows/build_after_push.yml/badge.svg)](https://github.com/paulchen/kotlin-rocket-lib/actions/workflows/build_after_push.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=paulchen_kotlin-rocket-lib&metric=alert_status)](https://sonarcloud.io/dashboard?id=paulchen_kotlin-rocket-lib)
[![Codefactor Analysis](https://www.codefactor.io/repository/github/paulchen/kotlin-rocket-lib/badge?style=flat-square)](https://www.codefactor.io/repository/github/paulchen/kotlin-rocket-lib/)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/d821d9304f0241a1a1d2a5195f83d330)](https://www.codacy.com/gh/paulchen/kotlin-rocket-lib/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=paulchen/kotlin-rocket-lib&amp;utm_campaign=Badge_Grade)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

This library is available from
[Maven Central](https://central.sonatype.com/artifact/at.rueckgr.kotlin.rocketbot/kotlin-rocket-lib).

You can also build it locally and publish it to the local Maven repository by running

`gradlew publishToMavenLocal`

Then, create your own bot project and and a dependency to `at.rueckgr.kotlin.rocketbot:kotlin-rocket-lib`.

Finally, create a `main` method in your project, instantiate the class `at.rueckgr.kotlin.rocketbot`
there and invoke `start()` on that instance.

Refer to `main.kt` in `kotlin-rocket-bot` for an example on how to do this.

__Important__: This bot handles incoming Rocket.Chat messages asynchronously.
Therefore, you need to ensure thread-safety in your handlers.  

## About the build

`./gradlew downloadRocketChatSpecs` downloads `user-management.yaml`, `statistics.yaml`, and `messaging.yaml`
from [Rocket.Chat's OpenAPI repository](https://github.com/RocketChat/Rocket.Chat-Open-API)
into `build/openapi/`. The `build` task also runs
this task automatically. Existing, unchanged outputs are reused by Gradle.
The upstream commit and SHA-256 checksums are pinned in `build.gradle.kts`; update them
together when upgrading the definitions. A fresh build requires network access.

The Kotlin/Ktor clients are generated into `build/generated/openapi/` before Kotlin
compilation and when building the sources JAR. Run
`./gradlew generateRocketChatUserManagementClient generateRocketChatStatisticsClient generateRocketChatMessagingClient`
to generate them explicitly. Generated types are internal implementation details;
`RestApiClient` remains the public facade and uses the generated clients for
`users.setStatus`, `roles.getUsersInRole`, `statistics`, and `chat.sendMessage`. For user management,
only the two migrated endpoints are currently generated. Other endpoints can be enabled in the
generator's operation filter as they are migrated. `skipFormModel=false` ensures
multipart models referenced by generated APIs are included. Generating the entire
user-management definition still produces invalid Kotlin for the `selector` query
parameter of `users.autocomplete` (`forEach` called on `Any`).
For messaging, only `chat.sendMessage` is currently generated.

This product uses the NVD API but is not endorsed or certified by the NVD.
