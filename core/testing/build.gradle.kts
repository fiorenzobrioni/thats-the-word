// What the UI tests share (PLANNING.md §12): the accessibility checks every screen's tests run,
// and the page walk at twice the text size and on an open foldable. Test code only: modules take
// it as `testImplementation`, and nothing here reaches the app.
plugins {
    alias(libs.plugins.thatstheword.android.library)
    alias(libs.plugins.thatstheword.android.compose)
}

android {
    namespace = "com.callbackdev.thatstheword.core.testing"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.truth)
    implementation(project(":core:designsystem"))

    testImplementation(libs.androidx.junit)
}
