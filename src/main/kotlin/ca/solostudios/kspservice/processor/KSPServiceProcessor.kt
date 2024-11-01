package ca.solostudios.kspservice.processor

import ca.solostudios.kspservice.annotation.Service
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import java.io.IOException

internal class KSPServiceProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val codeGenerator = environment.codeGenerator
    private val logger = environment.logger

    private val services: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private val serviceFiles: MutableMap<String, KSFile> = mutableMapOf()

    private val verify = environment.options["kspservice.verify"].toBoolean()
    private val writeComment = (environment.options["kspservice.comment"] ?: "true").toBoolean()
    private val verbose = environment.options["kspservice.verbose"].toBoolean()

    /**
     * - For each class annotated with [Service]
     *    - Verify the [Service] interface value is correct
     *    - Categorize the class by its service interface
     * - For each [Service] interface
     *    - Create a file named `META-INF/services/<interface>`
     *    - For each [Service] annotated class for this interface
     *       - Create an entry in the file
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val serviceType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(SERVICE_ANNOTATION_NAME))
            ?.asType(emptyList())

        if (serviceType == null) {
            logger.error(
                """
                    The @Service annotation could not be found.
                    Expected an annotation with the name $SERVICE_ANNOTATION_NAME.
                    Please add ksp-service-annotation to the compile classpath.
                """.trimIndent()
            )
            return emptyList()
        }

        val deferredDeclarations = mutableListOf<KSClassDeclaration>()

        resolver.getSymbolsWithAnnotation(SERVICE_ANNOTATION_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { serviceClassDeclaration ->
                val annotation = serviceClassDeclaration.annotations.find { it.annotationType.resolve() == serviceType }

                if (annotation == null) {
                    logger.error(
                        """
                            Could not locate the @Service annotation on the element ${serviceClassDeclaration.simpleName.asString()}.
                            Why is it being processed?

                            Please report this error.
                        """.trimIndent(),
                        serviceClassDeclaration
                    )
                    return@forEach
                }

                val argument = annotation.arguments
                    .find { it.name?.getShortName() == "value" }

                val argumentValue = argument?.value

                if (argumentValue == null) {
                    logger.error(
                        """
                            The 'value' property was found in the @Service annotation, but was null.
                            Expected the 'value' property to be non-null.
                        """.trimIndent(),
                        argument
                    )
                    return@forEach
                }

                val serviceInterfaces = try {
                    @Suppress("UNCHECKED_CAST")
                    argumentValue as? List<KSType> ?: listOf(argumentValue as KSType)
                } catch (e: ClassCastException) {
                    logger.error(
                        """
                            The 'value' property was found in the @Service annotation, but it is the wrong type.
                            Expected the 'value' property to be of type KClass<*>.
                        """.trimIndent(),
                        argument
                    )
                    return@forEach
                }

                if (serviceInterfaces.isEmpty()) {
                    logger.error(
                        """
                            No service interfaces specified by @Service annotation!
                            You can provide them in annotation parameters: @Service(YourService::class)
                        """.trimIndent(),
                        argument
                    )
                }

                val deferred = processServiceInterfaces(serviceClassDeclaration, serviceInterfaces)
                if (deferred)
                    deferredDeclarations += serviceClassDeclaration
            }

        generateConfigFiles()

        return deferredDeclarations
    }

    private fun processServiceInterfaces(serviceImplementation: KSClassDeclaration, serviceInterfaces: List<KSType>): Boolean {
        for (serviceInterface in serviceInterfaces) {
            val serviceDeclaration = serviceInterface.declaration.closestClassDeclaration()

            if (serviceInterface.isError)
                return true

            if (serviceDeclaration == null) {
                logger.error(
                    """
                        Cannot locate the class declaration for ${serviceInterface.declaration.simpleName}.
                    """.trimIndent(),
                    serviceInterface.declaration
                )
            } else when (serviceImplementation.failsServiceValidation(serviceInterface)) {
                ValidationResult.VALID    -> {
                    val serviceImplementors = services.getOrPut(serviceDeclaration.toBinaryName()) { mutableSetOf() }

                    serviceImplementors += serviceImplementation.toBinaryName()

                    serviceFiles[serviceImplementation.toBinaryName()] = serviceImplementation.containingFile!! // should never be null
                }

                ValidationResult.INVALID  -> {
                    logger.error(
                        """
                            Classes annotated with @Service must implement the service classe(s)/interface(s).
                            ${serviceImplementation.simpleName} does not implement ${serviceDeclaration.qualifiedName}
                        """.trimIndent(),
                        serviceImplementation
                    )
                }

                ValidationResult.DEFERRED -> return true
            }
        }

        return false
    }

    private fun generateConfigFiles() {
        for (serviceInterface in services.keys) {
            val resourceFile = "META-INF/services/$serviceInterface"

            logger.verbose { "Writing service file for $serviceInterface: $resourceFile" }

            try {
                val serviceImplementors = services[serviceInterface]!!
                val serviceFiles = serviceImplementors.mapNotNull { serviceFiles[it] }

                if (serviceImplementors.isEmpty())
                    return logger.verbose { "Skipping writing '$resourceFile', as there are no services for $serviceInterface" }

                logger.verbose {
                    val joinedImplementors = serviceImplementors.joinToString(separator = ",", prefix = "(", postfix = ")")
                    "Appending services $joinedImplementors to service file '$resourceFile'."
                }

                val dependencies = Dependencies(true, *serviceFiles.toTypedArray())

                codeGenerator.createNewFile(dependencies = dependencies, packageName = "", fileName = resourceFile, extensionName = "")
                    .bufferedWriter().use { writer ->
                        if (writeComment)
                            writer.appendLine(
                                """
                                ###################################################
                                ## Generated by KSP Service Annotation processor ##
                                ###################################################
                            """.trimIndent()
                            )
                        writer.append(serviceImplementors.joinToString(separator = "\n"))
                        if (writeComment)
                            writer.append("\n###################################################")
                    }

                logger.verbose { "Successfully wrote to $resourceFile" }
            } catch (exception: IOException) {
                logger.error(
                    """
                        Unable to write to $resourceFile:
                        $exception
                    """.trimIndent()
                )
            }
        }

        services.clear()
    }

    private fun KSPLogger.verbose(message: () -> String) {
        if (verbose)
            logging(message())
    }

    private fun KSClassDeclaration.failsServiceValidation(serviceType: KSType): ValidationResult {
        if (!verify)
            return ValidationResult.VALID

        val supertypes = getAllSuperTypes()

        if (supertypes.any { it.isAssignableFrom(serviceType) })
            ValidationResult.VALID

        if (supertypes.any { it.isError })
            return ValidationResult.DEFERRED

        return ValidationResult.INVALID
    }

    private fun KSClassDeclaration.toBinaryName(): String = toClassName().reflectionName()

    private fun KSClassDeclaration.toClassName(): ClassName {
        require(!isLocal()) {
            "Local and anonymous classes are not supported."
        }

        val pkgName = packageName.asString()
        val typesString = qualifiedName!!.asString().removePrefix("$pkgName.")

        val simpleNames = typesString.split(".")
        return ClassName(pkgName, simpleNames)
    }

    private enum class ValidationResult {
        VALID,
        INVALID,
        DEFERRED,
    }

    companion object {
        val SERVICE_ANNOTATION_NAME = Service::class.qualifiedName!!
    }
}
