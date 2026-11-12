import pathlib
import tempfile
import unittest

from pipeline import hunspell

AFF = """SET UTF-8
NEEDAFFIX Z

SFX A Y 3
SFX A are o are
SFX A are i [^cg]are
SFX A are hi [cg]are

SFX B N 1
SFX B 0 s .

SFX C Y 1
SFX C r ción/S ar

SFX S Y 1
SFX S 0 es n

PFX R Y 1
PFX R 0 ri .
"""

DIC = """6
/ a comment line, as the Italian dictionary has
parlare/AR
pagare/A
gatto/B
Roma/B
crear/C
radice/ZB
"""


class HunspellTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        root = pathlib.Path(self.dir.name)
        (root / "t.aff").write_text(AFF, encoding="utf-8")
        (root / "t.dic").write_text(DIC, encoding="utf-8")
        self.forms = hunspell.forms(root / "t.dic", root / "t.aff")

    def tearDown(self):
        self.dir.cleanup()

    def test_suffixes_strip_add_and_their_conditions(self):
        self.assertTrue({"parlare", "parlo", "parli"} <= self.forms)
        self.assertIn("paghi", self.forms)
        self.assertNotIn("pagi", self.forms)

    def test_a_prefix_crosses_with_the_suffixes(self):
        self.assertTrue({"riparlare", "riparlo", "riparli"} <= self.forms)

    def test_a_suffix_without_cross_product_still_applies(self):
        self.assertIn("gattos", self.forms)

    def test_continuation_classes_take_a_second_suffix(self):
        self.assertTrue({"crear", "creación", "creaciónes"} <= self.forms)

    def test_capitalised_stems_and_comments_are_not_read(self):
        self.assertNotIn("Roma", self.forms)
        self.assertNotIn("Romas", self.forms)
        self.assertFalse(any(f.startswith("/") for f in self.forms))

    def test_a_stem_that_needs_an_affix_is_not_a_word_by_itself(self):
        self.assertNotIn("radice", self.forms)
        self.assertIn("radices", self.forms)


if __name__ == "__main__":
    unittest.main()
