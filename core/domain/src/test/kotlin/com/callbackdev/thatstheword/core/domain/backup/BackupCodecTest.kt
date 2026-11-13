package com.callbackdev.thatstheword.core.domain.backup

import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.Move
import com.callbackdev.thatstheword.core.domain.word
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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * The backup file (PLANNING.md §5, §11 Phase 6): the round trip, and every file that must not be
 * restored (not a backup, a newer one, a damaged one, one that says what no game could have
 * written) told apart before anything is written.
 */
class BackupCodecTest {
    private val day = LocalDate.of(2026, 11, 13)
    private val at = Instant.parse("2026-11-13T09:30:00Z")

    private fun daily(
        language: Language,
        solution: String,
        guesses: List<String>,
        date: LocalDate = day,
        startedOn: LocalDate = date,
        practice: Int = 0,
        hints: List<HintKind> = emptyList(),
        interfaceLanguage: Language = Language.ENGLISH,
    ): BackupGame {
        val moves = hints.map { Move.Hinted(it) } + guesses.map { Move.Guessed(word(it, language)) }
        val game = BackupGame(
            language = language,
            interfaceLanguage = interfaceLanguage,
            mode = GameMode.Daily(date, DailyCalendar.numberOf(date)),
            solution = word(solution, language),
            attempts = 6,
            hard = false,
            moves = moves,
            startedAt = at,
            startedOn = startedOn,
            finishedAt = null,
            practice = practice,
        )
        return if (game.state.finished) game.copy(finishedAt = at.plusSeconds(90)) else game
    }

    private val settings = UserSettings(
        gameLanguage = Language.SPANISH,
        levels = mapOf(Language.ITALIAN to Level.ADVANCED, Language.SPANISH to Level.INTERMEDIATE),
        hard = true,
        sound = false,
        haptics = true,
        reducedMotion = true,
        themeMode = ThemeMode.DARK,
        palette = AppPalette.PAPER,
        font = AppFont.INTER,
        wallpaperColors = true,
        firstRunDone = true,
        tipsSeen = false,
        reminder = true,
        reminderTime = LocalTime.of(21, 15),
        askedNotifications = false,
    )

    private val backup = Backup(
        exportedAt = at,
        zone = "Europe/Rome",
        appVersion = "1.0.0",
        settings = settings,
        games = listOf(
            daily(Language.ITALIAN, "citta", listOf("carta", "citta")),
            daily(Language.ITALIAN, "citta", listOf("citta"), practice = 1),
            daily(Language.SPANISH, "nubes", listOf("niños", "nubes"), hints = listOf(HintKind.LETTER)),
            daily(Language.ENGLISH, "apple", listOf("arise", "angle"), date = day.minusDays(3)),
            BackupGame(
                language = Language.ENGLISH,
                interfaceLanguage = Language.ENGLISH,
                mode = GameMode.Free(Level.BASE),
                solution = word("crane"),
                attempts = 6,
                hard = true,
                moves = listOf(Move.Guessed(word("cream"))),
                startedAt = at,
                startedOn = day,
                finishedAt = null,
                practice = 0,
            ),
        ),
        notebook = listOf(
            NotebookEntry(Language.ITALIAN, word("citta", Language.ITALIAN), day, day, 1, 1, 0, 0, false),
            NotebookEntry(Language.SPANISH, word("año", Language.SPANISH), day, day, 1, 0, 0, 0, true),
        ),
        milestones = mapOf(Milestone.FIRST_FOREIGN_WIN to at),
    )

    private fun read(text: String) = BackupCodec.decode(text)

    private fun ok(text: String): Backup {
        val read = read(text)
        assertThat(read).isInstanceOf(BackupRead.Ok::class.java)
        return (read as BackupRead.Ok).backup
    }

    /** The file with one piece of text replaced, as a hand or a tool might have changed it. */
    private fun edited(from: String, to: String): String {
        val text = BackupCodec.encode(backup)
        assertThat(text).contains(from)
        return text.replaceFirst(from, to)
    }

    @Test
    fun `a backup comes back as it went`() {
        val back = ok(BackupCodec.encode(backup))
        assertThat(back).isEqualTo(backup)
        assertThat(back.games.map { it.state })
            .containsExactly(GameState.WON, GameState.WON, GameState.WON, GameState.PLAYING, GameState.PLAYING)
            .inOrder()
        assertThat(back.countedGames).isEqualTo(2)
    }

