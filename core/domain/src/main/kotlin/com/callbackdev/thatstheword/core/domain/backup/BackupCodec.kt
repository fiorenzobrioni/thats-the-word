package com.callbackdev.thatstheword.core.domain.backup

import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.Move
import com.callbackdev.thatstheword.core.domain.reminder.ReminderSchedule
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.AppFont
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.NotebookEntry
import com.callbackdev.thatstheword.core.model.ThemeMode
import com.callbackdev.thatstheword.core.model.UserSettings
import com.callbackdev.thatstheword.core.model.Word
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** What reading a file came to. */
sealed interface BackupRead {
    data class Ok(val backup: Backup) : BackupRead

    /** Readable or not, not a file That's The Word wrote. */
    data object NotABackup : BackupRead

    /** A backup in a format this version does not know yet: the app needs an update to read it. */
    data class TooNew(val version: Int) : BackupRead

    /** A backup cut short, or saying something no version of the game could have written. */
    data object Damaged : BackupRead
}

/**
 * The backup file (PLANNING.md §5, VISION §11): JSON, UTF-8, one object, indented, so it can be read
 * without the app.
 *
 * - `format` is always `thatstheword-backup`; `version` is [VERSION], raised only for a change an
 *   older reader would misread (a new mode, a fourth language). An added field does not raise it:
 *   a reader skips what it does not know.
 * - A word is its key form in the keys' lower case (`citta`, `año`), as the database keeps it; a
 *   date is ISO (`2026-11-13`), an instant epoch milliseconds; the names of modes, states, hints,
 *   levels and looks are lower case. A game's moves are in the order they were made, each a guess
 *   or a hint.
 *
 * **Read whole and checked before anything is written**: [decode] returns a backup only when every
 * part of it is one the game could have written, so a restore never writes half a file, and never a
 * game that would replay into another. Strict where a value decides a mark, a date or a result (a
 * word, a move, a state, a day's counted game); lenient where a default cannot mislead (a look this
 * build does not know reads as its default; a milestone it does not know is left out).
 */
object BackupCodec {
    const val FORMAT: String = "thatstheword-backup"
    const val VERSION: Int = 1

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    /** The name offered for the file, dated so several backups sort and tell apart. */
    fun fileName(date: LocalDate): String = "thatstheword-backup-$date.json"

    fun encode(backup: Backup): String = json.encodeToString(BackupFile.serializer(), backup.toFile())

    fun decode(text: String): BackupRead {
        // An editor may have put a byte-order mark in front: it says nothing about the content.
        val body = text.removePrefix(BOM)
        val root = try {
            json.parseToJsonElement(body) as? JsonObject ?: return BackupRead.NotABackup
        } catch (_: SerializationException) {
            // Not JSON: a file of something else, or one of the game's cut short on its way here.
            return if (body.contains("\"$FORMAT\"")) BackupRead.Damaged else BackupRead.NotABackup
        }
        val format = runCatching { root["format"]?.jsonPrimitive?.content }.getOrNull()
        if (format != FORMAT) return BackupRead.NotABackup
        val version = runCatching { root["version"]?.jsonPrimitive?.intOrNull }.getOrNull()
            ?: return BackupRead.Damaged
        if (version > VERSION) return BackupRead.TooNew(version)
        if (version < 1) return BackupRead.Damaged
        return try {
            BackupRead.Ok(json.decodeFromJsonElement(BackupFile.serializer(), root).toBackup())
        } catch (_: SerializationException) {
            BackupRead.Damaged
        } catch (_: IllegalArgumentException) {
            BackupRead.Damaged
        } catch (_: IllegalStateException) {
            BackupRead.Damaged
        } catch (_: DateTimeException) {
            BackupRead.Damaged
        }
    }

    private val BOM = Char(0xFEFF).toString()
}

// The file's names, lower case; never renamed: a file written today is read by every version after.
private const val MODE_DAILY = "daily"
private const val MODE_FREE = "free"
private const val MODE_GUIDED = "guided"

