// The home-screen card (Jetpack Glance, PLANNING.md §7.8, Phase 7b): today's word in each
// language and the streak, on the family's card; its receiver, its settings screen with the real
// card as its preview, and what keeps it fresh (a game saved, the non-wakeup midnight alarm, the
// exempt broadcasts). The card, its inks and its colours: docs/adr/0004-home-screen-card.md.
plugins {
    alias(libs.plugins.thatstheword.android.library)
    alias(libs.plugins.thatstheword.android.compose)
    alias(libs.plugins.thatstheword.android.hilt)
}

android {
    namespace = "com.callbackdev.thatstheword.widget"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    testImplementation(project(":core:testing"))
    testImplementation(project(":core:words"))

    implementation(libs.androidx.glance.appwidget)
    // Glance runs its sessions on WorkManager; named here so the version pinned in the catalog is
    // the one that resolves (PLANNING.md §1).
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    // The loader's test reads a real database and settings file, as the app does.
    testImplementation(libs.androidx.room.runtime)
}
