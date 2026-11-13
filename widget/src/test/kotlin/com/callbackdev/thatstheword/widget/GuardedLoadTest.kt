package com.callbackdev.thatstheword.widget

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

/** A card must always reach `provideContent`: Glance shows its spinner until it does. */
class GuardedLoadTest {
    private val sample = WidgetSamples.model()

    @Test
    fun `a read that works is the card`() = runTest {
        assertThat(guardedLoad(1_000, "test") { sample }).isEqualTo(sample)
    }

    @Test
    fun `a read that fails is a card that says so`() = runTest {
        assertThat(guardedLoad(1_000, "test") { error("the database is gone") }.today).isNull()
    }

    @Test
    fun `a read that never ends is a card that says so, in time`() = runTest {
        assertThat(guardedLoad(1_000, "test") { awaitCancellation() }.today).isNull()
    }

    @Test
    fun `cancellation is not swallowed`() {
        assertThrows(CancellationException::class.java) {
            runBlocking { guardedLoad(1_000, "test") { throw CancellationException("gone") } }
        }
    }
}
