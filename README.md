# KSP Service Annotation

[![Apache 2.0 license](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/ca.solo-studios/ksp-service-annotation.svg?style=for-the-badge&label=Maven%20Central)](https://search.maven.org/search?q=g:ca.solo-studios%20a:ksp-service-annotation)
[![Pure Kotlin](https://img.shields.io/badge/100%25-kotlin-blue.svg?style=for-the-badge)](https://kotlinlang.org/)
[![Discord Server](https://img.shields.io/discord/871114669761372221?color=7389D8&label=Discord&logo=discord&logoColor=8fa3ff&style=for-the-badge)](https://discord.solo-studios.ca)

This library contains a KSP annotation processor and an `@Service` annotation.

When a service is annotated with `@Service`, it will generate the appropriate files in `META-INF/services/`.

This is inspired by Google's [AutoService](https://github.com/google/auto/tree/master/service).

## License & Credit

This code is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0).

Credit for the implementation of this goes entirely to
[ZacSweers/auto-service-ksp](https://github.com/ZacSweers/auto-service-ksp), as I took a *lot* of code from that
repository.

## Including

You can include KSP Service Annotation in your project by adding the following:

### Gradle Groovy DSL

```groovy
plugins {
    id 'com.google.devtools.ksp' version '1.6.10-1.0.2'
}

dependencies {
    compileOnly 'ca.solo-studios:ksp-service-annotation:1.0.0'
    ksp 'ca.solo-studios:ksp-service-annotation:1.0.0'
}
```

### Gradle Kotlin DSL

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.6.10-1.0.2"
}

dependencies {
    compileOnly("ca.solo-studios:ksp-service-annotation:1.0.0")
    ksp("ca.solo-studios:ksp-service-annotation:1.0.0")
}
```
