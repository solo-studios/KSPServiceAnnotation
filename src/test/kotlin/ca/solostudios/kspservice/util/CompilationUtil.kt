package ca.solostudios.kspservice.util

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.property.Exhaustive
import io.kotest.property.exhaustive.of
import io.kotest.property.exhaustive.plus
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

val ksp1LanguageVersions = Exhaustive.of("1.7", "1.8", "1.9")

val ksp2LanguageVersions = ksp1LanguageVersions + Exhaustive.of("2.0")

@ExperimentalCompilerApi
fun kotlinCompilation(block: KotlinCompilation.() -> Unit): KotlinCompilation {
    return KotlinCompilation().apply {
        verbose = false
    }.apply(block)
}

@ExperimentalCompilerApi
fun JvmCompilationResult.shouldBeSuccess() {
    exitCode should succeed()
}

@ExperimentalCompilerApi
fun JvmCompilationResult.shouldBeError() {
    exitCode shouldNot succeed()
}

@ExperimentalCompilerApi
fun succeed(): Matcher<ExitCode> = object : Matcher<ExitCode> {
    override fun test(value: ExitCode): MatcherResult {
        return MatcherResult(
            value == ExitCode.OK,
            { "compilation should have succeeded, but was $value" },
            { "compilation should have failed, but was $value" }
        )
    }
}
