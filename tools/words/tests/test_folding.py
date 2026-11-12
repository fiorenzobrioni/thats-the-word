import unittest

from pipeline import folding


class FoldingTest(unittest.TestCase):
    def test_italian_drops_every_mark(self):
        self.assertEqual(folding.fold("città", "it"), "citta")
        self.assertEqual(folding.fold("però", "it"), "pero")
        self.assertEqual(folding.fold("Perché", "it"), "perche")

    def test_spanish_keeps_the_enye_and_drops_the_rest(self):
        self.assertEqual(folding.fold("niño", "es"), "niño")
        self.assertEqual(folding.fold("AÑOS", "es"), "años")
        self.assertEqual(folding.fold("pingüino", "es"), "pinguino")
        self.assertEqual(folding.fold("árbol", "es"), "arbol")

    def test_the_enye_written_decomposed_is_still_the_enye(self):
        self.assertEqual(folding.fold("niño", "es"), "niño")

    def test_spanish_enye_and_n_are_two_letters(self):
        self.assertNotEqual(folding.fold("años", "es"), folding.fold("anos", "es"))
        self.assertNotEqual(folding.pack("años", "es"), folding.pack("anos", "es"))

    def test_the_enye_is_not_a_key_outside_spanish(self):
        self.assertEqual(folding.fold("niño", "it"), "nino")
        self.assertEqual(folding.fold("niño", "en"), "nino")

    def test_anything_outside_the_keys_is_not_a_word(self):
        for word in ("l'ape", "rock-n", "due parole", "abc1d", "ça va"):
            self.assertIsNone(folding.fold(word, "it"), word)
        self.assertIsNone(folding.fold("", "en"))

    def test_packing_is_five_bits_a_letter_first_letter_highest(self):
        self.assertEqual(folding.pack("aaaab", "en"), 1)
        self.assertEqual(folding.pack("baaaa", "en"), 1 << 20)
        self.assertEqual(folding.pack("ñ", "es"), 26)
        for word, lang in (("gatto", "it"), ("niños", "es"), ("zebra", "en")):
            self.assertEqual(folding.unpack(folding.pack(word, lang), len(word), lang), word)

    def test_sorting_packed_words_sorts_the_words(self):
        words = ["zebra", "apple", "mango", "abbey", "zzzzz", "aaaaa"]
        self.assertEqual(sorted(words), sorted(words, key=lambda w: folding.pack(w, "en")))

    def test_the_italian_alphabet_has_twenty_one_letters(self):
        self.assertEqual(len(folding.ITALIAN_ALPHABET), 21)
        self.assertTrue(folding.within_italian_alphabet("gatto"))
        self.assertFalse(folding.within_italian_alphabet("taxi"))
        self.assertFalse(folding.within_italian_alphabet("jeans"))


if __name__ == "__main__":
    unittest.main()
