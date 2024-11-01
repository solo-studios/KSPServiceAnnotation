package ca.solostudios.kspservice.annotation

import java.util.ServiceLoader
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * An annotation that marks a class as providing a service.
 *
 * This is used to that the KSP annotation processor can be used to
 * generate the required configuration files for the class to be loaded
 * with [ServiceLoader]
 *
 * Note: The annotated class must conform with the service loader
 * requirements.
 *
 * @property value The services this class provides.
 */
@MustBeDocumented
@Retention(BINARY)
@Target(CLASS)
public annotation class Service(
    vararg val value: KClass<*>
)
