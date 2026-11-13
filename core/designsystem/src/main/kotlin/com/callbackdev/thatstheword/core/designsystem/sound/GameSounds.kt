package com.callbackdev.thatstheword.core.designsystem.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.callbackdev.thatstheword.core.designsystem.R
import com.callbackdev.thatstheword.core.model.Mark
import java.util.concurrent.ConcurrentHashMap

/**
 * The game's sounds (PLANNING.md §7.5), written by `tools/make_sounds.py`: a letter set, a tile
 * revealed (a note a mark: a row plays its result as a short phrase), a word refused, a word found,
 * a milestone.
 */
enum class GameSound(@param:RawRes val res: Int) {
    KEY(R.raw.sound_key),
    RIGHT_PLACE(R.raw.sound_right),
    ELSEWHERE(R.raw.sound_elsewhere),
    ABSENT(R.raw.sound_absent),
    REFUSAL(R.raw.sound_refusal),
    WIN(R.raw.sound_win),
    MILESTONE(R.raw.sound_milestone),
    ;

    companion object {
        fun of(mark: Mark): GameSound = when (mark) {
            Mark.RIGHT_PLACE -> RIGHT_PLACE
            Mark.ELSEWHERE -> ELSEWHERE
            Mark.ABSENT -> ABSENT
        }
    }
}

/**
 * The sounds, decoded into a [SoundPool] for as long as this object lives: the game page makes one
 * when it comes on screen and releases it when it leaves (PLANNING.md §9). A sound not decoded yet
 * is skipped, never waited for: no sound is information.
 *
 * Silent whenever the phone is on silent or vibrate (the owner, PLANNING §15, Open 13): the
 * ringer's mode is read at each sound, so a phone switched to silent mid-game is obeyed at once.
 */
class GameSounds(context: Context) {
    private val audio: AudioManager? = context.getSystemService(AudioManager::class.java)
    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    private val pool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
        .apply { setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id } }
    private val ids = GameSound.entries.associateWith { pool.load(context, it.res, 1) }

    fun play(sound: GameSound) {
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val id = ids.getValue(sound)
        if (id in loaded) pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        loaded.clear()
        pool.release()
    }

    private companion object {
        /** A row's five notes overlap as they ring out; six leaves room for a key typed meanwhile. */
        const val MAX_STREAMS = 6
    }
}

/**
 * The sounds for a page, while it is started: decoded at its start, released at its stop, and none
 * at all while [enabled] is off (the in-app switch, VISION §12). The function it returns plays a
 * sound, or nothing when there is none to play.
 */
@Composable
fun rememberGameSounds(enabled: Boolean): (GameSound) -> Unit {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val holder = remember { arrayOfNulls<GameSounds>(1) }
    DisposableEffect(owner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (enabled && holder[0] == null) holder[0] = GameSounds(context)

                Lifecycle.Event.ON_STOP -> {
                    holder[0]?.release()
                    holder[0] = null
                }

                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            holder[0]?.release()
            holder[0] = null
        }
    }
    val on by rememberUpdatedState(enabled)
    return remember { { sound -> if (on) holder[0]?.play(sound) } }
}
