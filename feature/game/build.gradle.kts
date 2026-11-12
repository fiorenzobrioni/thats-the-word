// The game (PLANNING.md §2, §7): the grid, the keyboard with its marks, the refusals' line, the end
// card and sharing; from Phase 5 the hints, from Phase 7 the guided first game.
plugins {
    alias(libs.plugins.thatstheword.android.feature)
}

android {
    namespace = "com.callbackdev.thatstheword.feature.game"
}

dependencies {
    // The tests read the real words' files, as the game does.
    testImplementation(project(":core:words"))
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
