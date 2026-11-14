"""The pipeline's choices, in one place, each with its reason (PLANNING.md §6, docs/words/README.md).

Changing a value here changes what the app ships: rebuild, read the report, and record the change
in PLANNING.md §15.
"""

import datetime
import pathlib

TOOLS = pathlib.Path(__file__).resolve().parent.parent
ROOT = TOOLS.parent.parent
LISTS = TOOLS / "lists"
ASSETS = ROOT / "core" / "words" / "src" / "main" / "assets" / "words"
REPORT = ROOT / "docs" / "words"

LANGUAGES = ("it", "en", "es")
# The length 1.0 plays, and the only one whose files ship (PLANNING.md §6.6).
LENGTH = 5
ATTEMPTS = 6
# The lengths the pipeline builds and measures: 1.1's Bridge (*gatto / cat / gato*) and 1.2's
# variable length (VISION §8: 4 to 7 letters) need them. Each is built and counted in the report
# with the same rules, so a later version adds a length by shipping it, not by writing the rules
# again (§15, 14 Nov 2026). Only `SHIPPED_LENGTHS` become files in the app: the others would weigh
# on 1.0 for nothing, and a daily schedule is frozen only when its mode ships.
LENGTHS = (3, 4, 5, 6, 7, 8)
SHIPPED_LENGTHS = (LENGTH,)

# The Spanish dictionaries read: Spain's, Mexico's and the Río de la Plata's (its voseo).
HUNSPELL = {"it": ("it_IT",), "es": ("es_ES", "es_MX", "es_AR")}

# A solution must be in wordfreq's list at all: below this Zipf value a word is too rare to be
# fair even in Complete.
MIN_ZIPF = 1.5

# The four levels' lower bounds, as Zipf frequencies (§6.3): Base, Intermediate, Advanced; Complete
# takes what is left. The same in the three languages, so that a level means the same frequency
# everywhere: Base from 4.5 (a word met about thirty times in a million, among a learner's first
# words), Intermediate from 3.5, Advanced from 2.7. The counts are in the report.
LEVEL_NAMES = ("Base", "Intermediate", "Advanced", "Complete")
LEVEL_THRESHOLDS = {
    "it": (4.5, 3.5, 2.7),
    "en": (4.5, 3.5, 2.7),
    "es": (4.5, 3.5, 2.7),
}


def level_of(language: str, zipf: float) -> int:
    for level, bound in enumerate(LEVEL_THRESHOLDS[language]):
        if zipf >= bound:
            return level
    return len(LEVEL_THRESHOLDS[language])


# The daily schedule (§6.5): drawn from Base and Intermediate, shuffled with this seed, at least
# ten years long from the epoch: daily number 1, fixed by the owner on 12 Nov 2026 (§15 Open 10)
# and never moved again. The schedule's entries are by number, so it chose no word.
DAILY_LEVELS = (0, 1)
DAILY_SEED = "thats-the-word daily schedule, 12 Nov 2026"
EPOCH = datetime.date(2026, 11, 1)
SCHEDULE_YEARS = 10
# A meaning (§6.4), as the end card, the hint and the notebook show it: at most two lines on a
# phone (about 100 characters at the default text size), never cut with an ellipsis: a definition
# too long is shortened only where a clause or a sentence ends, else another sense is read, else
# the word has none (§15, 14 Nov 2026). A single word is a synonym or a translation, not a
# definition (*lungi*, «lontano»; *pooch*, «a dog»); two words of meaning can define (*tizio*,
# «individuo qualsiasi»), articles and prepositions not counted.
MEANING_MAX = 100
MEANING_MIN_WORDS = 2
# A meaning is read in a word's first sense and the finer senses under it (at most this many),
# or in the sense a translation table names: a later, rarer sense read in the first one's place
# showed *oveja* as mutton and *media* as a shipping loss (§15, 14 Nov 2026).
MEANING_SENSES = 5
# With no reliable translation, the meaning may still be read through the first candidates one
# source gives for the word's first sense (the translation itself is not shown).
MEANING_THROUGH_SINGLE = 2

# Whether a translation that one translation table alone gives, in the word's first sense, is shown
# (§6.4). Two agreeing sources are always enough.
SINGLE_TABLE_TRANSLATIONS = True
