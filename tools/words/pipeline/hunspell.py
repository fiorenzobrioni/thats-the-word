"""A Hunspell dictionary expanded into every form it accepts (PLANNING.md §6.1).

Hunspell keeps a stem and the affix classes it takes (`abbacchiare/ALKhlTXI`); the game needs the
forms themselves. This is a small expander of the parts of the format the Italian and Spanish
dictionaries use: prefixes and suffixes, their strip and add, their conditions, the cross product
of a prefix and a suffix, and continuation classes (a suffix whose result takes another suffix,
as Spanish's `ción/S`, two levels deep). It is run at build time and never shipped; it is this
repository's own code, under its licence.

A stem written with a capital (a proper noun, an abbreviation) is not read: the game's words are
common words.
"""

import dataclasses
import pathlib
import re


@dataclasses.dataclass(frozen=True)
class Rule:
    strip: str
    add: str
    flags: tuple[str, ...]
    condition: re.Pattern


@dataclasses.dataclass
class AffixClass:
    kind: str  # "PFX" or "SFX"
    cross: bool
    rules: list


@dataclasses.dataclass
class Affixes:
    flag_type: str
    classes: dict
    need_affix: str | None
    forbidden: str | None


def _split_flags(flags: str, flag_type: str) -> tuple[str, ...]:
    if not flags:
        return ()
    if flag_type == "long":
        return tuple(flags[i : i + 2] for i in range(0, len(flags), 2))
    if flag_type == "num":
        return tuple(f for f in flags.split(",") if f)
    return tuple(flags)


def _condition(text: str, kind: str) -> re.Pattern:
    if text in (".", ""):
        return re.compile("")
    pattern = text.replace("\\", "\\\\")
    return re.compile(pattern + "$" if kind == "SFX" else "^" + pattern)


def read_aff(path: pathlib.Path) -> Affixes:
    raw = path.read_bytes()
    encoding = "utf-8"
    for line in raw.splitlines():
        if line.startswith(b"SET "):
            encoding = line.split()[1].decode().lower().replace("iso8859", "iso-8859")
            break
    text = raw.decode(encoding)
    flag_type = "char"
    classes = {}
    need_affix = forbidden = None
    lines = text.splitlines()
    i = 0
    while i < len(lines):
        parts = lines[i].split()
        i += 1
        if not parts or parts[0].startswith("#"):
            continue
        head = parts[0]
        if head == "FLAG" and len(parts) > 1:
            flag_type = {"UTF-8": "char", "long": "long", "num": "num"}.get(parts[1], "char")
        elif head == "NEEDAFFIX":
            need_affix = parts[1]
        elif head == "FORBIDDENWORD":
            forbidden = parts[1]
        elif head in ("PFX", "SFX") and len(parts) == 4 and parts[3].isdigit():
            flag, cross, count = parts[1], parts[2] == "Y", int(parts[3])
            cls = classes.setdefault(flag, AffixClass(head, cross, []))
            for _ in range(count):
                rule = lines[i].split()
                i += 1
                if len(rule) < 4 or rule[0] != head:
                    continue
                strip = "" if rule[2] == "0" else rule[2]
                add, _, contflags = rule[3].partition("/")
                add = "" if add == "0" else add
                condition = rule[4] if len(rule) > 4 else "."
                cls.rules.append(
                    Rule(strip, add, _split_flags(contflags, flag_type), _condition(condition, head))
                )
    return Affixes(flag_type, classes, need_affix, forbidden)


def read_dic(path: pathlib.Path, affixes: Affixes):
    """The stems and their flags, skipping the count line, comments and capitalised stems."""
    with open(path, encoding="utf-8") as stream:
        first = True
        for line in stream:
            line = line.rstrip("\n").split("\t")[0].strip()
            if first:
                first = False
                if line.isdigit():
                    continue
            if not line or line.startswith("/") or line.startswith("#"):
                continue
            # A slash escaped as `\/` belongs to the word.
            parts = re.split(r"(?<!\\)/", line, maxsplit=1)
            word = parts[0].replace("\\/", "/").split(" ")[0]
            flags = parts[1].split(" ")[0] if len(parts) > 1 else ""
            if not word or word[0].isupper() or word[0].isdigit():
                continue
            yield word, _split_flags(flags, affixes.flag_type)


def _apply(word: str, cls: AffixClass):
    for rule in cls.rules:
        if cls.kind == "SFX":
            if rule.strip and not word.endswith(rule.strip):
                continue
            if not rule.condition.search(word):
                continue
            stem = word[: len(word) - len(rule.strip)] if rule.strip else word
            yield stem + rule.add, rule
        else:
            if rule.strip and not word.startswith(rule.strip):
                continue
            if not rule.condition.search(word):
                continue
            yield rule.add + word[len(rule.strip) :], rule


def expand(word: str, flags: tuple[str, ...], affixes: Affixes, depth: int = 2) -> set[str]:
    """Every form a stem with its flags produces."""
    forms = set()
    if affixes.forbidden and affixes.forbidden in flags:
        return forms
    if not (affixes.need_affix and affixes.need_affix in flags):
        forms.add(word)
    prefixes = [affixes.classes[f] for f in flags if f in affixes.classes and affixes.classes[f].kind == "PFX"]
    suffixed = []
    for f in flags:
        cls = affixes.classes.get(f)
        if cls is None or cls.kind != "SFX":
            continue
        for form, rule in _apply(word, cls):
            if not (affixes.need_affix and affixes.need_affix in rule.flags):
                forms.add(form)
            suffixed.append((form, cls.cross))
            if rule.flags and depth > 1:
                forms |= expand(form, rule.flags, affixes, depth - 1)
    for pcls in prefixes:
        for form, _ in _apply(word, pcls):
            forms.add(form)
        if pcls.cross:
            for sform, cross in suffixed:
                if cross:
                    for form, _ in _apply(sform, pcls):
                        forms.add(form)
    return forms


def forms(dic: pathlib.Path, aff: pathlib.Path, keep=lambda form: True) -> set[str]:
    affixes = read_aff(aff)
    out = set()
    for word, flags in read_dic(dic, affixes):
        for form in expand(word, flags, affixes):
            if keep(form):
                out.add(form)
    return out


def forms_by_stem(dic: pathlib.Path, aff: pathlib.Path, keep=lambda form: True) -> dict[str, set[str]]:
    """Every form, with the dictionary's stems it comes from: a form is refused as a guess when every
    stem it comes from is (PLANNING.md §6.2), so an inflection is judged with its lemma."""
    affixes = read_aff(aff)
    out: dict[str, set[str]] = {}
    for word, flags in read_dic(dic, affixes):
        for form in expand(word, flags, affixes):
            if keep(form):
                out.setdefault(form, set()).add(word)
    return out
