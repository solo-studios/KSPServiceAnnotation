package ca.solostudios.kspservice.processor

import ca.solostudios.kspservice.util.kotlinCompilation
import ca.solostudios.kspservice.util.ksp1LanguageVersions
import ca.solostudios.kspservice.util.ksp2LanguageVersions
import ca.solostudios.kspservice.util.shouldBeSuccess
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.writeTo
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
class DeferredServiceTest : FunSpec({
    context("a deferred service") {
        checkAll(Exhaustive.boolean() /*Exhaustive.boolean()*/) { incrementalKsp /*, ksp2*/ ->
            // https://github.com/google/ksp/issues/1916
            val ksp2 = true

            checkAll(if (ksp2) ksp2LanguageVersions else ksp1LanguageVersions) { kotlinVersion ->
                context("incremental=$incrementalKsp, ksp2=$ksp2, kotlin=$kotlinVersion") {
                    class InterfaceGenerator(private val codeGenerator: CodeGenerator) : SymbolProcessor {
                        override fun process(resolver: Resolver): List<KSAnnotated> {
                            resolver.getSymbolsWithAnnotation("test.GenerateInterface")
                                .filterIsInstance<KSClassDeclaration>()
                                .forEach { annotated ->
                                    FileSpec.builder(ClassName(annotated.packageName.asString(), "I${annotated.simpleName.asString()}"))
                                        .addType(
                                            TypeSpec.interfaceBuilder("I${annotated.simpleName.asString()}")
                                                .addOriginatingKSFile(annotated.containingFile!!)
                                                .build()
                                        )
                                        .build()
                                        .writeTo(codeGenerator, aggregating = false)
                                }
                            return emptyList()
                        }
                    }

                    val compilation = kotlinCompilation {
                        sources += SourceFile.kotlin(
                            "GenerateInterface.kt",
                            """
                                package test

                                annotation class GenerateInterface
                            """.trimIndent()
                        )
                        sources += SourceFile.kotlin(
                            "CustomCallable.kt",
                            """
                                package test
                                import ca.solostudios.kspservice.annotation.Service

                                @GenerateInterface
                                class Example

                                @Service(IExample::class)
                                class ExampleImplementation : IExample
                            """.trimIndent()
                        )

                        inheritClassPath = true
                        languageVersion = if (ksp2) "2.0" else kotlinVersion
                        apiVersion = languageVersion

                        configureKsp(ksp2) {
                            symbolProcessorProviders += KSPServiceProcessorProvider()
                            symbolProcessorProviders += SymbolProcessorProvider { env -> InterfaceGenerator(env.codeGenerator) }
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
                        val callableServiceFile = generatedServicesDir.resolve("test.IExample")

                        generatedServicesDir.shouldExist()
                        generatedServicesDir.shouldBeADirectory()
                        generatedServicesDir shouldContainFile "test.IExample"

                        callableServiceFile.shouldExist()
                        callableServiceFile.shouldBeAFile()
                        callableServiceFile.readText() shouldContain "ExampleImplementation"
                    }
                }
            }
        }
    }
})
