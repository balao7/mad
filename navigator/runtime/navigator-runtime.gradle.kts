plugins {
    alias(libs.plugins.fgp.android)
    alias(libs.plugins.poko)
    alias(libs.plugins.fgp.publish)
}

// workaround for Gradle/Studio not being able to differentiate between
// :whetstone:runtime* and :navigator:runtime* if the group is the same
// has no effect on publishing
project.group = "navigator"

freeletics {
    explicitApi()
    optIn("com.freeletics.mad.navigator.internal.InternalNavigatorApi")
    enableParcelize()
}

android {
    defaultConfig {
        consumerProguardFile(project.file("navigator.pro"))
    }
}

dependencies {
    api(libs.androidx.activity)
    api(libs.coroutines.core)
    api(libs.androidx.core)

    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.viewmodel.savedstate)
    implementation(libs.kotlin.parcelize)
    implementation(libs.uri)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.lifecycle.testing)
    testImplementation(libs.coroutines.test)
}
