import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType


internal class AndroidComposeApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            with(pluginManager) {
                apply(libs.findPlugin("kotlinCompose").get().get().pluginId)
            }
            extensions.configure<ApplicationExtension> {
                defaultConfig {
                    targetSdk = 35
                    applicationId = namespace // Equal name for namespace and applicationId
                }

                buildFeatures {
                    compose = true
                }

                composeOptions {
                    kotlinCompilerExtensionVersion = "1.5.15"
                }

                packaging {
                    resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }

            dependencies {
                "api"(platform(libs.findLibrary("compose-bom").get()))
                "implementation"(libs.findBundle("compose").get())
            }

        }
    }
}
