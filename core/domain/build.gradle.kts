// Pure Kotlin/JVM: the game engine (PLANNING.md §4). The folding, the marks, Hard, the hints, the
// daily's number and word, free play's draw, streaks, statistics, milestones, the share text, the
// words' decoder and the backup file, every one tested on the JVM. No Android: if a class here
// needs a `Context`, it is in the wrong module.
plugins {
    alias(libs.plugins.thatstheword.jvm.library)
    // The backup file (Phase 6): kotlinx.serialization, already the shell's for its routes, pure
    // Kotlin like the rest of this module (PLANNING.md §1).
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.serialization.json)
}
