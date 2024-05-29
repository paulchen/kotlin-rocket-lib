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

This product uses the NVD API but is not endorsed or certified by the NVD.