    @Test
    fun `the file reads without the app`() {
        val text = BackupCodec.encode(backup)
        assertThat(text).contains("\"format\": \"thatstheword-backup\"")
        assertThat(text).contains("\"date\": \"2026-11-13\"")
        assertThat(text).contains("\"guess\": \"niños\"")
        assertThat(text).contains("\"hint\": \"letter\"")
        assertThat(text).contains("\"status\": \"won\"")
        assertThat(text).contains("\"theme\": \"dark\"")
        assertThat(BackupCodec.fileName(day)).isEqualTo("thatstheword-backup-2026-11-13.json")
    }

    @Test
    fun `a byte-order mark in front changes nothing`() {
        assertThat(ok("" + BackupCodec.encode(backup))).isEqualTo(backup)
    }

    @Test
    fun `not a backup`() {
        assertThat(read("")).isEqualTo(BackupRead.NotABackup)
        assertThat(read("Dear diary")).isEqualTo(BackupRead.NotABackup)
        assertThat(read("[1, 2, 3]")).isEqualTo(BackupRead.NotABackup)
        assertThat(read("""{"format": "tetto-backup", "version": 1}""")).isEqualTo(BackupRead.NotABackup)
        assertThat(read("""{"caps": []}""")).isEqualTo(BackupRead.NotABackup)
    }

    @Test
    fun `a newer backup asks for an update`() {
        assertThat(read(edited("\"version\": 1", "\"version\": 2"))).isEqualTo(BackupRead.TooNew(2))
    }

