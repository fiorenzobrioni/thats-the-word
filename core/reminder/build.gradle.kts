// The daily reminder's Android half (PLANNING.md §8): its one inexact alarm and the broadcasts
// that re-arm it, the `reminder` channel and its notification. When it comes is :core:domain's
// (`ReminderSchedule`); whether there is anything to say is the games' repository's.
plugins {
    alias(libs.plugins.thatstheword.android.library)
    alias(libs.plugins.thatstheword.android.hilt)
}

android {
    namespace = "com.callbackdev.thatstheword.core.reminder"
}

dependencies {
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:words"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    // DailyReminderTest runs on a real in-memory database and settings file, as the app does.
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.datastore.preferences)
}
