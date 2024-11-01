package ca.solostudios.kspservice.kotest

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.names.DuplicateTestNameMode
import io.kotest.core.names.TestNameCase
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.core.test.TestCaseOrder
import io.kotest.extensions.allure.AllureTestReporter
import io.kotest.extensions.htmlreporter.HtmlReporter
import io.kotest.extensions.junitxml.JunitXmlReporter

class KotestConfig : AbstractProjectConfig() {
    override val parallelism = 1

    override val testCaseOrder = TestCaseOrder.Sequential
    override val duplicateTestNameMode = DuplicateTestNameMode.Error
    override val testNameCase = TestNameCase.InitialLowercase
    override val specExecutionOrder = SpecExecutionOrder.Lexicographic

    override val isolationMode = IsolationMode.InstancePerLeaf

    override fun extensions() = listOf(
        AllureTestReporter(
            includeContainers = false
        ),
        JunitXmlReporter(
            includeContainers = false,
            useTestPathAsName = true,
            outputDir = "test-results/$taskName"
        ),
        HtmlReporter(
            outputDir = "reports/tests/$taskName"
        )
    )

    private val taskName: String
        get() = System.getProperty("gradle.task.name")
}
