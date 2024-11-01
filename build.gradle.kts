@file:Suppress("UnstableApiUsage")

import ca.solostudios.nyx.util.reposiliteMaven

plugins {
    java
    signing
    `java-library`
    `maven-publish`
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.axion.release)
    alias(libs.plugins.nyx)
    alias(libs.plugins.allure)
}

nyx {
    info {
        name = "KSP Service Annotation"
        group = "ca.solo-studios"
        module = "ksp-service-annotation"
        version = scmVersion.version
        description = """
            A KSP Annotation processor to automatically create the required files in META-INF/services for services.
        """.trimIndent()

        organizationUrl = "https://solo-studios.ca/"
        organizationName = "Solo Studios"

        developer {
            id = "solonovamax"
            name = "solonovamax"
            email = "solonovamax@12oclockpoint.com"
            url = "https://solonovamax.gay"
        }

        repository.fromGithub("solo-studios", "KSPServiceAnnotation")
        license.useApachev2()
    }

    compile {
        withJavadocJar()
        withSourcesJar()

        allWarnings = true
        warningsAsErrors = true
        distributeLicense = true
        buildDependsOnJar = true
        reproducibleBuilds = true
        jvmTarget = 8

        kotlin {
            apiVersion = "1.7"
            languageVersion = "1.7"

            withExplicitApi()
        }
    }

    publishing {
        withSignedPublishing()

        repositories {
            maven {
                name = "Sonatype"
                val repositoryId: String? by project
                url = when {
                    repositoryId != null -> uri("https://s01.oss.sonatype.org/service/local/staging/deployByRepositoryId/$repositoryId/")
                    else                 -> uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
                credentials(PasswordCredentials::class)
            }
            reposiliteMaven {
                name = "SoloStudiosReleases"
                url = uri("https://maven.solo-studios.ca/releases/")
                credentials(PasswordCredentials::class)
            }
            reposiliteMaven {
                name = "SoloStudiosSnapshots"
                url = uri("https://maven.solo-studios.ca/snapshots/")
                credentials(PasswordCredentials::class)
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.ksp.api)

    implementation(libs.bundles.kotlinpoet)


    testImplementation(libs.bundles.kotest)
    testImplementation(libs.kotlin.ksp.base)
    testImplementation(libs.bundles.kotlin.scripting)
    testImplementation(libs.bundles.kotlin.compile.testing)
}

allure {
    version = "2.29.0"
    adapter {
        autoconfigure = false
        autoconfigureListeners = false
        frameworks {
            junit5.enabled = false
        }
    }
}

testing.suites {
    withType<JvmTestSuite>().configureEach {
        useJUnitJupiter()

        targets.configureEach {
            testTask {
                maxHeapSize = "4G"
                failFast = false
                forkEvery = 1
                finalizedBy(tasks.allureReport)

                systemProperty("gradle.build.dir", layout.buildDirectory.get().asFile)
                systemProperty("gradle.task.name", name)
                systemProperty("kotest.framework.config.fqn", "ca.solostudios.kspservice.kotest.KotestConfig")
                systemProperty("kotest.framework.classpath.scanning.config.disable", true)
                systemProperty("kotest.framework.classpath.scanning.autoscan.disable", true)

                reports {
                    html.required = false
                    junitXml.required = false
                }
            }
        }
    }
}

tasks {
    allureReport {
        clean = true
    }
}

val isSnapshot: Boolean
    get() = version.toString().endsWith("-SNAPSHOT")
