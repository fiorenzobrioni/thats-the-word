package com.callbackdev.thatstheword.core.words

import android.content.res.AssetManager
import com.callbackdev.thatstheword.core.domain.words.WordFiles
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * The words' files as the APK holds them (`assets/words/`), mapped, not copied (PLANNING.md §9,
 * rule 4): the APK stores them uncompressed (`noCompress` in `:app`), so each is a range of the
 * APK the system pages in as a lookup touches it, and nothing is written to storage.
 *
 * A file the packaging compressed after all (a test's assets) cannot be mapped: it is read whole
 * into memory instead, once, so the game still works, only heavier.
 */
class AssetWordFiles(private val assets: AssetManager) : WordFiles {
    override fun open(path: String): ByteBuffer {
        val name = "$DIRECTORY/$path"
        val mapped = try {
            assets.openFd(name).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.length)
                }
            }
        } catch (_: IOException) {
            null
        }
        return mapped ?: assets.open(name).use { ByteBuffer.wrap(it.readBytes()) }
    }

    companion object {
        const val DIRECTORY = "words"
    }
}
