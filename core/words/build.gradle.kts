// The words' files in the APK's assets (PLANNING.md §6.6), written by tools/words/ and never by hand,
// mapped where they lie and handed to the decoder in :core:domain. Nothing here decides a word.
plugins {
    alias(libs.plugins.thatstheword.android.library)
}

android {
    namespace = "com.callbackdev.thatstheword.core.words"
}

dependencies {
    api(project(":core:domain"))

    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
}
