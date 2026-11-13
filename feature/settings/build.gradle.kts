// Settings (PLANNING.md §2). Phase 6 brings its first page, your data: a copy of the games, the
// notebook and the milestones saved to a file, and one brought back (PLANNING.md §5). Phase 7 adds
// the settings around it.
plugins {
    alias(libs.plugins.thatstheword.android.feature)
}

android {
    namespace = "com.callbackdev.thatstheword.feature.settings"
}

dependencies {
    // The file pickers of the Storage Access Framework: no storage permission.
    implementation(libs.androidx.activity.compose)

    testImplementation(project(":core:words"))
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
