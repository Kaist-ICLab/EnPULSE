import com.android.build.api.dsl.ApplicationExtension
import kaist.iclab.convention.buildlogic.configureKotlinAndroid
import kaist.iclab.convention.buildlogic.configureKotlinCompilerOption
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension


internal class AndroidBasicApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            with(pluginManager) {
                apply(libs.findPlugin("androidApplication").get().get().pluginId)
                apply(libs.findPlugin("jetbrainsKotlinAndroid").get().get().pluginId)
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)

            }
            extensions.configure<KotlinAndroidProjectExtension> {
                configureKotlinCompilerOption(this)
            }

            dependencies {
                "implementation"(libs.findBundle("androidx").get())
            }
        }
    }
}
