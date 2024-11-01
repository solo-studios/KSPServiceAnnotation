package ca.solostudios.kspservice.util

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@ExperimentalCompilerApi
fun kotlinCompilation(block: KotlinCompilation.() -> Unit): KotlinCompilation {
    return KotlinCompilation().apply(block)
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
