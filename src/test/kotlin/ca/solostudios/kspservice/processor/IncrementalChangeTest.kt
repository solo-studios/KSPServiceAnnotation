package ca.solostudios.kspservice.processor

import ca.solostudios.kspservice.util.kotlinCompilation
import ca.solostudios.kspservice.util.ksp1LanguageVersions
import ca.solostudios.kspservice.util.ksp2LanguageVersions
import ca.solostudios.kspservice.util.shouldBeSuccess
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
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
class IncrementalChangeTest : FunSpec({
    context("an incremental change") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) ksp2LanguageVersions else ksp1LanguageVersions) { kotlinVersion ->
                context("incremental=$incrementalKsp, ksp2=$ksp2, kotlin=$kotlinVersion") {
                    val compilation = kotlinCompilation {
                        sources += SourceFile.kotlin(
                            "CustomCallable.kt",
                            """
                                package test
                                import ca.solostudios.kspservice.annotation.Service
                                import java.util.concurrent.Callable

                                @Service(Callable::class)
                                class CustomCallable : Callable<String> {
                                    override fun call(): String = "Hello world!"
                                }
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

                    compilation.compile()

                    compilation.sources += SourceFile.kotlin(
                        "CustomCallable2.kt",
                        """
                            package test
                            import ca.solostudios.kspservice.annotation.Service
                            import java.util.concurrent.Callable

                            @Service(Callable::class)
                            class CustomCallable2 : Callable<String> {
                                override fun call(): String = "Hello world!"
                            }
                        """.trimIndent()
                    )

                    test("should not error") {
                        val result = shouldNotThrowAny { compilation.compile() }

                        result.shouldBeSuccess()
                    }

                    test("should produce correct files") {
                        shouldNotThrowAny { compilation.compile() }

                        val generatedSourcesDir = compilation.kspSourcesDir
                        val generatedServicesDir = generatedSourcesDir.resolve("resources/META-INF/services")
                        val callableServiceFile = generatedServicesDir.resolve("java.util.concurrent.Callable")

                        generatedServicesDir.shouldExist()
                        generatedServicesDir.shouldBeADirectory()
                        generatedServicesDir shouldContainFile "java.util.concurrent.Callable"

                        callableServiceFile.shouldExist()
                        callableServiceFile.shouldBeAFile()
                        callableServiceFile.readText() shouldContain "test.CustomCallable"
                        callableServiceFile.readText() shouldContain "test.CustomCallable2"
                    }
                }
            }
        }
    }
})
