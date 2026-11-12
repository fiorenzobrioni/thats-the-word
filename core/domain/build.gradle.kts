// Pure Kotlin/JVM: the game engine (PLANNING.md §4). The folding, the marks, Hard, the hints, the
// daily's number and word, free play's draw, streaks, statistics, milestones, the share text and
// the words' decoder, every one tested on the JVM. No Android: if a class here needs a `Context`,
// it is in the wrong module.
plugins {
    alias(libs.plugins.thatstheword.jvm.library)
}

dependencies {
    api(project(":core:model"))
}
