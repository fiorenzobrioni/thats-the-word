"""The filters and the glossary's rules, on entries written by hand (no source needed)."""

import unittest

from pipeline import config, glossary, lexicon, register


def entry(word, pos, senses, translations=(), lang="it", extract="en.it", labels=""):
    return lexicon.Entry(word, lang, pos, list(senses), list(translations), [], labels, extract)


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

    # The words below are made up: the rules read labels, so no test needs a real one.
    def test_a_guess_is_refused_when_its_first_sense_or_most_of_them_are_harsh(self):
        first = [entry("xxxxa", "noun", [{"g": "coarse", "r": "C"}, {"g": "plain"}])]
        self.assertTrue(lexicon.refused_in(first))
        # A first sense only derogatory, in one edition of two, is a usage note, not a verdict.
        noted = [entry("xxxxk", "adj", [{"g": "d", "r": "D"}, {"g": "plain"}, {"g": "plain"}], lang="es", extract="es")]
        self.assertFalse(lexicon.refused_in(noted))
        wikt = FakeWiktionary(noted + [entry("xxxxk", "adj", [{"g": "plain"}], lang="es", extract="en.es")])
        self.assertFalse(lexicon.refused(wikt, "es", "xxxxk"))
        self.assertTrue(lexicon.refused(FakeWiktionary(noted), "es", "xxxxk"))
        # One edition of two opening with a coarse sense is not most of them.
        split = [
            entry("xxxxp", "verb", [{"g": "c", "r": "C"}, {"g": "plain"}, {"g": "plain"}], lang="es", extract="es"),
            entry("xxxxp", "verb", [{"g": "plain"}], lang="es", extract="en.es"),
        ]
        self.assertFalse(lexicon.refused(FakeWiktionary(split), "es", "xxxxp"))
        self.assertTrue(lexicon.refused(FakeWiktionary(split[:1]), "es", "xxxxp"))
        mostly = [entry("xxxxb", "noun", [{"g": "plain"}, {"g": "c", "r": "C"}, {"g": "c", "r": "C"}])]
        self.assertTrue(lexicon.refused_in(mostly))
        # Derogatory senses do not make a majority, nor do another entry's senses.
        figurative = [entry("xxxxl", "noun", [{"g": "plain"}, {"g": "d", "r": "D"}, {"g": "d", "r": "D"}])]
        self.assertFalse(lexicon.refused_in(figurative))
        other = [
            entry("xxxxm", "noun", [{"g": "plain"}, {"g": "c", "r": "C"}]),
            entry("xxxxm", "intj", [{"g": "c", "r": "C"}]),
        ]
        self.assertFalse(lexicon.refused_in(other))
        half = [entry("xxxxc", "noun", [{"g": "plain"}, {"g": "coarse", "r": "C"}])]
        self.assertFalse(lexicon.refused_in(half))
        # A form first («plural of»): a slang sense after it is not the word's first.
        plural = [entry("xxxxn", "noun", [{"g": "plural of xxxxo", "f": ["xxxxo"]}, {"g": "c", "r": "C"}])]
        self.assertFalse(lexicon.refused_in(plural))
        # An entry's own category speaks for its one sense.
        lone = [entry("xxxxd", "noun", [{"g": "plain"}], labels="C")]
        self.assertTrue(lexicon.refused_in(lone))

    def test_a_solution_has_no_harsh_sense_anywhere_nor_an_intimate_first_one(self):
        later = [entry("xxxxe", "noun", [{"g": "a fruit"}, {"g": "a tree"}, {"g": "d", "r": "D"}])]
        self.assertFalse(lexicon.refused_in(later))
        self.assertTrue(lexicon.harsh_anywhere(later))
        intimate = [entry("xxxxf", "noun", [{"g": "anatomy", "r": "S"}, {"g": "plain"}])]
        self.assertTrue(lexicon.intimate_first(intimate))
        self.assertFalse(lexicon.harsh_anywhere(intimate))
        plain = [entry("xxxxg", "noun", [{"g": "plain"}, {"g": "anatomy", "r": "S"}])]
        self.assertFalse(lexicon.intimate_first(plain) or lexicon.harsh_anywhere(plain))

    def test_any_edition_can_keep_a_word_out(self):
        wikt = FakeWiktionary(
            [
                entry("xxxxh", "noun", [{"g": "plain"}], lang="es", extract="en.es"),
                entry("xxxxh", "noun", [{"g": "coarse", "r": "C"}], lang="es", extract="es"),
            ]
        )
        self.assertTrue(lexicon.refused(wikt, "es", "xxxxh"))  # the Spanish edition's entry is all coarse
        self.assertTrue(lexicon.labelled(wikt, "es", "xxxxh"))
        self.assertFalse(lexicon.refused(wikt, "es", "xxxxi"))

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
        self.assertEqual(glossary.clean("ampio corso d'acqua ( tassonomia)", "fiume"), "Ampio corso d'acqua")

    def test_a_meaning_is_never_cut_only_shortened_where_a_clause_ends(self):
        clause = "piccolo felino domestico; " + "parola " * 30
        self.assertEqual(glossary.fit(clause), "Piccolo felino domestico")
        sentence = "A small domestic feline. " + "More words here " * 10 + "."
        self.assertEqual(glossary.fit(sentence), "A small domestic feline")
        self.assertIsNone(glossary.fit("parola " * 30))
        città = "centro abitato di grandi dimensioni, dove si accentrano attività amministrative, sociali, economiche e culturali"
        self.assertEqual(glossary.fit(città), "Centro abitato di grandi dimensioni")
        horse = "di cavalli o altri quadrupedi, procedere a passo di ambio, muovendo alternativamente le zampe ora dell'uno ora dell'altro lato"
        self.assertIsNone(glossary.fit(horse))
        self.assertEqual(glossary.fit("En este instante, en este momento"), "En este instante, en este momento")
        self.assertIsNone(glossary.fit("una definizione già tagliata…"))
        for text in ("x " * 200, "a, b, c, " * 40):
            out = glossary.fit(text)
            self.assertTrue(out is None or (len(out) <= config.MEANING_MAX and "…" not in out))

    def test_a_meaning_is_a_definition_and_never_names_the_word(self):
        self.assertIsNone(glossary.fit("heart"))  # a translation, not a definition
        self.assertIsNone(glossary.fit("A dog"))  # a synonym with its article
        self.assertIsNone(glossary.fit("casa f sing"))
        self.assertEqual(glossary.fit("individuo qualsiasi"), "Individuo qualsiasi")
        self.assertIsNone(glossary.fit("Synonym of something else entirely"))
        self.assertIsNone(glossary.fit("vedasi la definizione in alto"))
        self.assertIsNone(glossary.fit("Participio pasado del verbo comer"))
        self.assertIsNone(glossary.fit("femminile di nano"))
        self.assertIsNone(glossary.fit("Bulbo de esta planta, utilizado como condimento"))
        self.assertEqual(glossary.fit("small fish, such as an anchovy"), "Small fish, such as an anchovy")
        self.assertTrue(glossary.names_only("Help, assistance, aid"))
        self.assertTrue(glossary.names_only("Coconut palm (Cocos nucifera)"))
        self.assertFalse(glossary.names_only("A muscular organ that pumps blood"))
        self.assertIsNone(glossary.fit("un piccolo gattino di casa", ("gatto",)))
        self.assertIsNone(glossary.fit("il verso del gatto domestico", ("gatto",)))
        self.assertEqual(glossary.fit("il verso del cane domestico", ("gatto",)), "Il verso del cane domestico")
        self.assertEqual(glossary.fit("(zoology) small domestic feline (Felis catus)"), "Small domestic feline (Felis catus)")

    def test_register_reads_labels_not_words(self):
        self.assertEqual(register.of_tags(["vulgar", "slang"]), {register.COARSE})
        self.assertEqual(register.of_tags(["derogatory"]), {register.DEROGATORY})
        self.assertEqual(register.of_tags(["Malsonante"]), {register.COARSE})
        self.assertEqual(register.of_tags(["usualmente despectivo"]), {register.DEROGATORY})
        self.assertEqual(register.of_tags(["non volgare"]), set())
        self.assertEqual(register.of_categories(["ES:Palabras provenientes del latín vulgar"]), set())
        self.assertEqual(register.of_categories(["Parole volgari-IT"]), {register.COARSE})
        self.assertEqual(register.of_categories(["English ethnic slurs"]), {register.COARSE})
        self.assertEqual(register.of_categories(["ES:Términos despectivos"]), {register.DEROGATORY})
        self.assertEqual(register.of_categories(["Sex"]), {register.INTIMATE})
        self.assertEqual(register.of_categories(["Sessualità-IT"]), {register.INTIMATE})
        self.assertEqual(register.of_categories(["Places in Essex, England", "Sexual orientations"]), set())

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
                entry("gatto", "noun", [{"g": "piccolo felino domestico"}], [["es", "gato", "felino", -1]], "it", "it"),
                # A word only one table translates, in its first sense.
                entry("guado", "noun", [{"g": "passaggio di un fiume"}], [["es", "vado", "fiume", -1]], "it", "it"),
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
        self.assertEqual(gatto.meaning, "Mamífero felino doméstico")
        guado = gl.gloss("it", "es", "guado")
        self.assertEqual(guado.translations, ["vado"])
        self.assertFalse(guado.agreed)
        self.assertEqual(gl.gloss("it", "es", "tondo").translations, [])

    def test_the_same_language_shows_the_definition_alone(self):
        wikt = FakeWiktionary([entry("fiume", "noun", [{"g": "ampio corso d'acqua"}], lang="it", extract="it")])
        g = glossary.Glossary(wikt).gloss("it", "it", "fiume")
        self.assertEqual((g.translations, g.meaning), ([], "Ampio corso d'acqua"))

    def test_the_meaning_through_a_translation_is_the_sense_its_table_names(self):
        wikt = FakeWiktionary(
            [
                entry("cuore", "noun", [{"g": "heart"}], lang="it", extract="en.it"),
                entry("cuore", "noun", [{"g": "organo che pompa il sangue"}], [["en", "heart", "organo", -1]], "it", "it"),
                entry(
                    "heart",
                    "noun",
                    [{"g": "Emotions, kindness, moral effort, or spirit in general"}, {"g": "A muscular organ that pumps blood"}],
                    [["it", "cuore", "", 1]],
                    "en",
                    "en.en",
                ),
            ]
        )
        g = glossary.Glossary(wikt).gloss("it", "en", "cuore")
        self.assertEqual(g.translations, ["heart"])
        self.assertEqual(g.meaning, "A muscular organ that pumps blood")
        self.assertEqual(g.meaning_via, "heart")

    def test_a_translation_no_guess_may_be_is_never_shown(self):
        wikt = FakeWiktionary(
            [
                entry("xxxxj", "noun", [{"g": "yyyyy"}, {"g": "zzzzz"}], lang="it", extract="en.it"),
                entry("xxxxj", "noun", [{"g": "una parola qualsiasi"}], [["en", "yyyyy", "", -1], ["en", "zzzzz", "", -1]], "it", "it"),
                entry("yyyyy", "noun", [{"g": "harsh", "r": "C"}], lang="en", extract="en.en"),
            ]
        )
        self.assertEqual(glossary.Glossary(wikt).gloss("it", "en", "xxxxj").translations, ["zzzzz"])


if __name__ == "__main__":
    unittest.main()
