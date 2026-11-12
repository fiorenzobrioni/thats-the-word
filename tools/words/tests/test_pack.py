import gzip
import unittest

from pipeline import build, folding, frequency, lexicon, pack


class PackTest(unittest.TestCase):
    def test_the_header(self):
        data = pack.accepted("es", 5, ["niños", "gatos"])
        h = pack.read_header(data)
        self.assertEqual(data[:4], b"TTWA")
        self.assertEqual(h, {"kind": "accepted", "version": 1, "language": 2, "length": 5, "extra": 0, "count": 2})
        self.assertEqual(len(data), 16 + 2 * 4)

    def test_accepted_words_are_sorted(self):
        words = ["zebra", "apple", "mango"]
        values = pack.read_words(pack.accepted("en", 5, words))
        self.assertEqual(values, sorted(values))
        self.assertEqual([folding.unpack(v, 5, "en") for v in values], sorted(words))

    def test_solutions_keep_their_level_in_the_low_bits(self):
        sols = [lexicon.Solution("gatto", "gatto", 4.5, "noun", 0), lexicon.Solution("trama", "trama", 3.0, "noun", 2)]
        values = pack.read_words(pack.solutions("it", 5, sols))
        decoded = {folding.unpack(v >> 2, 5, "it"): v & 3 for v in values}
        self.assertEqual(decoded, {"gatto": 0, "trama": 2})

    def test_the_daily_reads_back_and_is_not_plain(self):
        words = ["gatto", "fiume", "gatto", "notte"]
        data = pack.daily("it", 5, words)
        self.assertEqual(pack.read_daily(data, "it"), words)
        plain = pack.words("daily", "it", 5, [folding.pack(w, "it") for w in words])
        self.assertNotEqual(data[16:], plain[16:])
        # The same word on two days is written differently.
        self.assertNotEqual(data[16:20], data[24:28])

    def test_a_table_is_sorted_by_its_keys_bytes_and_reads_back(self):
        records = {"perro": [["cane"], "animale", ""], "año": [["anno"], "", ""], "casa": [["casa", "dimora"], "x", "y"]}
        data = pack.table("glossary", "es", 0, records, folding.LANGUAGE_CODE["it"])
        h = pack.read_header(data)
        self.assertEqual((h["kind"], h["extra"], h["count"]), ("glossary", 0, 3))
        back = pack.read_table(data)
        self.assertEqual(list(back), sorted(records, key=lambda k: k.encode()))
        self.assertEqual(back["casa"], [["casa", "dimora"], "x", "y"])
        self.assertEqual(back["perro"][0], "cane")

    def test_a_separator_inside_a_field_is_refused(self):
        with self.assertRaises(ValueError):
            pack.table("forms", "it", 5, {"bad": ["a\u001fb"]})


class LongerWordsTest(unittest.TestCase):
    def test_four_bytes_up_to_six_letters_eight_from_seven_to_twelve(self):
        self.assertEqual([pack.word_width(n) for n in (4, 5, 6)], [4, 4, 4])
        self.assertEqual([pack.word_width(n) for n in (7, 9, 12)], [8, 8, 8])
        for n in (0, 13):
            with self.assertRaises(ValueError):
                pack.word_width(n)

    def test_a_solution_of_six_and_of_twelve_letters_keeps_its_level(self):
        for word in ("zzzzzz", "zzzzzzzzzzzz"):
            sols = [lexicon.Solution(word, word, 1.0, "noun", 3)]
            data = pack.solutions("en", len(word), sols)
            self.assertEqual(len(data), 16 + pack.word_width(len(word)))
            (value,) = pack.read_words(data)
            self.assertEqual((folding.unpack(value >> 2, len(word), "en"), value & 3), (word, 3))

    def test_seven_letter_files_sort_and_read_back(self):
        words = ["giraffa", "abbaino", "zampone", "cammino"]
        values = pack.read_words(pack.accepted("it", 7, words))
        self.assertEqual([folding.unpack(v, 7, "it") for v in values], sorted(words))
        daily = pack.daily("es", 7, ["caminar", "señores"])
        self.assertEqual(len(daily), 16 + 2 * 8)
        self.assertEqual(pack.read_daily(daily, "es"), ["caminar", "señores"])

    def test_the_five_letter_files_are_unchanged_by_longer_lengths(self):
        # The four-byte mask is the first four bytes of the eight-byte one: the files already
        # written keep their bytes.
        self.assertEqual(pack.daily_mask("it", 9, 8) >> 32, pack.daily_mask("it", 9, 4))
        self.assertEqual(len(pack.daily("it", 5, ["gatto"])), 16 + 4)


class FrequencyTest(unittest.TestCase):
    def test_a_cbpack_file_decodes_to_zipf_values(self):
        # [{"format": "cB", "version": 1}, ["the"], [], ["cat", "dog"]] as MessagePack.
        header = bytes([0x82, 0xA6]) + b"format" + bytes([0xA2]) + b"cB" + bytes([0xA7]) + b"version" + bytes([0x01])
        body = bytes([0x94]) + header + bytes([0x91, 0xA3]) + b"the" + bytes([0x90, 0x92, 0xA3]) + b"cat" + bytes([0xA3]) + b"dog"
        value, end = frequency._decode(gzip.decompress(gzip.compress(body)))
        self.assertEqual(end, len(body))
        self.assertEqual(value[0], {"format": "cB", "version": 1})
        self.assertEqual(value[1:], [["the"], [], ["cat", "dog"]])


class FormsRecordsTest(unittest.TestCase):
    def words(self, displays, solutions, lemma_of=None):
        return lexicon.LanguageWords(
            "it", 5, set(displays), displays, lemma_of or {}, set(), solutions, {}, {}, {}, set(displays)
        )

    def test_a_solutions_own_written_form_comes_first(self):
        # *citta* and *città* are both lemmas of the key citta; the solution is the more frequent.
        lw = self.words(
            {"citta": ["citta", "città"], "gatto": ["gatto", "gattò"]},
            [lexicon.Solution("citta", "città", 5.4, "noun"), lexicon.Solution("gatto", "gatto", 4.6, "noun")],
        )
        records = build.forms_records(lw)
        self.assertEqual(records["citta"], [["città"], ["citta"]])
        self.assertEqual(records["gatto"], [["gatto"], ["gattò"]])

    def test_a_key_that_is_no_solution_keeps_its_forms_in_order(self):
        lw = self.words({"bacio": ["bacio", "baciò"]}, [], {"baciò": ["baciare"]})
        self.assertEqual(build.forms_records(lw)["bacio"], [["bacio"], ["baciò", "baciare"]])


if __name__ == "__main__":
    unittest.main()
