# kotlin-rocket-lib

Library for creating Rocket.Chat bots in Kotlin

[![Build Status](https://github.com/paulchen/kotlin-rocket-lib/actions/workflows/build_after_push.yml/badge.svg)](https://github.com/paulchen/kotlin-rocket-lib/actions/workflows/build_after_push.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=paulchen_kotlin-rocket-lib&metric=alert_status)](https://sonarcloud.io/dashboard?id=paulchen_kotlin-rocket-lib)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)


Add the variables `OSSRH_USERNAME` and `OSSRH_PASSWORD` to your `~/.gradle/gradle.properties`.
You can leave both empty as long as you do not plan to publish to Maven Central via OSSRH:

```
OSSRH_USERNAME=
OSSRH_PASSWORD=
```

Build this project and publish it to the local Maven repository by running

`gradlew publishToMavenLocal`

Then, create your own bot project and and a dependency to `at.rueckgr.kotlin.rocketbot:kotlin-rocket-lib`.

Finally, create a `main` method in your project, instantiate the class `at.rueckgr.kotlin.rocketbot`
there and invoke `start()` on that instance.

Refer to `main.kt` in `kotlin-rocket-bot` for an example on how to do this.

__Important__: This bot handles incoming Rocket.Chat messages asynchronously.
Therefore, you need to ensure thread-safety in your handlers.  
