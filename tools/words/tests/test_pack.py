import gzip
import unittest

from pipeline import folding, frequency, lexicon, pack


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


class FrequencyTest(unittest.TestCase):
    def test_a_cbpack_file_decodes_to_zipf_values(self):
        # [{"format": "cB", "version": 1}, ["the"], [], ["cat", "dog"]] as MessagePack.
        header = bytes([0x82, 0xA6]) + b"format" + bytes([0xA2]) + b"cB" + bytes([0xA7]) + b"version" + bytes([0x01])
        body = bytes([0x94]) + header + bytes([0x91, 0xA3]) + b"the" + bytes([0x90, 0x92, 0xA3]) + b"cat" + bytes([0xA3]) + b"dog"
        value, end = frequency._decode(gzip.decompress(gzip.compress(body)))
        self.assertEqual(end, len(body))
        self.assertEqual(value[0], {"format": "cB", "version": 1})
        self.assertEqual(value[1:], [["the"], [], ["cat", "dog"]])


if __name__ == "__main__":
    unittest.main()
