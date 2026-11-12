"""English words from SCOWL (PLANNING.md §6.1, §6.2).

SCOWL's lists are already expanded (every plural and every tense is a line) and split by size (10
the most common, 95 the rarest) and by spelling: `english` holds what every spelling shares,
`american`, `british`, `british_z` (the -ize British), `canadian` and `australian` what is one
spelling's own, and `variant_1` to `_3` the variants, from the accepted to the seldom seen.

- **Accepted**: the shared, the five spellings' and the first variants', up to size 70. Both
  spellings are accepted (VISION §6).
- **One spelling**: a word that both the American and the British lists have (the shared list
  or each spelling's own), at any size. A word that only one of them has is spelled two ways and
  is never a solution (VISION §6): *color*, *colour*, *gray*, *grey*, *ax*, *axe*. (SCOWL's
  variant lists cannot say it alone: they hold *among* beside *amongst*.)
"""

import functools
import re
import tarfile

from . import sources

ACCEPTED_SIZE = 70
_ACCEPTED_LISTS = ("english", "american", "british", "british_z", "canadian", "australian", "variant_1")
_NAME = re.compile(r"final/([a-z_0-9]+)-(words|proper-names)\.(\d+)$")


def _lists():
    with tarfile.open(sources.path("scowl"), "r:gz") as tar:
        for member in sorted(tar.getmembers(), key=lambda m: m.name):
            match = _NAME.search(member.name)
            if not match or not member.isfile():
                continue
            stream = tar.extractfile(member)
            if stream is None:
                continue
            words = stream.read().decode("iso-8859-1").split()
            if match.group(2) == "words":
                yield match.group(1), int(match.group(3)), words
            else:
                yield match.group(1) + "-names", int(match.group(3)), words


def read() -> tuple[set[str], set[str]]:
    """The accepted words (as written) and the words both spellings write the same way."""
    accepted, american, british = set(), set(), set()
    for name, size, words in _lists():
        if name in _ACCEPTED_LISTS and size <= ACCEPTED_SIZE:
            accepted.update(words)
        if name in ("english", "american"):
            american.update(words)
        if name in ("english", "british"):
            british.update(words)
    return accepted, american & british


@functools.cache
def _sizes() -> tuple[dict, dict]:
    """The smallest size each word and each name is listed at (smaller is more common)."""
    words, names = {}, {}
    for name, size, items in _lists():
        target = names if name.endswith("-names") else words
        for w in items:
            key = w.lower() if target is names else w
            if size < target.get(key, 999):
                target[key] = size
    return words, names


def name_outranks(word: str) -> bool:
    """Whether SCOWL lists the word as a name at a more common size than as a word.

    wordfreq folds case, so *lewis* has Lewis's frequency; SCOWL lists *Lewis* at size 10 and
    *lewis* at 70.
    """
    words, names = _sizes()
    return names.get(word, 999) < words.get(word, 999)
