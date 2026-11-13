// The first run (PLANNING.md §2, §11 Phase 7): the welcome, the game language, the level for a
// foreign one, then the guided first game, which is :feature:game's.
plugins {
    alias(libs.plugins.thatstheword.android.feature)
}

android {
    namespace = "com.callbackdev.thatstheword.feature.onboarding"
}

dependencies {
    // The system's back, page by page through the run.
    implementation(libs.androidx.activity.compose)

    testImplementation(project(":core:words"))
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
