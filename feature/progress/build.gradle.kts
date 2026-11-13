// The progress page (PLANNING.md §2, §11 Phase 6): the statistics of each language and mode, the
// distribution of the attempts, the hints and Hard counted apart, the streaks and the milestones.
plugins {
    alias(libs.plugins.thatstheword.android.feature)
}

android {
    namespace = "com.callbackdev.thatstheword.feature.progress"
}

dependencies {
    testImplementation(libs.androidx.junit)
}
