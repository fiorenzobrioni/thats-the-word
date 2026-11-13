package com.callbackdev.thatstheword.core.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The checks themselves: each finds what it is for, and lets pass what is meant. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityChecksTest {
    @get:Rule val compose = createComposeRule()

    private val sentence = "A sentence long enough to need three lines in so narrow a box"

    @Test
    fun `a text in a box too short for it is cut`() {
        compose.setContent { Box(Modifier.width(120.dp).height(20.dp)) { Text(sentence) } }
        val failure = runCatching { compose.assertAccessible() }.exceptionOrNull()
        assertThat(failure).hasMessageThat().contains("is cut")
    }

    @Test
    fun `a text with a few lines and an ellipsis, on purpose, is not`() {
        compose.setContent {
            Box(Modifier.width(120.dp)) { Text(sentence, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        compose.assertAccessible()
    }

    @Test
    fun `a text given its room is not`() {
        compose.setContent { Box(Modifier.width(120.dp)) { Text(sentence) } }
        compose.assertAccessible()
    }

    @Test
    fun `a page without a heading is refused by the walk`() {
        compose.setContent { Text(sentence) }
        val failure = runCatching { compose.walkPage(isRoot(), "no-heading") }.exceptionOrNull()
        assertThat(failure).hasMessageThat().contains("heading")
    }

    @Test
    fun `a page with one is walked`() {
        compose.setContent { Text(sentence, Modifier.semantics { heading() }) }
        compose.walkPage(isRoot(), "a-heading")
    }
}
