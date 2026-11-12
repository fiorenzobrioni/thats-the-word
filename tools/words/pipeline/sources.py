"""The sources, pinned, and their fetching into the cache (PLANNING.md §6.1, §6.7).

Every source is named here with the exact version it was read at, its address, the SHA-256 of
its content and its licence. `fetch` downloads what the cache lacks and checks every file against
its pin; a file whose content differs is refused, so a build is always made from the sources
written here. The cache lives outside the repository (`~/.cache/thats-the-word/words`, or
`$THATSTHEWORD_WORDS_CACHE`): nothing fetched is ever committed, only what the pipeline writes.

The Wiktionary extracts are republished by kaikki.org every week and the older ones are not kept
there. When a pin can no longer be fetched, `build.py fetch --repin` takes the current files and
writes their new checksums here (the report says the date); the daily schedules are append-only,
so a new extract can change a meaning, never a day's word.
"""

import dataclasses
import gzip
import hashlib
import os
import pathlib
import shutil
import sys
import urllib.request

LIBREOFFICE_COMMIT = "c0865a85eb67dc377f2cf8e8fe2c4fa9e53f0dcb"
_LO = f"https://raw.githubusercontent.com/LibreOffice/dictionaries/{LIBREOFFICE_COMMIT}"


@dataclasses.dataclass(frozen=True)
class Source:
    key: str
    url: str
    file: str
    sha256: str
    size: int
    # "gzip": served gzip-encoded, stored as received, pinned by its decoded content.
    transfer: str = "plain"


@dataclasses.dataclass(frozen=True)
class Origin:
    """A source as the report and `licenses/README.md` credit it."""

    name: str
    version: str
    licence: str
    home: str
    sources: tuple[str, ...]
    note: str = ""


