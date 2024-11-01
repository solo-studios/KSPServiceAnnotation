package ca.solostudios.kspservice.processor

import ca.solostudios.kspservice.util.kotlinCompilation
import ca.solostudios.kspservice.util.ksp1LanguageVersions
import ca.solostudios.kspservice.util.ksp2LanguageVersions
import ca.solostudios.kspservice.util.shouldBeSuccess
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldBeADirectory
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldContainFile
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.boolean
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi


@ExperimentalCompilerApi
class MultipleServicesTest : FunSpec({
    context("multiple annotated services") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) ksp2LanguageVersions else ksp1LanguageVersions) { kotlinVersion ->
                context("incremental=$incrementalKsp, ksp2=$ksp2, kotlin=$kotlinVersion") {
                    val compilation = kotlinCompilation {
                        sources += SourceFile.kotlin(
                            "SomeService.kt",
                            """
                                package test

                                interface SomeService
                            """.trimIndent()
                        )
                        sources += SourceFile.kotlin(
                            "AnotherService.kt",
                            """
                                package test

                                interface AnotherService
                            """.trimIndent()
                        )
                        sources += SourceFile.kotlin(
                            "MultiServiceProvider.kt",
                            """
                                package test

                                import ca.solostudios.kspservice.annotation.Service

                                @Service(SomeService::class, AnotherService::class)
                                class MultiServiceProvider : SomeService, AnotherService
                            """.trimIndent()
                        )

                        inheritClassPath = true
                        languageVersion = if (ksp2) "2.0" else kotlinVersion
                        apiVersion = languageVersion

                        configureKsp(ksp2) {
                            symbolProcessorProviders += KSPServiceProcessorProvider()
                            incremental = incrementalKsp
                            kspWithCompilation = true
                        }
                    }

                    test("should not error") {
                        val result = shouldNotThrowAny { compilation.compile() }

                        result.shouldBeSuccess()
                    }

                    test("should produce correct files") {
                        shouldNotThrowAny { compilation.compile() }

                        val generatedSourcesDir = compilation.kspSourcesDir
                        val generatedServicesDir = generatedSourcesDir.resolve("resources/META-INF/services")
                        val someServiceFile = generatedServicesDir.resolve("test.SomeService")
                        val anotherServiceFile = generatedServicesDir.resolve("test.AnotherService")

                        generatedServicesDir.shouldExist()
                        generatedServicesDir.shouldBeADirectory()
                        generatedServicesDir shouldContainFile "test.SomeService"
                        generatedServicesDir shouldContainFile "test.AnotherService"

                        someServiceFile.shouldExist()
                        someServiceFile.shouldBeAFile()
                        someServiceFile.readText() shouldContain "test.MultiServiceProvider"

                        anotherServiceFile.shouldExist()
                        anotherServiceFile.shouldBeAFile()
                        anotherServiceFile.readText() shouldContain "test.MultiServiceProvider"
                    }
                }
            }
        }
    }
})
