// The Material 3 theme, typography, motion and the shared components (PLANNING.md §2): the
// family's design language (docs/adr/0002-design-language.md), with the game's own tiles.
plugins {
    alias(libs.plugins.thatstheword.android.library)
    alias(libs.plugins.thatstheword.android.compose)
}

android {
    namespace = "com.callbackdev.thatstheword.core.designsystem"
}

dependencies {
    // The settings the theme reads (the dress, the face).
    api(project(":core:model"))

    // The BOM is exported with the libraries, so every module that reaches Compose through this
    // one resolves the same versions.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.androidx.junit)
}
