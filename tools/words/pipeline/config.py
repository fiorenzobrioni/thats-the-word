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
LENGTH = 5
ATTEMPTS = 6

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
# ten years long from the epoch. The epoch is provisional until the 1.0 release (§15 Open 10).
DAILY_LEVELS = (0, 1)
DAILY_SEED = "thats-the-word daily schedule, 12 Nov 2026"
PROVISIONAL_EPOCH = datetime.date(2026, 11, 12)
SCHEDULE_YEARS = 10
# How long a meaning may be, in characters: the end card shows a line or two.
MEANING_MAX = 160

# Whether a translation that one translation table alone gives, in the word's first sense, is shown
# (§6.4). Two agreeing sources are always enough.
SINGLE_TABLE_TRANSLATIONS = True
