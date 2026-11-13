package com.callbackdev.thatstheword.feature.game.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.callbackdev.thatstheword.core.designsystem.components.inkButtonColors
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.domain.share.ShareText
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.feature.game.R
import com.callbackdev.thatstheword.feature.game.languageName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.format.DateTimeFormatter

/**
 * Sharing a result (PLANNING.md §7.6, VISION §11): the picture, shown as it will be sent, and two
 * ways to send it: as the picture, through a `FileProvider` (no permission), or as the text any
 * messaging app takes. Both without the word. The system's chooser picks where it goes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareSheet(facts: ResultFacts, text: () -> String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reduced = reducedMotion()
    val spec = pictureSpec(facts)
    val chooserTitle = stringResource(R.string.end_share)
    var picture by remember(spec) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(spec) {
        picture = withContext(Dispatchers.Default) { ResultPicture.draw(context, spec) }
    }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, modifier = Modifier.testTag(ShareTags.SHEET)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.end_share),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.share_sheet_line),
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            val description = stringResource(R.string.share_picture_description)
            val frame = RoundedCornerShape(20.dp)
            // The picture at its own proportions, as large as the sheet allows on a phone held upright.
            Box(
                Modifier
                    .heightIn(max = PREVIEW_HEIGHT)
                    .aspectRatio(ResultPicture.WIDTH / ResultPicture.HEIGHT.toFloat())
                    .clip(frame)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, frame),
            ) {
                AnimatedContent(
                    targetState = picture,
                    transitionSpec = {
                        fadeIn(ThatsTheWordMotion.effects(reduced)) togetherWith
                            fadeOut(ThatsTheWordMotion.effects(reduced))
                    },
                    label = "picture",
                ) { shown ->
                    if (shown != null) {
                        Image(
                            bitmap = remember(shown) { shown.asImageBitmap() },
                            contentDescription = description,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(ShareTags.PICTURE),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        val shown = picture ?: return@Button
                        scope.launch {
                            val sent = withContext(Dispatchers.IO) { sharePicture(context, shown, chooserTitle) }
                            if (sent) onDismiss()
                        }
                    },
                    enabled = picture != null,
                    colors = inkButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag(ShareTags.PICTURE_BUTTON),
                ) {
                    Icon(GameIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.share_picture), style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = {
                        val shared = text() ?: return@OutlinedButton
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shared)
                        runCatching { context.startActivity(Intent.createChooser(send, chooserTitle)) }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag(ShareTags.TEXT_BUTTON),
                ) {
                    Text(stringResource(R.string.share_as_text), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * The picture's words, in the interface's language (PLANNING.md §7.6): the name in its drawn
 * capitals, the language with the daily's number or free play's name, the date in the locale's own
 * order, the score as a shared result says it, Hard and the hints.
 */
@Composable
internal fun pictureSpec(facts: ResultFacts): PictureSpec {
    val locale = LocalConfiguration.current.locales[0]
    val resources = LocalResources.current
    val language = stringResource(languageName(facts.language))
    val eyebrow = when (val mode = facts.mode) {
        is GameMode.Daily -> stringResource(R.string.game_daily_eyebrow, language, mode.number)
        is GameMode.Free -> stringResource(R.string.share_free_eyebrow, language, stringResource(R.string.game_free))
        GameMode.Guided -> stringResource(R.string.share_free_eyebrow, language, stringResource(R.string.game_guided))
    }
    val name = stringResource(R.string.share_app_name)
    val hard = stringResource(R.string.share_hard)
    val date = remember(facts.date, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMMy"), locale)
            .format(facts.date)
            .replaceFirstChar { it.titlecase(locale) }
    }
    val extras = buildList {
        if (facts.hard) add(hard)
        if (facts.hints > 0) add(resources.getQuantityString(R.plurals.share_hints, facts.hints, facts.hints))
    }.takeIf { it.isNotEmpty() }?.joinToString(ShareText.SEPARATOR)
    return remember(facts, locale, name, eyebrow, date, extras) {
        PictureSpec(
            name = name.uppercase(locale),
            eyebrow = eyebrow.uppercase(locale),
            date = date,
            rows = facts.rows,
            hintRows = facts.hints,
            length = facts.length,
            attempts = facts.attempts,
            score = "${if (facts.won) facts.attemptsUsed.toString() else ShareText.LOST}/${facts.attempts}",
            extras = extras,
        )
    }
}

/**
 * The picture written to the cache's `shared/` (the only folder the `FileProvider` serves, never in
 * a backup) and handed to the system's chooser with a read grant for that one file. False when it
 * could not be written or no app can take it.
 */
private fun sharePicture(context: Context, picture: Bitmap, title: String): Boolean = try {
    val folder = File(context.cacheDir, SHARED_FOLDER).apply { mkdirs() }
    val file = File(folder, PICTURE_FILE)
    file.outputStream().use { picture.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(PICTURE_FILE, uri)
    context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: IOException) {
    false
} catch (_: IllegalArgumentException) {
    false
} catch (_: android.content.ActivityNotFoundException) {
    false
}

/** What the tests find the sheet's parts by. */
object ShareTags {
    const val SHEET = "share_sheet"
    const val PICTURE = "share_picture"
    const val PICTURE_BUTTON = "share_picture_button"
    const val TEXT_BUTTON = "share_text_button"
}

/** The `FileProvider`'s authority is the package's own, so the debug build's is its own too. */
private const val AUTHORITY_SUFFIX = ".share"
private const val SHARED_FOLDER = "shared"
private const val PICTURE_FILE = "thats-the-word.png"
private val PREVIEW_HEIGHT = 380.dp
