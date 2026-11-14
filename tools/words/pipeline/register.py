"""A sense's register, read from the labels Wiktionary's editors give it (PLANNING.md §6.2).

The game keeps no list of words to avoid: what keeps a word out is the editions' own labels, read
here as three kinds. Every pattern below names a *label* (a tag, a usage note, a category), never
a word, so the rules can be read, tested and changed without writing an offensive word anywhere.

- **Coarse** (`C`): vulgar, obscene, offensive, a slur, a swear word, blasphemous.
- **Derogatory** (`D`): derogatory or pejorative.

  A solution may have neither anywhere, in any edition (*rosso*, whose one sense in many is
  derogatory, is never a word to find); a guess is refused when an edition's first sense of it
  is coarse, or most of its first entry's senses are, or every edition's first sense is
  derogatory (`lexicon.refused`), and accepted otherwise.
- **Intimate** (`S`): the editions' topics of sex and sexuality. A solution's first sense may not
  be one (a daily is everyone's); a guess is never refused for it.

The three editions write their labels differently: the English edition's tags are regular
(`vulgar`, `derogatory`); the Italian and Spanish editions add usage notes in their own words
(«dispregiativo», «malsonante») and categories («Parole volgari-IT», «ES:Términos despectivos»).
"""

import re

COARSE = "C"
DEROGATORY = "D"
INTIMATE = "S"

_COARSE_TAGS = frozenset({"vulgar", "offensive", "slur", "obscene", "swear-word", "profanity", "blasphemous"})
_DEROGATORY_TAGS = frozenset({"derogatory", "pejorative", "disparaging"})

# Usage notes, as the Italian and Spanish editions write them: the label's stem.
_COARSE_NOTE = re.compile(
    r"vulgar|volgar|malsonante|ofensiv|offensiv|insult|blasfem|bestemmi|osceno|obscen|sexista|slur|"
    r"profanit|swear",
    re.I,
)
# A note that names the label to deny it, or to speak of a language's history.
# A note that names the label to deny it, or to speak of a language's history, or of a form that
# is only substandard: a Spanish «vulgarismo» is a word of the street, not an obscenity
# (*abajar*, to go down).
_NOT_A_LABEL = re.compile(
    r"\bnon volgar|lat[ií]n vulgar|latino volgare|antiguamente vulgar|incorrecto o vulgar|"
    r"vulgarism|volgarism|desprovist|of an offen[cs]e|of an offensive|criminal offen",
    re.I,
)
_DEROGATORY_NOTE = re.compile(r"despectiv|dispregiativ|spregiativ|derogativ|derogator|pejorativ|peggiorativ", re.I)

_COARSE_CATEGORY = re.compile(
    r"swear words|slurs\b|parole volgari|t[ée]rminos malsonantes|t[ée]rminos vulgares|vulgarities|"
    r"offensive terms|profanit|blasfemi",
    re.I,
)
_DEROGATORY_CATEGORY = re.compile(r"t[ée]rminos despectivos|dispregiativ|^derogatory |derogatory terms|pejorative terms", re.I)
# The topics as each edition names them: «Sex» (English), «ES:Sexualidad» (Spanish), «Sessualità-IT»
# (Italian).
_INTIMATE_CATEGORY = re.compile(
    r"^(?:[a-z]{2}:)?(?:sex|sexuality|sex positions|sexual acts|sexology|sexual slang|sexualidad|"
    r"sessualit[àa]|sesso)(?:-[a-z]{2,3})?$",
    re.I,
)


def of_tags(tags) -> set[str]:
    """The register a sense's tags and usage notes give it."""
    out = set()
    for tag in tags or ():
        low = tag.lower().strip()
        if low in _COARSE_TAGS:
            out.add(COARSE)
        elif low in _DEROGATORY_TAGS:
            out.add(DEROGATORY)
        elif _NOT_A_LABEL.search(low):
            continue
        elif _COARSE_NOTE.search(low):
            out.add(COARSE)
        elif _DEROGATORY_NOTE.search(low):
            out.add(DEROGATORY)
    return out


def of_categories(names) -> set[str]:
    """The register a sense's or an entry's categories give it."""
    out = set()
    for name in names or ():
        name = name.strip()
        if _NOT_A_LABEL.search(name):
            continue
        if _COARSE_CATEGORY.search(name):
            out.add(COARSE)
        elif _DEROGATORY_CATEGORY.search(name):
            out.add(DEROGATORY)
        elif _INTIMATE_CATEGORY.search(name):
            out.add(INTIMATE)
    return out


def code(labels) -> str:
    """The labels as the reduced files keep them: their letters, sorted."""
    return "".join(sorted(labels))