private fun name(value: Enum<*>): String = value.name.lowercase(Locale.ROOT)

private inline fun <reified E : Enum<E>> enumOf(name: String?): E? =
    name?.let { n -> enumValues<E>().firstOrNull { it.name.lowercase(Locale.ROOT) == n } }

private fun languageOf(tag: String): Language = requireNotNull(Language.ofTag(tag)) { "No language is $tag" }

private fun wordOf(language: Language, key: String): Word =
    requireNotNull(Alphabet.of(language).parse(key)) { "$key is not a word of $language" }

private fun keyOf(language: Language, word: Word): String = Alphabet.of(language).keyForm(word)

private fun Backup.toFile() = BackupFile(
    format = BackupCodec.FORMAT,
    version = BackupCodec.VERSION,
    exportedAtMillis = exportedAt.toEpochMilli(),
    zone = zone,
    app = appVersion,
    settings = SettingsDto(
        gameLanguage = settings.gameLanguage.tag,
        levels = settings.levels.entries.sortedBy { it.key.ordinal }.associate { (language, level) ->
            language.tag to name(level)
        },
        hard = settings.hard,
        sound = settings.sound,
        haptics = settings.haptics,
        reducedMotion = settings.reducedMotion,
        theme = name(settings.themeMode),
        palette = name(settings.palette),
        font = name(settings.font),
        wallpaperColors = settings.wallpaperColors,
        reminder = settings.reminder,
        reminderTime = settings.reminderTime.toString(),
    ),
    games = games.map { game ->
        GameDto(
            language = game.language.tag,
            interfaceLanguage = game.interfaceLanguage.tag,
            mode = when (game.mode) {
                is GameMode.Daily -> MODE_DAILY
                is GameMode.Free -> MODE_FREE
                GameMode.Guided -> MODE_GUIDED
            },
            date = (game.mode as? GameMode.Daily)?.date?.toString(),
            level = (game.mode as? GameMode.Free)?.level?.let(::name),
            solution = keyOf(game.language, game.solution),
            attempts = game.attempts,
            hard = game.hard,
            status = name(game.state),
            startedAtMillis = game.startedAt.toEpochMilli(),
            startedOn = game.startedOn.toString(),
            finishedAtMillis = game.finishedAt?.toEpochMilli(),
            practice = game.practice,
            moves = game.moves.map { move ->
                when (move) {
                    is Move.Guessed -> MoveDto(guess = keyOf(game.language, move.word))
                    is Move.Hinted -> MoveDto(hint = name(move.kind))
                }
            },
        )
    },
    notebook = notebook.map {
        WordDto(
            language = it.language.tag,
            word = keyOf(it.language, it.word),
            firstMet = it.firstMet.toString(),
            lastMet = it.lastMet.toString(),
            timesMet = it.timesMet,
            solved = it.solved,
            missed = it.missed,
            withHints = it.withHints,
            savedByHand = it.savedByHand,
        )
    },
    milestones = milestones.entries.sortedBy { it.value }.map { (milestone, at) ->
        MilestoneDto(name(milestone), at.toEpochMilli())
    },
)

