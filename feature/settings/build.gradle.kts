// Settings (PLANNING.md §2): the page of choices (Phase 7: the game, the languages, the daily
// reminder, sound, touch and motion, the appearance, about and the licences) and Your data (Phase
// 6: a copy of the games, the notebook and the milestones saved to a file, and one brought back).
plugins {
    alias(libs.plugins.thatstheword.android.feature)
}

android {
    namespace = "com.callbackdev.thatstheword.feature.settings"
}

dependencies {
    // The daily reminder's state on screen: whether its notification can show, and its alarm told
    // when that may have changed.
    implementation(project(":core:reminder"))
    // The file pickers of the Storage Access Framework: no storage permission.
    implementation(libs.androidx.activity.compose)

    testImplementation(project(":core:words"))
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
