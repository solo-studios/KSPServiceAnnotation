import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    java
    signing
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.axion.release)
}

group = "ca.solo-studios"
version = scmVersion.version

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    target.compilations.configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            apiVersion = "1.7"
            languageVersion = "1.7"
        }
    }
}

dependencies {
    implementation(libs.kotlin.ksp.api)
    
    implementation(libs.bundles.kotlinpoet)
    
    
    testImplementation(libs.bundles.junit)
    
    testImplementation(libs.kotlin.ksp.base)
    testImplementation(libs.bundles.kotlin.testing)
    testImplementation(libs.bundles.kotlin.scripting)
}

tasks.test {
    // enabled = false
    useJUnitPlatform()
}

val dokkaHtml by tasks.getting(DokkaTask::class)

val javadocJar by tasks.getting(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

val sourcesJar by tasks.getting(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.build {
    dependsOn(tasks.withType<Jar>())
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            version = version as String
            groupId = group as String
            artifactId = "ksp-service-annotation"
            
            pom {
                val projectOrg = "solo-studios"
                val projectRepo = "KSPServiceAnnotation"
                val githubBaseUri = "github.com/$projectOrg/$projectRepo"
                val githubUrl = "https://$githubBaseUri"
    
                name.set("KSP Service Annotation")
                description.set("A KSP Annotation processor to automatically create the required files in META-INF/services for services.")
                url.set(githubUrl)
    
                inceptionYear.set("2021")
    
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0/")
                    }
                }
                developers {
                    developer {
                        id.set("solonovamax")
                        name.set("solonovamax")
                        email.set("solonovamax@12oclockpoint.com")
                        url.set("https://github.com/solonovamax")
                    }
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("$githubUrl/issues")
                }
                scm {
                    connection.set("scm:git:$githubUrl.git")
                    developerConnection.set("scm:git:ssh://$githubBaseUri.git")
                    url.set(githubUrl)
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "Sonatype"
    
            val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") // releases repo
            val snapshotUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") // snapshot repo
            url = if (isSnapshot) snapshotUrl else releasesUrl
    
            credentials(PasswordCredentials::class)
        }
        maven {
            name = "SoloStudios"
    
            val releasesUrl = uri("https://maven.solo-studios.ca/releases/")
            val snapshotUrl = uri("https://maven.solo-studios.ca/snapshots/")
            url = if (isSnapshot) snapshotUrl else releasesUrl
    
            credentials(PasswordCredentials::class)
            authentication { // publishing doesn't work without this for some reason
                create<BasicAuthentication>("basic")
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

val isSnapshot: Boolean
    get() = version.toString().endsWith("-SNAPSHOT")
