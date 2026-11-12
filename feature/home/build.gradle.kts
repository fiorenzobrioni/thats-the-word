// The start page (PLANNING.md §2, §11 Phase 4): today's word in each language, the quick switch of
// the game language, the streak, the calendar of the dailies with the archive's games, free play
// with its level, and the Hard option.
plugins {
    alias(libs.plugins.thatstheword.android.feature)
}

android {
    namespace = "com.callbackdev.thatstheword.feature.home"
}

dependencies {
    // The tests read the real words' files, as the start page does.
    testImplementation(project(":core:words"))
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