/** The file as a backup, every part checked: [require] fails it, as damaged, on anything the game could not have written. */
private fun BackupFile.toBackup(): Backup {
    val chosen = settings
    val settings = UserSettings(
        gameLanguage = languageOf(chosen.gameLanguage),
        // A level this build does not know is a level not chosen: Base, as every language starts.
        levels = chosen.levels.mapNotNull { (tag, level) ->
            enumOf<Level>(level)?.let { languageOf(tag) to it }
        }.toMap(),
        hard = chosen.hard,
        sound = chosen.sound,
        haptics = chosen.haptics,
        reducedMotion = chosen.reducedMotion,
        themeMode = enumOf<ThemeMode>(chosen.theme) ?: ThemeMode.SYSTEM,
        palette = enumOf<AppPalette>(chosen.palette) ?: AppPalette.VIVID,
        font = enumOf<AppFont>(chosen.font) ?: AppFont.GOOGLE_SANS,
        wallpaperColors = chosen.wallpaperColors,
        firstRunDone = true,
        tipsSeen = false,
        reminder = chosen.reminder,
        // An hour this build cannot read is the hour not chosen: the reminder's own.
        reminderTime = runCatching { LocalTime.parse(chosen.reminderTime) }.getOrDefault(ReminderSchedule.DEFAULT_TIME),
        // Whether this phone asked for the permission is this phone's: the restoring phone decides.
        askedNotifications = false,
    )
    val games = games.map { it.toGame() }
    checkDays(games)
    val notebook = notebook.map { it.toEntry() }
    require(notebook.map { it.language to it.word }.toSet().size == notebook.size) { "A word kept twice" }
    return Backup(
        exportedAt = Instant.ofEpochMilli(exportedAtMillis),
        zone = zone,
        appVersion = app,
        settings = settings,
        games = games,
        notebook = notebook,
        // A milestone this build does not know is left out: a later version's, said by that version.
        milestones = milestones.mapNotNull { dto ->
            enumOf<Milestone>(dto.id)?.let { it to Instant.ofEpochMilli(dto.unlockedAtMillis) }
        }.toMap(),
    )
}

private fun GameDto.toGame(): BackupGame {
    val language = languageOf(language)
    val solution = wordOf(language, solution)
    val startedOn = LocalDate.parse(startedOn)
    val mode = when (mode) {
        MODE_DAILY -> {
            val date = LocalDate.parse(requireNotNull(date) { "A daily without its date" })
            require(!date.isBefore(DailyCalendar.EPOCH)) { "A daily before the first" }
            // A daily belongs to the date it was started on, or is played later from the archive.
            require(!date.isAfter(startedOn)) { "A daily started before its day" }
            GameMode.Daily(date, DailyCalendar.numberOf(date))
        }

        MODE_FREE -> {
            require(practice == 0) { "Free play replayed as practice" }
            GameMode.Free(requireNotNull(enumOf<Level>(level)) { "Free play without its level" })
        }

        MODE_GUIDED -> {
            require(practice == 0) { "The guided game replayed as practice" }
            GameMode.Guided
        }

        else -> throw IllegalArgumentException("No mode is $mode")
    }
    require(attempts >= 1) { "A game of no attempt" }
    require(practice >= 0) { "A replay numbered below zero" }
    val moves = moves.map { move ->
        when {
            move.guess != null && move.hint == null -> {
                val word = wordOf(language, move.guess)
                require(word.length == solution.length) { "A guess of another length" }
                Move.Guessed(word)
            }

            move.hint != null && move.guess == null ->
                Move.Hinted(requireNotNull(enumOf<HintKind>(move.hint)) { "No hint is ${move.hint}" })

            else -> throw IllegalArgumentException("A move that is neither a guess nor a hint")
        }
    }
    require(moves.size <= attempts) { "More moves than attempts" }
    val hints = moves.filterIsInstance<Move.Hinted>().map { it.kind }
    require(hints.toSet().size == hints.size) { "A hint taken twice" }
    val game = BackupGame(
        language = language,
        interfaceLanguage = languageOf(interfaceLanguage),
        mode = mode,
        solution = solution,
        attempts = attempts,
        hard = hard,
        moves = moves,
        startedAt = Instant.ofEpochMilli(startedAtMillis),
        startedOn = startedOn,
        finishedAt = finishedAtMillis?.let(Instant::ofEpochMilli),
        practice = practice,
    )
    // Hints are only in a language that is not the interface's (VISION §5).
    require(hints.isEmpty() || game.language != game.interfaceLanguage) { "A hint in the interface's language" }
    // No move after the game was over: every shorter replay is still under way.
    require((0 until moves.size).none { game.replay(moves.take(it)).state.finished }) { "A move after the end" }
    val state = game.state
    require(name(state) == status) { "A game that says $status and replays as $state" }
    require((finishedAtMillis != null) == state.finished) { "A finish that does not match the game" }
    return game
}

