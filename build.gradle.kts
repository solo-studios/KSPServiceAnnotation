import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    signing
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.5.0"
}

group = "ca.solo-studios"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.6.10-1.0.2")
    implementation("com.squareup:kotlinpoet:1.10.2")
    
    testImplementation(kotlin("test"))
    testImplementation(kotlin("script-runtime"))
    testImplementation(kotlin("compiler-embeddable"))
    testImplementation(kotlin("scripting-compiler-embeddable"))
    
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.7")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.7")
    testImplementation("com.google.devtools.ksp:symbol-processing:1.6.10-1.0.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadoc by tasks.getting(Javadoc::class)

val jar by tasks.getting(Jar::class)

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.build {
    dependsOn(tasks.withType<Jar>())
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(sourcesJar)
            artifact(javadocJar)
            artifact(jar)
            
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
            name = "sonatypeStaging"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials(org.gradle.api.credentials.PasswordCredentials::class)
        }
        maven {
            name = "sonatypeSnapshot"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials(PasswordCredentials::class)
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
