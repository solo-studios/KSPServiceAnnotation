package ca.solostudios.kspservice.processor

import ca.solostudios.kspservice.util.kotlinCompilation
import ca.solostudios.kspservice.util.shouldBeError
import ca.solostudios.kspservice.util.shouldBeSuccess
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldBeADirectory
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldBeEmptyDirectory
import io.kotest.matchers.file.shouldContainFile
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.boolean
import io.kotest.property.exhaustive.of
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi


@ExperimentalCompilerApi
class ServiceSymbolProcessorTest : FunSpec({
    val kotlin1Versions = Exhaustive.of("1.7", "1.8", "1.9")
    val kotlin2Versions = Exhaustive.of("2.0")

    context("a kotlin service") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) kotlin2Versions else kotlin1Versions) { kotlinVersion ->
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
                    }
                }
            }
        }
    }

    context("a java service") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) kotlin2Versions else kotlin1Versions) { kotlinVersion ->
                context("incremental=$incrementalKsp, ksp2=$ksp2, kotlin=$kotlinVersion") {
                    val compilation = kotlinCompilation {
                        @Suppress("ClassNameDiffersFromFileName")
                        sources += SourceFile.java(
                            "CustomCallable.java",
                            """
                                package test;
                                import ca.solostudios.kspservice.annotation.Service;
                                import java.util.concurrent.Callable;

                                @Service(Callable.class)
                                public class CustomCallable implements Callable<String> {
                                    @Override
                                    public String call() {
                                        return "Hello World";
                                    }
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
                    }
                }
            }
        }
    }

    context("the comment is disabled") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) kotlin2Versions else kotlin1Versions) { kotlinVersion ->
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
                            kspProcessorOptions += "kspservice.comment" to "false"
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
                        val callableServiceFile = generatedServicesDir.resolve("java.util.concurrent.Callable")

                        generatedServicesDir.shouldExist()
                        generatedServicesDir.shouldBeADirectory()
                        generatedServicesDir shouldContainFile "java.util.concurrent.Callable"

                        callableServiceFile.shouldExist()
                        callableServiceFile.shouldBeAFile()
                        callableServiceFile.readText() shouldContain "test.CustomCallable"
                        callableServiceFile.readText() shouldNotContain """
                        ###################################################
                        ## Generated by KSP Service Annotation processor ##
                        ###################################################
                    """.trimIndent()
                    }
                }
            }
        }
    }

    context("multiple annotated services") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) kotlin2Versions else kotlin1Versions) { kotlinVersion ->
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

    context("full test") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) kotlin2Versions else kotlin1Versions) { kotlinVersion ->
                context("incremental=$incrementalKsp, ksp2=$ksp2, kotlin=$kotlinVersion") {
                    val compilation = kotlinCompilation {
                        @Suppress("ClassNameDiffersFromFileName")
                        sources += SourceFile.kotlin(
                            "SomeService.kt",
                            """
                               package test

                               interface SomeService
                            """.trimIndent()
                        )
                        sources += SourceFile.kotlin(
                            "SomeServiceProvider1.kt",
                            """
                                package test

                                import ca.solostudios.kspservice.annotation.Service

                                @Service(SomeService::class)
                                class SomeServiceProvider1 : SomeService
                            """.trimIndent()
                        )
                        sources += SourceFile.kotlin(
                            "SomeServiceProvider2.kt",
                            """
                                package test

                                import ca.solostudios.kspservice.annotation.Service

                                @Service(SomeService::class)
                                class SomeServiceProvider2 : SomeService
                            """.trimIndent()
                        )
                        sources += SourceFile.kotlin(
                            "Enclosing.kt",
                            """
                                package test

                                import ca.solostudios.kspservice.annotation.Service

                                class Enclosing {
                                    @Service(SomeService::class)
                                    class NestedSomeServiceProvider : SomeService
                                }
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
                            "AnotherServiceProvider.kt",
                            """
                                package test

                                import ca.solostudios.kspservice.annotation.Service

                                @Service(AnotherService::class)
                                class AnotherServiceProvider : AnotherService
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
                        someServiceFile.readText() shouldContain "test.SomeServiceProvider1"
                        someServiceFile.readText() shouldContain "test.SomeServiceProvider2"
                        someServiceFile.readText() shouldContain "test.Enclosing${'$'}NestedSomeServiceProvider"

                        anotherServiceFile.shouldExist()
                        anotherServiceFile.shouldBeAFile()
                        anotherServiceFile.readText() shouldContain "test.AnotherServiceProvider"
                    }
                }
            }
        }
    }

    context("no service specified") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) kotlin2Versions else kotlin1Versions) { kotlinVersion ->
                context("incremental=$incrementalKsp, ksp2=$ksp2, kotlin=$kotlinVersion") {
                    val compilation = kotlinCompilation {
                        sources += SourceFile.kotlin(
                            "NoServices.kt",
                            """
                               package test

                               import ca.solostudios.kspservice.annotation.Service

                               @Service
                               class NoServices
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

    context("a generic service") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) kotlin2Versions else kotlin1Versions) { kotlinVersion ->
                context("incremental=$incrementalKsp, ksp2=$ksp2, kotlin=$kotlinVersion") {
                    val compilation = kotlinCompilation {
                        sources += SourceFile.kotlin(
                            "GenericService.kt",
                            """
                                package test

                                interface GenericService<T>
                            """.trimIndent()
                        )
                        sources += SourceFile.kotlin(
                            "GenericServiceProvider.kt",
                            """
                                package test

                                import ca.solostudios.kspservice.annotation.Service

                                @Service(GenericService::class)
                                class GenericServiceProvider<T> : GenericService<T>
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

                    test("should not error") {
                        val result = shouldNotThrowAny { compilation.compile() }

                        result.shouldBeSuccess()
                    }

                    test("should produce correct files") {
                        shouldNotThrowAny { compilation.compile() }

                        val generatedSourcesDir = compilation.kspSourcesDir
                        val generatedServicesDir = generatedSourcesDir.resolve("resources/META-INF/services")
                        val genericServiceFile = generatedServicesDir.resolve("test.GenericService")

                        generatedServicesDir.shouldExist()
                        generatedServicesDir.shouldBeADirectory()
                        generatedServicesDir shouldContainFile "test.GenericService"

                        genericServiceFile.shouldExist()
                        genericServiceFile.shouldBeAFile()
                        genericServiceFile.readText() shouldContain "test.GenericServiceProvider"
                    }
                }
            }
        }
    }

    context("a missing service") {
        checkAll(Exhaustive.boolean(), Exhaustive.boolean()) { incrementalKsp, ksp2 ->
            checkAll(if (ksp2) kotlin2Versions else kotlin1Versions) { kotlinVersion ->
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
