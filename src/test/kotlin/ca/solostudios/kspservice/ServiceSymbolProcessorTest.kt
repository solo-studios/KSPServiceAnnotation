package ca.solostudios.kspservice

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import ca.solostudios.kspservice.processor.KSPServiceProcessor
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ServiceSymbolProcessorTest {
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testServiceKotlin(incremental: Boolean) {
        val source = SourceFile.kotlin("CustomCallable.kt", """
            package test
            import ca.solostudios.kspservice.annotation.Service
            import java.util.concurrent.Callable
            
            @Service(Callable::class)
            class CustomCallable : Callable<String> {
                override fun call(): String = "Hello world!"
            }
        """)
        
        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            inheritClassPath = true
            symbolProcessorProviders = listOf(KSPServiceProcessor.Provider())
            kspIncremental = incremental
        }
        val result = compilation.compile()
        assertEquals(ExitCode.OK, result.exitCode)
        
        val generatedSourcesDir = compilation.kspSourcesDir
        val generatedFile = File(generatedSourcesDir, "resources/META-INF/services/java.util.concurrent.Callable")
        
        assertTrue(generatedFile.exists())
        assertEquals("test.CustomCallable\n", generatedFile.readText())
    }
    
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testServiceJava(incremental: Boolean) {
        val source = SourceFile.java("CustomCallable.java", """
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
        """)
        
        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            inheritClassPath = true
            symbolProcessorProviders = listOf(KSPServiceProcessor.Provider())
            kspIncremental = incremental
        }
        val result = compilation.compile()
        assertEquals(ExitCode.OK, result.exitCode)
        
        val generatedSourcesDir = compilation.kspSourcesDir
        val generatedFile = File(generatedSourcesDir, "resources/META-INF/services/java.util.concurrent.Callable")
        
        assertTrue(generatedFile.exists())
        assertEquals("test.CustomCallable\n", generatedFile.readText())
    }
    
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun fullServiceTest(incremental: Boolean) {
        val sourceFiles = mutableListOf<SourceFile>()
        sourceFiles += SourceFile.kotlin("SomeService.kt", """
           package test
           
           interface SomeService
        """)
        sourceFiles += SourceFile.kotlin("SomeServiceProvider1.kt", """
            package test
            
            import ca.solostudios.kspservice.annotation.Service
           
            @Service(SomeService::class)
            class SomeServiceProvider1 : SomeService
        """)
        sourceFiles += SourceFile.kotlin("SomeServiceProvider2.kt", """
            package test
            
            import ca.solostudios.kspservice.annotation.Service
           
            @Service(SomeService::class)
            class SomeServiceProvider2 : SomeService
        """)
        sourceFiles += SourceFile.kotlin("Enclosing.kt", """
            package test
            
            import ca.solostudios.kspservice.annotation.Service
            
            class Enclosing {
                @Service(SomeService::class)
                class NestedSomeServiceProvider : SomeService
            }
        """)
        sourceFiles += SourceFile.kotlin("AnotherService.kt", """
            package test
            
            interface AnotherService
        """)
        sourceFiles += SourceFile.kotlin("AnotherServiceProvider.kt", """
            package test
            
            import ca.solostudios.kspservice.annotation.Service
            
            @Service(AnotherService::class)
            class AnotherServiceProvider : AnotherService
        """)
        
        val compilation = KotlinCompilation().apply {
            sources = sourceFiles
            inheritClassPath = true
            symbolProcessorProviders = listOf(KSPServiceProcessor.Provider())
            kspIncremental = incremental
        }
        val result = compilation.compile()
        assertEquals(ExitCode.OK, result.exitCode)
        
        val generatedSourcesDir = compilation.kspSourcesDir
        val someServiceFile = File(generatedSourcesDir, "resources/META-INF/services/test.SomeService")
        val someServiceContents = """
            test.Enclosing${'$'}NestedSomeServiceProvider
            test.SomeServiceProvider1
            test.SomeServiceProvider2
            
        """.trimIndent()
        val anotherServiceFile = File(generatedSourcesDir, "resources/META-INF/services/test.AnotherService")
        val anotherServiceContents = """
            test.AnotherServiceProvider
            
        """.trimIndent()
        
        assertTrue(someServiceFile.exists())
        assertTrue(anotherServiceFile.exists())
        
        assertEquals(someServiceContents, someServiceFile.readText())
        assertEquals(anotherServiceContents, anotherServiceFile.readText())
    }
    
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testMultiService(incremental: Boolean) {
        val sourceFiles = mutableListOf<SourceFile>()
        sourceFiles += SourceFile.kotlin("SomeService.kt", """
           package test
           
           interface SomeService
        """)
        sourceFiles += SourceFile.kotlin("AnotherService.kt", """
            package test
            
            interface AnotherService
        """)
        sourceFiles += SourceFile.kotlin("MultiServiceProvider.kt", """
            package test
            
            import ca.solostudios.kspservice.annotation.Service
            
            @Service(SomeService::class, AnotherService::class) 
            class MultiServiceProvider : SomeService, AnotherService
        """)
        
        val compilation = KotlinCompilation().apply {
            sources = sourceFiles
            inheritClassPath = true
            symbolProcessorProviders = listOf(KSPServiceProcessor.Provider())
            kspIncremental = incremental
        }
        val result = compilation.compile()
        assertEquals(ExitCode.OK, result.exitCode)
        
        val generatedSourcesDir = compilation.kspSourcesDir
        val someServiceFile = File(generatedSourcesDir, "resources/META-INF/services/test.SomeService")
        val someServiceContents = """
            test.MultiServiceProvider
            
        """.trimIndent()
        val anotherServiceFile = File(generatedSourcesDir, "resources/META-INF/services/test.AnotherService")
        val anotherServiceContents = """
            test.MultiServiceProvider
            
        """.trimIndent()
        
        assertTrue(someServiceFile.exists())
        assertTrue(anotherServiceFile.exists())
        
        assertEquals(someServiceContents, someServiceFile.readText())
        assertEquals(anotherServiceContents, anotherServiceFile.readText())
    }
    
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testBadMultiService(incremental: Boolean) {
        val sourceFiles = mutableListOf<SourceFile>()
        sourceFiles += SourceFile.kotlin("NoServices.kt", """
           package test
           
           import ca.solostudios.kspservice.annotation.Service
           
           @Service()
           class NoServices
        """)
        
        val compilation = KotlinCompilation().apply {
            sources = sourceFiles
            inheritClassPath = true
            symbolProcessorProviders = listOf(KSPServiceProcessor.Provider())
            kspIncremental = incremental
        }
        val result = compilation.compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
    }
    
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testGenericService(incremental: Boolean) {
        val sourceFiles = mutableListOf<SourceFile>()
        sourceFiles += SourceFile.kotlin("GenericService.kt", """
           package test
           
           interface GenericService<T>
        """)
        sourceFiles += SourceFile.kotlin("GenericServiceProvider.kt", """
            package test
            
            import ca.solostudios.kspservice.annotation.Service
            
            @Service(GenericService::class)
            class GenericServiceProvider<T> : GenericService<T>
        """)
        
        val compilation = KotlinCompilation().apply {
            sources = sourceFiles
            inheritClassPath = true
            symbolProcessorProviders = listOf(KSPServiceProcessor.Provider())
            kspIncremental = incremental
        }
        val result = compilation.compile()
        assertEquals(ExitCode.OK, result.exitCode)
        
        val generatedSourcesDir = compilation.kspSourcesDir
        
        val genericServiceFile = File(generatedSourcesDir, "resources/META-INF/services/test.GenericService")
        val genericServiceContents = """
            test.GenericServiceProvider
            
        """.trimIndent()
        
        assertTrue(genericServiceFile.exists())
        
        assertEquals(genericServiceContents, genericServiceFile.readText())
    }
    
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testMissingService(incremental: Boolean) {
        val sourceFiles = mutableListOf<SourceFile>()
        sourceFiles += SourceFile.kotlin("GenericServiceProvider.kt", """
            package test
            
            import ca.solostudios.kspservice.annotation.Service
            
            @Service(MissingService::class)
            class GenericMissingServiceProvider<T> : MissingService<T>
        """)
        
        val compilation = KotlinCompilation().apply {
            sources = sourceFiles
            inheritClassPath = true
            symbolProcessorProviders = listOf(KSPServiceProcessor.Provider())
            kspIncremental = incremental
        }
        val result = compilation.compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
    }
    
}
