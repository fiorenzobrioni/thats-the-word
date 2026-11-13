package com.callbackdev.thatstheword

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The hand-written Baseline Profile (`src/main/baseline-prof.txt`, PLANNING.md §15 "Phase 8") is
 * read by AGP without a word when a rule matches nothing: a package moved or renamed would leave
 * its code out of the profile in silence. Every rule must be well formed and name a package, or a
 * class prefix, that still holds the game's code.
 */
class ProfileRulesTest {
    private val root = File("..").canonicalFile
    private val rules = File("src/main/baseline-prof.txt").readLines()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") }

    @Test
    fun `every rule is in the format's own form`() {
        assertThat(rules).isNotEmpty()
        for (rule in rules) {
            assertWithMessage("«$rule»").that(RULE.matches(rule)).isTrue()
        }
    }

    @Test
    fun `every rule names code that is there`() {
        // Every class of the game's own code, as its path: a file's package and each class or
        // object it declares (a file may hold a class under another name), and the file's own class.
        val classes = root.walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.isFile && it.extension == "kt" && "/src/main/kotlin/" in it.invariantSeparatorsPath }
            .flatMap { file ->
                val folder = file.invariantSeparatorsPath.substringAfter("/src/main/kotlin/").substringBeforeLast('/')
                val declared = DECLARATION.findAll(file.readText()).map { it.groupValues[1] }
                (declared + "${file.nameWithoutExtension}Kt").map { "$folder/$it" }
            }
            .toList()
        for (rule in rules) {
            val path = RULE.matchEntire(rule)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() } ?: continue
            // The fixed part before the first wildcard: a package, or a package and a class's start.
            val prefix = path.substringBefore('*')
            val match = when {
                path.endsWith("/**") -> classes.any { it.startsWith(prefix) }

                // One segment: a class directly in the package (`*`), or a class name's start.
                else -> classes.any { it.startsWith(prefix) && '/' !in it.removePrefix(prefix) }
            }
            assertWithMessage("«$rule» names code that is no longer there").that(match).isTrue()
        }
    }

    private companion object {
        /**
         * A method rule, its flags then `L` + a class pattern + `;->` and any method of it; or a class
         * rule, `L` + the pattern + `;`, which takes no flags (AGP refuses the build on one that has).
         */
        val RULE = Regex("""^(?:[HSP]{1,3}L([A-Za-z0-9_/$*]+);->\*\*\(\*\*\)\*\*|L([A-Za-z0-9_/$*]+);)$""")

        /** A class, an object or an interface declared at a line's start, top-level or nested. */
        val DECLARATION = Regex("""(?m)^\s*(?:[a-z]+\s+)*(?:class|object|interface)\s+([A-Z][A-Za-z0-9_]*)""")
    }
}
