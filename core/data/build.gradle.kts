// What the player did and chose (PLANNING.md §5): the games, their moves, the notebook and the
// milestones in Room, the settings in DataStore, and the words' files opened once for the app.
// The repositories speak the domain's types; nothing above this module sees an entity.
plugins {
    alias(libs.plugins.thatstheword.android.library)
    alias(libs.plugins.thatstheword.android.hilt)
    alias(libs.plugins.thatstheword.android.room)
}

android {
    namespace = "com.callbackdev.thatstheword.core.data"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:words"))

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
