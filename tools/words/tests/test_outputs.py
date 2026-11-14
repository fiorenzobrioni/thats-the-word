"""The committed files, as the app will read them (no source needed: they are in the repository)."""

import datetime
import unittest

from pipeline import config, folding, lexicon, pack, schedule


def read(language, name):
    return (config.ASSETS / language / name).read_bytes()


def accepted(language):
    return set(pack.read_words(read(language, f"accepted-{config.LENGTH}.bin")))


def solutions(language):
    return {v >> 2: v & 3 for v in pack.read_words(read(language, f"solutions-{config.LENGTH}.bin"))}


@unittest.skipUnless(config.ASSETS.exists(), "the words are not built")
class OutputsTest(unittest.TestCase):
    def test_every_header_names_its_language_and_kind(self):
        for g in config.LANGUAGES:
            for name, kind in (("accepted-5.bin", "accepted"), ("solutions-5.bin", "solutions"), ("daily.bin", "daily"), ("forms-5.bin", "forms")):
                h = pack.read_header(read(g, name))
                self.assertEqual((h["kind"], h["language"], h["version"]), (kind, folding.LANGUAGE_CODE[g], 1), name)
            for u in config.LANGUAGES:
                h = pack.read_header(read(g, f"gloss-{u}.bin"))
                self.assertEqual((h["kind"], h["language"], h["extra"]), ("glossary", folding.LANGUAGE_CODE[g], folding.LANGUAGE_CODE[u]))

    def test_the_schedule_outlasts_the_build_by_three_years(self):
        # PLANNING.md §4.6: a test fails when fewer than three years are left from the build's date.
        today = datetime.date.today()
        for g in config.LANGUAGES:
            count = pack.read_header(read(g, "daily.bin"))["count"]
            left = schedule.years_left(count, config.EPOCH, today)
            self.assertGreaterEqual(left, 3, f"{g}: {left:.1f} years of dailies left")

    def test_every_daily_is_a_solution_of_the_daily_levels_and_accepted(self):
        for g in config.LANGUAGES:
            sols = solutions(g)
            acc = accepted(g)
            for i, key in enumerate(pack.read_daily(read(g, "daily.bin"), g)):
                value = folding.pack(key, g)
                self.assertIn(value, acc, f"{g} daily {i + 1}: {key}")
                self.assertIn(sols.get(value), config.DAILY_LEVELS, f"{g} daily {i + 1}: {key}")

    def test_every_solution_is_accepted_and_italian_ones_keep_to_its_alphabet(self):
        for g in config.LANGUAGES:
            acc = accepted(g)
            for value in solutions(g):
                self.assertIn(value, acc)
                if g == "it":
                    self.assertTrue(folding.within_italian_alphabet(folding.unpack(value, 5, g)))

    def test_the_common_words_are_accepted(self):
        for g in config.LANGUAGES:
            acc = accepted(g)
            common = lexicon.read_list(f"common_{g}.txt")
            self.assertEqual(len(common), 200, g)
            missing = [w for w in common if folding.pack(folding.fold(w, g), g) not in acc]
            self.assertEqual(missing, [], g)

    def test_every_solution_has_a_whole_short_meaning_in_the_three_languages(self):
        # PLANNING.md §6.4, §15 (14 Nov 2026): a definition of two lines at most, never cut.
        for g in config.LANGUAGES:
            forms = pack.read_table(read(g, f"forms-{config.LENGTH}.bin"))
            glossaries = {u: pack.read_table(read(g, f"gloss-{u}.bin")) for u in config.LANGUAGES}
            for value in solutions(g):
                key = folding.unpack(value, config.LENGTH, g)
                record = forms.get(key)
                display = (record[0] if isinstance(record[0], str) else record[0][0]) if record else key
                for u in config.LANGUAGES:
                    entry = glossaries[u].get(display)
                    self.assertIsNotNone(entry, f"{g} {display} in {u}")
                    meaning = entry[1]
                    self.assertTrue(meaning, f"{g} {display}: no meaning in {u}")
                    self.assertLessEqual(len(meaning), config.MEANING_MAX, f"{g} {display} in {u}")

    def test_no_meaning_is_cut(self):
        for g in config.LANGUAGES:
            for u in config.LANGUAGES:
                for word, fields in pack.read_table(read(g, f"gloss-{u}.bin")).items():
                    meaning = fields[1] if len(fields) > 1 else ""
                    self.assertNotIn("…", meaning, f"{g} {word} in {u}")
                    self.assertLessEqual(len(meaning), config.MEANING_MAX, f"{g} {word} in {u}")

    def test_no_list_of_words_to_avoid_is_kept(self):
        # The editions' labels keep a word out (`register.py`); no file names one (§15, 14 Nov 2026).
        self.assertEqual(sorted(p.name for p in config.LISTS.iterdir() if not p.name.startswith("common_")), [])


if __name__ == "__main__":
    unittest.main()
