package ca.solostudios.kspservice.processor

import ca.solostudios.kspservice.util.kotlinCompilation
import ca.solostudios.kspservice.util.ksp1LanguageVersions
import ca.solostudios.kspservice.util.ksp2LanguageVersions
import ca.solostudios.kspservice.util.shouldBeError
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldBeEmptyDirectory
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.boolean
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@ExperimentalCompilerApi
class MissingServiceTest : FunSpec({
    context("a missing service") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) ksp2LanguageVersions else ksp1LanguageVersions) { kotlinVersion ->
                context("incremental=$incrementalKsp, ksp2=$ksp2, kotlin=$kotlinVersion") {
                    val compilation = kotlinCompilation {
                        sources += SourceFile.kotlin(
                            "MissingService.kt",
                            """
                                package test

                                import ca.solostudios.kspservice.annotation.Service

                                @Service(MissingService::class)
                                class MissingServiceProvider : MissingService
                            """.trimIndent()
                        )

                        inheritClassPath = true
                        languageVersion = if (ksp2) "2.0" else kotlinVersion
                        apiVersion = languageVersion

                        configureKsp(ksp2) {
                            symbolProcessorProviders += KSPServiceProcessorProvider()
                            incremental = incrementalKsp
                        }
                    }

                    test("should error") {
                        val result = shouldNotThrowAny { compilation.compile() }

                        result.shouldBeError()
                    }

                    test("should not produce any files") {
                        shouldNotThrowAny { compilation.compile() }

                        val generatedSourcesDir = compilation.kspSourcesDir
                        generatedSourcesDir.resolve("resources").shouldBeEmptyDirectory()
                    }
                }
            }
        }
    }
})