SOURCES = {
    s.key: s
    for s in (
        # The Italian Hunspell dictionary, LibreOffice's (version 5.1.1, 7 Nov 2022).
        Source(
            "it_IT.dic",
            f"{_LO}/it_IT/it_IT.dic",
            "hunspell/it_IT.dic",
            "bae1e3501dcd2a923669592493b3fde6c02aae7c7aab83bf5e5b49077e73dd64",
            1295083,
        ),
        Source(
            "it_IT.aff",
            f"{_LO}/it_IT/it_IT.aff",
            "hunspell/it_IT.aff",
            "951afaa19272f13555b8823e8bcf9ccf78f8fe1a07835bdfb912ab3e4d537c2b",
            70054,
        ),
        Source(
            "it_IT.readme",
            f"{_LO}/it_IT/README_it_IT.txt",
            "hunspell/README_it_IT.txt",
            "34c3e93595cf3cf5f3afc9bc0d98eea12593750383f1e82ed1d3ca29f9681283",
            38902,
        ),
        # The Spanish Hunspell dictionaries of the RLA-ES project (version 2.9): Spain's, Mexico's
        # and the Río de la Plata's, whose voseo has five-letter forms (tenés, sabés).
        Source(
            "es_ES.dic",
            f"{_LO}/es/es_ES.dic",
            "hunspell/es_ES.dic",
            "6975dddec3d5d2c676069537bc67b4b5f786c65c5d4cf6703a82acf779ac9ec1",
            715989,
        ),
        Source(
            "es_ES.aff",
            f"{_LO}/es/es_ES.aff",
            "hunspell/es_ES.aff",
            "e73a9bf8e1383f4986a5dc9e2fbed49371c0c61f511c626d15586bd433c1cad9",
            169202,
        ),
        Source(
            "es_MX.dic",
            f"{_LO}/es/es_MX.dic",
            "hunspell/es_MX.dic",
            "44ce35af220962c68f97962776639bef271f7d90a85ba924b539a36e33315f82",
            723507,
        ),
        Source(
            "es_MX.aff",
            f"{_LO}/es/es_MX.aff",
            "hunspell/es_MX.aff",
            "d966cb748e4a688ed75ec84b50c0835ac438e5325ee4fce703a093794f9cba7e",
            160081,
        ),
        Source(
            "es_AR.dic",
            f"{_LO}/es/es_AR.dic",
            "hunspell/es_AR.dic",
            "4fe3e193425bb3841ee218d36a9fc95d85b8daa8909363bdbba55a2f9f4e3d1d",
            707817,
        ),
        Source(
            "es_AR.aff",
            f"{_LO}/es/es_AR.aff",
            "hunspell/es_AR.aff",
            "e9a96514295f8664db270d024c345700ccb9410a384eb71aec0fe5f0931d8c70",
            171527,
        ),
        Source(
            "es.license",
            f"{_LO}/es/LICENSE.md",
            "hunspell/es_LICENSE.md",
            "2b70fd5cb8d3b34cc6ae322c969557a2270b99017594addb2531ef5ffd9e1b17",
            991,
        ),
        # SCOWL, the last release in its classic form, with its lists already expanded.
        Source(
            "scowl",
            "https://downloads.sourceforge.net/wordlist/scowl-2020.12.07.tar.gz",
            "scowl/scowl-2020.12.07.tar.gz",
            "5587667caa20c4891390c2d42dbb4d5c4c3f41bee77af1457ece3ba23fb859cc",
            2569810,
        ),
        # wordfreq's wheel, read as a zip for its data files (nothing of it is installed or run).
        Source(
            "wordfreq",
            "https://files.pythonhosted.org/packages/24/61/62835c475d69872d30689f284497853fe33fe1d6dd18f57346d13305861d/wordfreq-3.1.1-py3-none-any.whl",
            "wordfreq/wordfreq-3.1.1-py3-none-any.whl",
            "4b1c6ecffc6198be3396d5cf871c4423ca71c907c231348d352dd54d62b97473",
            56834549,
        ),
        # Wiktionary as wiktextract reads it (kaikki.org): the English edition's Italian, Spanish
        # and English entries (the dump of 1 Oct 2026), and the Italian and Spanish editions.
        Source(
            "wiktionary.en.it",
            "https://kaikki.org/dictionary/Italian/kaikki.org-dictionary-Italian.jsonl",
            "wiktionary/kaikki.org-dictionary-Italian.jsonl.gz",
            "f112deb9747b7f33f4247da4999dfba0aa739bb4aaff9dd42fdf0615918a9c11",
            779439733,
            transfer="gzip",
        ),
        Source(
            "wiktionary.en.es",
            "https://kaikki.org/dictionary/Spanish/kaikki.org-dictionary-Spanish.jsonl",
            "wiktionary/kaikki.org-dictionary-Spanish.jsonl.gz",
            "336903d1290968d77bd361c620d7d799ad7a7a9bc6ad353ec35954f2e15fbe6f",
            1119672965,
            transfer="gzip",
        ),
        Source(
            "wiktionary.en.en",
            "https://kaikki.org/dictionary/English/kaikki.org-dictionary-English.jsonl",
            "wiktionary/kaikki.org-dictionary-English.jsonl.gz",
            "ce2b80e5ad1132f14cedd2f531a606e1f0c6aeab422d50515e88db13a16a59a7",
            3431211190,
            transfer="gzip",
        ),
        Source(
            "wiktionary.it",
            "https://kaikki.org/dictionary/downloads/it/it-extract.jsonl.gz",
            "wiktionary/it-extract.jsonl.gz",
            "9f35d5ac30f2fbab4774eb3e81ec6394806bd2723f802b405e7e9901185d5229",
            43849715,
            transfer="gzipfile",
        ),
        Source(
            "wiktionary.es",
            "https://kaikki.org/dictionary/downloads/es/es-extract.jsonl.gz",
            "wiktionary/es-extract.jsonl.gz",
            "0a7a02456ecf34dd3419e0d5adf4f18da5531487dde31829d126afe86eade861",
            103226090,
            transfer="gzipfile",
        ),
    )
}