    @Test
    fun `a file cut short is damaged`() {
        val text = BackupCodec.encode(backup)
        assertThat(read(text.substring(0, text.length / 2))).isEqualTo(BackupRead.Damaged)
        assertThat(read(edited("\"version\": 1", "\"version\": \"one\""))).isEqualTo(BackupRead.Damaged)
        assertThat(read(edited("\"version\": 1", "\"version\": 0"))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `a field missing is damaged, a field unknown is skipped`() {
        assertThat(read(edited("\"solution\": \"citta\",", ""))).isEqualTo(BackupRead.Damaged)
        assertThat(ok(edited("\"zone\":", "\"weather\": \"fair\",\n  \"zone\":"))).isEqualTo(backup)
    }

    @Test
    fun `a word no keyboard types is damaged`() {
        assertThat(read(edited("\"guess\": \"carta\"", "\"guess\": \"car7a\""))).isEqualTo(BackupRead.Damaged)
        assertThat(read(edited("\"guess\": \"carta\"", "\"guess\": \"cart\""))).isEqualTo(BackupRead.Damaged)
        // Ñ is Spanish's alone.
        assertThat(read(edited("\"solution\": \"citta\"", "\"solution\": \"cañta\""))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `a game that replays into another result is damaged`() {
        // Said won, its moves lose nothing yet: it is under way.
        assertThat(read(edited("\"status\": \"playing\"", "\"status\": \"won\""))).isEqualTo(BackupRead.Damaged)
        // A move after the word was found.
        assertThat(
            read(
                edited(
                    "{\n          \"guess\": \"citta\"\n        }",
                    "{\n          \"guess\": \"citta\"\n        }, {\"guess\": \"carta\"}",
                ),
            ),
        ).isEqualTo(BackupRead.Damaged)
        // More moves than attempts.
        assertThat(read(edited("\"attempts\": 6", "\"attempts\": 1"))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `a free word left for another comes back left`() {
        val left = BackupGame(
            language = Language.ENGLISH,
            interfaceLanguage = Language.ENGLISH,
            mode = GameMode.Free(Level.ADVANCED),
            solution = word("crane"),
            attempts = 6,
            hard = false,
            moves = listOf(Move.Guessed(word("cream"))),
            startedAt = at,
            startedOn = day,
            finishedAt = at.plusSeconds(60),
            practice = 0,
            conceded = true,
        )
        val with = backup.copy(games = backup.games + left)
        val text = BackupCodec.encode(with)
        val back = ok(text)
        assertThat(back).isEqualTo(with)
        assertThat(back.games.last().state).isEqualTo(GameState.LOST)
        assertThat(back.countedGames).isEqualTo(3)
        // A daily is never left: one that says lost and replays under way is damaged.
        assertThat(read(edited("\"status\": \"playing\"", "\"status\": \"lost\""))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `a move that is neither or both is damaged`() {
        assertThat(read(edited("\"hint\": \"letter\"", "\"hint\": \"phone a friend\""))).isEqualTo(BackupRead.Damaged)
        assertThat(
            read(edited("\"hint\": \"letter\"", "\"hint\": \"letter\", \"guess\": \"nubes\"")),
        ).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `a hint in the interface's own language is damaged`() {
        val own = backup.copy(
            games = listOf(
                daily(
                    Language.ENGLISH,
                    "apple",
                    listOf("apple"),
                    hints = listOf(HintKind.TRANSLATION),
                    interfaceLanguage = Language.ENGLISH,
                ),
            ),
        )
        assertThat(read(BackupCodec.encode(own))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `the days' rules are kept`() {
        // A day's counted game twice.
        val twice = backup.copy(games = backup.games + backup.games.first())
        assertThat(read(BackupCodec.encode(twice))).isEqualTo(BackupRead.Damaged)
        // A replay of a day that has no result.
        val orphan = backup.copy(games = listOf(daily(Language.ITALIAN, "citta", listOf("citta"), practice = 1)))
        assertThat(read(BackupCodec.encode(orphan))).isEqualTo(BackupRead.Damaged)
        // A daily started before its own day, or before the first.
        val early = backup.copy(
            games = listOf(daily(Language.ITALIAN, "citta", emptyList(), startedOn = day.minusDays(1))),
        )
        assertThat(read(BackupCodec.encode(early))).isEqualTo(BackupRead.Damaged)
        val beforeTheFirst = backup.copy(
            games = listOf(daily(Language.ITALIAN, "citta", emptyList(), date = DailyCalendar.EPOCH.minusDays(1))),
        )
        assertThat(read(BackupCodec.encode(beforeTheFirst))).isEqualTo(BackupRead.Damaged)
        // Two free games under way in one language.
        val free = backup.games.last()
        assertThat(read(BackupCodec.encode(backup.copy(games = backup.games + free)))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `a finish that does not match the game is damaged`() {
        val unfinished = backup.copy(games = listOf(backup.games.first().copy(finishedAt = null)))
        assertThat(read(BackupCodec.encode(unfinished))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `the notebook's rows are checked`() {
        assertThat(read(edited("\"timesMet\": 1", "\"timesMet\": -1"))).isEqualTo(BackupRead.Damaged)
        assertThat(
            read(edited("\"lastMet\": \"2026-11-13\"", "\"lastMet\": \"2026-11-12\"")),
        ).isEqualTo(BackupRead.Damaged)
        val twice = backup.copy(notebook = backup.notebook + backup.notebook.first())
        assertThat(read(BackupCodec.encode(twice))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `a look or a milestone this version does not know is left at its default`() {
        val back = ok(edited("\"theme\": \"dark\"", "\"theme\": \"sepia\""))
        assertThat(back.settings.themeMode).isEqualTo(ThemeMode.SYSTEM)
        val milestone = ok(edited("\"id\": \"first_foreign_win\"", "\"id\": \"a_thousand_words\""))
        assertThat(milestone.milestones).isEmpty()
        val level = ok(edited("\"it\": \"advanced\"", "\"it\": \"legendary\""))
        assertThat(level.settings.levels).doesNotContainKey(Language.ITALIAN)
        val hour = ok(edited("\"reminderTime\": \"21:15\"", "\"reminderTime\": \"teatime\""))
        assertThat(hour.settings.reminderTime).isEqualTo(LocalTime.of(20, 0))
    }

    @Test
    fun `the reminder and its hour travel, whether this phone asked for notifications does not`() {
        val back = ok(BackupCodec.encode(backup.copy(settings = settings.copy(askedNotifications = true))))
        assertThat(back.settings.reminder).isTrue()
        assertThat(back.settings.reminderTime).isEqualTo(LocalTime.of(21, 15))
        assertThat(back.settings.askedNotifications).isFalse()
    }

    @Test
    fun `a backup written before the reminder reads with it off`() {
        val text = BackupCodec.encode(backup)
            .replace(",\n    \"reminder\": true", "")
            .replace(",\n    \"reminderTime\": \"21:15\"", "")
        assertThat(text).doesNotContain("reminder")
        val back = ok(text)
        assertThat(back.settings.reminder).isFalse()
        assertThat(back.settings.reminderTime).isEqualTo(LocalTime.of(20, 0))
    }

    @Test
    fun `an unknown language is damaged, where it decides the words`() {
        assertThat(read(edited("\"gameLanguage\": \"es\"", "\"gameLanguage\": \"fr\""))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `the first run reads as done, the tips as the phone's`() {
        val back = ok(BackupCodec.encode(backup.copy(settings = settings.copy(firstRunDone = false, tipsSeen = true))))
        assertThat(back.settings.firstRunDone).isTrue()
        assertThat(back.settings.tipsSeen).isFalse()
    }

    @Test
    fun `an empty phone's backup comes back empty`() {
        val empty = backup.copy(games = emptyList(), notebook = emptyList(), milestones = emptyMap())
        assertThat(ok(BackupCodec.encode(empty))).isEqualTo(empty)
    }
}
