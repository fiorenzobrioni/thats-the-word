// The notebook (PLANNING.md §2, §11 Phase 5): the words met, with their meanings in the interface's
// language, searchable as a small dictionary.
plugins {
    alias(libs.plugins.thatstheword.android.feature)
}

android {
    namespace = "com.callbackdev.thatstheword.feature.notebook"
}

dependencies {
    // The tests read the real words' files, as the notebook does.
    testImplementation(project(":core:words"))
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.runtime)
}