ORIGINS = (
    Origin(
        "Italian Hunspell dictionary (LibreItalia)",
        f"5.1.1 (7 Nov 2022), LibreOffice/dictionaries at {LIBREOFFICE_COMMIT[:10]}",
        "GPL-3.0",
        "https://github.com/LibreOffice/dictionaries/tree/master/it_IT",
        ("it_IT.dic", "it_IT.aff", "it_IT.readme"),
        "Every Italian form the game accepts, expanded from its affixes by this pipeline.",
    ),
    Origin(
        "Spanish Hunspell dictionaries (RLA-ES)",
        f"2.9, es_ES, es_MX and es_AR, LibreOffice/dictionaries at {LIBREOFFICE_COMMIT[:10]}",
        "GPL-3.0-or-later, LGPL-3.0-or-later or MPL-1.1, at the user's choice: used under GPL-3.0-or-later",
        "https://github.com/sbosio/rla-es",
        ("es_ES.dic", "es_ES.aff", "es_MX.dic", "es_MX.aff", "es_AR.dic", "es_AR.aff", "es.license"),
        "Every Spanish form the game accepts, expanded from its affixes by this pipeline.",
    ),
    Origin(
        "SCOWL (Spell Checker Oriented Word Lists), Kevin Atkinson",
        "2020.12.07",
        "SCOWL's own permissive licence (MIT-like, with the notices of its sources kept)",
        "http://wordlist.aspell.net/",
        ("scowl",),
        "Every English form the game accepts, American and British, up to size 70.",
    ),
    Origin(
        "wordfreq, Robyn Speer",
        "3.1.1 (its data frozen in 2024)",
        "Code Apache-2.0 (not used); data CC BY-SA 4.0",
        "https://github.com/rspeer/wordfreq",
        ("wordfreq",),
        "How common a word is: the four levels.",
    ),
    Origin(
        "Wiktionary, through wiktextract (kaikki.org)",
        "the English edition's dump of 1 Oct 2026; the Italian and Spanish editions' of Oct 2026",
        "CC BY-SA 4.0 (and GFDL); used under CC BY-SA 4.0, one-way compatible with GPL-3.0",
        "https://kaikki.org/",
        ("wiktionary.en.it", "wiktionary.en.es", "wiktionary.en.en", "wiktionary.it", "wiktionary.es"),
        "Which words are lemmas, their meanings and translations, their written forms, the tags that keep a word out.",
    ),
)


def cache_dir() -> pathlib.Path:
    root = os.environ.get("THATSTHEWORD_WORDS_CACHE")
    if root:
        return pathlib.Path(root)
    return pathlib.Path.home() / ".cache" / "thats-the-word" / "words"


def path(key: str) -> pathlib.Path:
    return cache_dir() / SOURCES[key].file


def content_digest(file: pathlib.Path, transfer: str) -> tuple[str, int]:
    """The SHA-256 and the size of a file's content (decoded, for what was stored compressed)."""
    digest = hashlib.sha256()
    size = 0
    opener = gzip.open if transfer in ("gzip",) else open
    with opener(file, "rb") as stream:
        while chunk := stream.read(1 << 20):
            digest.update(chunk)
            size += len(chunk)
    return digest.hexdigest(), size


def _download(source: Source, target: pathlib.Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    part = target.with_suffix(target.suffix + ".part")
    headers = {"User-Agent": "thats-the-word-words/1"}
    if source.transfer == "gzip":
        headers["Accept-Encoding"] = "gzip"
    request = urllib.request.Request(source.url, headers=headers)
    with urllib.request.urlopen(request) as response, open(part, "wb") as out:
        if source.transfer == "gzip" and response.headers.get("Content-Encoding") != "gzip":
            # The server sent it plain: store it compressed, as the cache expects.
            with gzip.GzipFile(fileobj=out, mode="wb", mtime=0) as packed:
                shutil.copyfileobj(response, packed, 1 << 20)
        else:
            shutil.copyfileobj(response, out, 1 << 20)
    part.replace(target)


def verify(key: str) -> tuple[bool, str, int]:
    source = SOURCES[key]
    digest, size = content_digest(path(key), source.transfer)
    return digest == source.sha256, digest, size


def fetch(keys=None, log=print) -> list[str]:
    """Downloads what the cache lacks; returns the keys whose content differs from its pin."""
    wrong = []
    for key in keys or SOURCES:
        target = path(key)
        if not target.exists():
            log(f"fetch  {key}  {SOURCES[key].url}")
            _download(SOURCES[key], target)
        ok, digest, size = verify(key)
        if not ok:
            wrong.append(key)
            log(f"DIFFERS {key}: content {digest} ({size} bytes), pinned {SOURCES[key].sha256}")
        else:
            log(f"ok     {key}")
    return wrong


def repin(keys, log=print) -> dict[str, tuple[str, int]]:
    """Downloads the current files again and returns their new checksums, to be written above."""
    found = {}
    for key in keys:
        target = path(key)
        if target.exists():
            target.unlink()
        _download(SOURCES[key], target)
        digest, size = content_digest(target, SOURCES[key].transfer)
        found[key] = (digest, size)
        log(f"{key}: sha256 {digest}, {size} bytes")
    return found


if __name__ == "__main__":
    sys.exit(1 if fetch() else 0)
