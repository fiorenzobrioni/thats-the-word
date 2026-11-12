"""The filters and the glossary's rules, on entries written by hand (no source needed)."""

import unittest

from pipeline import config, glossary, lexicon


def entry(word, pos, senses, translations=(), lang="it", extract="en.it"):
    return lexicon.Entry(word, lang, pos, list(senses), list(translations), [], False, extract)


class FakeWiktionary:
    def __init__(self, entries):
        self.index = {}
        for e in entries:
            self.index.setdefault((e.extract, e.lang), {}).setdefault(e.word, []).append(e)

    def entries(self, extract, lang, word):
        return self.index.get((extract, lang), {}).get(word, [])

    def words(self, extract, lang):
        return self.index.get((extract, lang), {})

    def native_extracts(self, lang):
        return tuple(dict.fromkeys((f"en.{lang}", lang if lang != "en" else "en.en")))


class LexiconRulesTest(unittest.TestCase):
    def test_a_word_that_only_opens_with_forms_is_a_form(self):
        cuori = [entry("cuori", "noun", [{"g": "plural of cuore", "f": ["cuore"]}, {"g": "hearts (suit)"}])]
        self.assertTrue(lexicon.mostly_a_form(cuori))
        notte = [
            entry("notte", "noun", [{"g": "night"}]),
            entry("notte", "verb", [{"g": "a form", "f": ["nottare"]}]),
        ]
        self.assertFalse(lexicon.mostly_a_form(notte))

    def test_offensive_when_the_first_sense_or_half_of_them_is(self):
        apple = [entry("apple", "noun", [{"g": "a fruit"}, {"g": "a tree"}, {"g": "a slur", "t": ["ethnic", "slur"]}])]
        self.assertFalse(lexicon.is_offensive(apple))
        first = [entry("xxxxx", "noun", [{"g": "vulgar", "t": ["vulgar"]}, {"g": "plain"}])]
        self.assertTrue(lexicon.is_offensive(first))
        half = [entry("yyyyy", "noun", [{"g": "plain"}, {"g": "vulgar", "t": ["vulgar"]}])]
        self.assertTrue(lexicon.is_offensive(half))

    def test_dated_only_when_every_sense_is(self):
        self.assertTrue(lexicon.is_dated([entry("aguto", "adj", [{"g": "sharp", "t": ["archaic"]}])]))
        self.assertFalse(lexicon.is_dated([entry("acuto", "adj", [{"g": "sharp"}, {"g": "x", "t": ["archaic"]}])]))

    def test_roman_numerals_are_not_words_but_civil_is(self):
        self.assertTrue(lexicon._ROMAN.match("xviii"))
        self.assertTrue(lexicon._ROMAN.match("mcccx"))
        for word in ("civil", "vivid", "livid", "mild"):
            self.assertIsNone(lexicon._ROMAN.match(word), word)

    def test_levels_follow_the_thresholds(self):
        base, intermediate, advanced = config.LEVEL_THRESHOLDS["it"]
        self.assertEqual(config.level_of("it", base), 0)
        self.assertEqual(config.level_of("it", base - 0.01), 1)
        self.assertEqual(config.level_of("it", intermediate - 0.01), 2)
        self.assertEqual(config.level_of("it", advanced - 0.01), 3)


class GlossaryRulesTest(unittest.TestCase):
    def test_clean_drops_the_editions_notes_and_empty_glosses(self):
        self.assertIsNone(glossary.clean("casa ( approfondimento) f sing", "casa"))
        self.assertIsNone(glossary.clean("definizione mancante; se vuoi, aggiungila tu", "dolce"))
        self.assertEqual(glossary.clean("corso d'acqua ( tassonomia)", "fiume"), "corso d'acqua")
        long = glossary.clean("parola " * 60, "x")
        self.assertLessEqual(len(long), config.MEANING_MAX + 1)
        self.assertTrue(long.endswith("…"))

    def test_candidates_lose_their_articles(self):
        self.assertEqual(glossary._candidate("to love", "en"), "love")
        self.assertEqual(glossary._candidate("il cane", "it"), "cane")
        self.assertEqual(glossary._candidate("el perro (animal)", "es"), "perro")
        self.assertIsNone(glossary._candidate("a long explanation of many words", "en"))

    def test_two_sources_make_a_translation_reliable_one_table_a_single_one(self):
        wikt = FakeWiktionary(
            [
                # The Spanish edition glosses the Italian word: one source.
                entry("gatto", "noun", [{"g": "Gato."}], lang="it", extract="es"),
                # The Italian edition's table: a second source.
                entry("gatto", "noun", [{"g": "piccolo felino domestico"}], [["es", "gato", "felino"]], "it", "it"),
                # A word only one table translates, in its first sense.
                entry("guado", "noun", [{"g": "passaggio di un fiume"}], [["es", "vado", "fiume"]], "it", "it"),
                # A word only one gloss names: not shown.
                entry("tondo", "adj", [{"g": "redondo"}], lang="it", extract="es"),
                entry("gato", "noun", [{"g": "Mamífero felino doméstico."}], lang="es", extract="es"),
            ]
        )
        gl = glossary.Glossary(wikt)
        gatto = gl.gloss("it", "es", "gatto")
        self.assertEqual(gatto.translations, ["gato"])
        self.assertTrue(gatto.agreed)
        self.assertEqual(gatto.meaning_via, "gato")
        self.assertEqual(gatto.meaning, "Mamífero felino doméstico.")
        guado = gl.gloss("it", "es", "guado")
        self.assertEqual(guado.translations, ["vado"])
        self.assertFalse(guado.agreed)
        self.assertEqual(gl.gloss("it", "es", "tondo").translations, [])

    def test_the_same_language_shows_the_definition_alone(self):
        wikt = FakeWiktionary([entry("fiume", "noun", [{"g": "corso d'acqua"}], lang="it", extract="it")])
        g = glossary.Glossary(wikt).gloss("it", "it", "fiume")
        self.assertEqual((g.translations, g.meaning), ([], "corso d'acqua"))


if __name__ == "__main__":
    unittest.main()
