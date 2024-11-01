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
            logger.error("@Service type was not found on the classpath.")
            return emptyList()
        }

        resolver.getSymbolsWithAnnotation(SERVICE_ANNOTATION_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { serviceClassDeclaration ->
                val annotation = serviceClassDeclaration.annotations.find { it.annotationType.resolve() == serviceType }

                if (annotation == null) {
                    logger.error("@Service annotation not found", serviceClassDeclaration)
                    return@forEach
                }

                val argument = annotation.arguments
                    .find { it.name?.getShortName() == "value" }

                val argumentValue = argument?.value

                if (argumentValue == null) {
                    logger.error("@Service value was null when it should not be.", argument)
                    return@forEach
                }

                val serviceInterfaces = try {
                    @Suppress("UNCHECKED_CAST")
                    argumentValue as? List<KSType> ?: listOf(argumentValue as KSType)
                } catch (e: ClassCastException) {
                    logger.error("The 'value' property was found in the annotation, but it is the wrong type.", argument)
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

                processServiceInterfaces(serviceClassDeclaration, serviceInterfaces)
            }

        generateConfigFiles()

        return emptyList()
    }

    private fun processServiceInterfaces(serviceImplementation: KSClassDeclaration, serviceInterfaces: List<KSType>) {
        for (serviceInterface in serviceInterfaces) {
            val serviceDeclaration = serviceInterface.declaration.closestClassDeclaration()

            when {
                serviceDeclaration == null                                       -> {
                    logger.error("The declaration of the inherited service is null when it should not be.", serviceInterface.declaration)
                }

                serviceImplementation.failsServiceVerification(serviceInterface) -> {
                    logger.error(buildString {
                        append("Classes annotated with @Service must implement the appropriate service interface. ")
                        append(serviceImplementation.qualifiedName)
                        append(" does not implement ")
                        append(serviceDeclaration.qualifiedName)
                    }, serviceImplementation)
                }

                else                                                             -> {
                    val serviceImplementors = services[serviceDeclaration.toBinaryName()]
                        ?: mutableSetOf<String>().also { services[serviceDeclaration.toBinaryName()] = it }

                    serviceImplementors.add(serviceImplementation.toBinaryName())

                    serviceFiles[serviceImplementation.toBinaryName()] = serviceImplementation.containingFile!!
                }
            }
        }
    }

    private fun generateConfigFiles() {
        for (serviceInterface in services.keys) {
            val resourceFile = "META-INF/services/$serviceInterface"

            logger.verbose { "Working on resource file $resourceFile" }

            try {
                val serviceImplementors = services[serviceInterface]!!
                val serviceFiles = serviceImplementors.mapNotNull { serviceFiles[it] }

                if (serviceImplementors.isEmpty()) {
                    logger.verbose { "Skipping writing '$resourceFile', as service list is empty" }
                    return
                }

                logger.verbose {
                    val joinedImplementors = serviceImplementors.joinToString(
                        separator = ",",
                        prefix = "(",
                        postfix = ")"
                    )

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
            } catch (e: IOException) {
                logger.error("Unable to write to $resourceFile: $e")
            }
        }

        services.clear()
    }

    private fun KSPLogger.verbose(message: () -> String) {
        if (verbose)
            logging(message())
    }

    private fun KSClassDeclaration.failsServiceVerification(serviceType: KSType): Boolean {
        return if (verify)
            this.getAllSuperTypes().any { it.isAssignableFrom(serviceType) }
        else
            false
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

    companion object {
        val SERVICE_ANNOTATION_NAME = Service::class.qualifiedName!!
    }
}