/**
 * The days' rules, across the games (PLANNING.md §4.6, §4.7): a day has one counted game in a
 * language, its replays numbered once each and only after it ended, and a language has one free
 * game under way at a time.
 */
private fun checkDays(games: List<BackupGame>) {
    val dailies = games.filter { it.mode is GameMode.Daily }
    val keys = dailies.map { Triple(it.language, (it.mode as GameMode.Daily).date, it.practice) }
    require(keys.toSet().size == keys.size) { "A day's game twice" }
    val counted = dailies.filter { it.practice == 0 }.associateBy { it.language to (it.mode as GameMode.Daily).date }
    dailies.filter { it.practice > 0 }.forEach { replay ->
        val day = counted[replay.language to (replay.mode as GameMode.Daily).date]
        require(day != null && day.state.finished) { "A replay of a day with no result" }
    }
    Language.entries.forEach { language ->
        val underWay = games.count { it.language == language && it.mode is GameMode.Free && !it.state.finished }
        require(underWay <= 1) { "Two free games under way in $language" }
    }
}

private fun WordDto.toEntry(): NotebookEntry {
    val language = languageOf(language)
    val entry = NotebookEntry(
        language = language,
        word = wordOf(language, word),
        firstMet = LocalDate.parse(firstMet),
        lastMet = LocalDate.parse(lastMet),
        timesMet = timesMet,
        solved = solved,
        missed = missed,
        withHints = withHints,
        savedByHand = savedByHand,
    )
    require(!entry.lastMet.isBefore(entry.firstMet)) { "A word last met before it was first" }
    require(listOf(timesMet, solved, missed, withHints).all { it >= 0 }) { "A count below zero" }
    require(timesMet >= 1) { "A word never met" }
    require(solved + missed > 0 || savedByHand) { "A word no game hid and no hand kept" }
    require(withHints <= solved + missed) { "More games with hints than games" }
    return entry
}

@Serializable
internal data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAtMillis: Long,
    val zone: String = "",
    val app: String = "",
    val settings: SettingsDto,
    val games: List<GameDto> = emptyList(),
    val notebook: List<WordDto> = emptyList(),
    val milestones: List<MilestoneDto> = emptyList(),
)

@Serializable
internal data class SettingsDto(
    val gameLanguage: String,
    val levels: Map<String, String> = emptyMap(),
    val hard: Boolean = false,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val reducedMotion: Boolean? = null,
    val theme: String = "system",
    val palette: String = "vivid",
    val font: String = "google_sans",
    val wallpaperColors: Boolean = false,
    val reminder: Boolean = false,
    val reminderTime: String = "20:00",
)

@Serializable
internal data class GameDto(
    val language: String,
    val interfaceLanguage: String,
    val mode: String,
    val date: String? = null,
    val level: String? = null,
    val solution: String,
    val attempts: Int,
    val hard: Boolean = false,
    val status: String,
    val startedAtMillis: Long,
    val startedOn: String,
    val finishedAtMillis: Long? = null,
    val practice: Int = 0,
    val moves: List<MoveDto> = emptyList(),
)

/** A move: a guess's key form, or a hint's kind; never both. */
@Serializable
internal data class MoveDto(val guess: String? = null, val hint: String? = null)

@Serializable
internal data class WordDto(
    val language: String,
    val word: String,
    val firstMet: String,
    val lastMet: String,
    val timesMet: Int,
    val solved: Int = 0,
    val missed: Int = 0,
    val withHints: Int = 0,
    val savedByHand: Boolean = false,
)

@Serializable
internal data class MilestoneDto(val id: String, val unlockedAtMillis: Long)
