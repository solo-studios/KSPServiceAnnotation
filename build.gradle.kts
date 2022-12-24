import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    java
    signing
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.7.20"
    id("org.jetbrains.dokka") version "1.7.20"
}

group = "ca.solo-studios"
version = "1.0.2"

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
    val kspVersion = "1.7.20-1.0.7"
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    
    val kotlinPoetVersion = "1.12.0"
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    
    testImplementation(kotlin("test"))
    testImplementation(kotlin("script-runtime"))
    testImplementation(kotlin("compiler-embeddable"))
    testImplementation(kotlin("scripting-compiler-embeddable"))
    
    val kotlinCompileTestingVersion = "1.4.9"
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:$kotlinCompileTestingVersion")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:$kotlinCompileTestingVersion")
    testImplementation("com.google.devtools.ksp:symbol-processing:$kspVersion")
    
    val junitJupiterVersion = "5.9.1"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
}

tasks.test {
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
                name.set("KSP Service Annotation")
                description.set("A KSP Annotation processor to automatically create the required files in META-INF/services for services.")
                url.set("https://github.com/solo-studios/KSPServiceAnnotation")
                
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
                    url.set("https://github.com/solo-studios/KSPServiceAnnotation/issues")
                }
                scm {
                    connection.set("scm:git:https://github.com/solo-studios/KSPServiceAnnotation.git")
                    developerConnection.set("scm:git:ssh://github.com/solo-studios/KSPServiceAnnotation.git")
                    url.set("https://github.com/solo-studios/KSPServiceAnnotation/")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "SonatypeStaging"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = "SonatypeSnapshot"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = "SoloStudiosRelease"
            url = uri("https://maven.solo-studios.ca/release/")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = "SoloStudiosSnapshot"
            url = uri("https://maven.solo-studios.ca/snapshot/")
            credentials(PasswordCredentials::class)
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
