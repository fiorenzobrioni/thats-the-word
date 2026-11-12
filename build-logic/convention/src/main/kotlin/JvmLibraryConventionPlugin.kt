import com.callbackdev.onmytongue.buildlogic.OnMyTongueJavaVersion
import com.callbackdev.onmytongue.buildlogic.configureKotlinCompile
import com.callbackdev.onmytongue.buildlogic.library
import com.callbackdev.onmytongue.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * A pure Kotlin/JVM module: `core:model` and `core:domain`. No Android on the classpath, so a
 * `Context` or a `Resources` cannot sneak in: if a class needs one, it is in the wrong module.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            // So that `:app:lintDebug` (checkDependencies) analyzes these sources too.
            pluginManager.apply("com.android.lint")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = OnMyTongueJavaVersion
                targetCompatibility = OnMyTongueJavaVersion
            }
            configureKotlinCompile()

            dependencies {
                add("testImplementation", libs.library("junit"))
                add("testImplementation", libs.library("truth"))
                add("testImplementation", libs.library("turbine"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}
