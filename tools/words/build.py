"""That's The Word's words, rebuilt from the pinned sources (PLANNING.md §6, tools/words/README.md).

    python3 tools/words/build.py                 # fetch what is missing, build, write every file
    python3 tools/words/build.py --check         # build, and fail if a file in the repository differs
    python3 tools/words/build.py fetch           # fetch and check the sources only
    python3 tools/words/build.py fetch --repin wiktionary.it ...   # take a source's current file

The first run downloads about 1 GB into the cache (`~/.cache/thats-the-word/words`, or
`$THATSTHEWORD_WORDS_CACHE`) and reduces Wiktionary once (about ten minutes); a build after that
takes two or three minutes and needs about 5 GB of memory.
"""

import argparse
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from pipeline import build, config, sources  # noqa: E402


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("command", nargs="?", default="build", choices=("build", "fetch"))
    parser.add_argument("--check", action="store_true", help="compare with the committed files, write nothing")
    parser.add_argument("--repin", nargs="+", metavar="KEY", help="with fetch: take these sources' current files")
    args = parser.parse_args(argv)

    if args.command == "fetch":
        if args.repin:
            found = sources.repin(args.repin)
            print("\nWrite these into tools/words/pipeline/sources.py, then rebuild:")
            for key, (digest, size) in found.items():
                print(f"  {key}: sha256 {digest}, size {size}")
            return 0
        return 1 if sources.fetch() else 0

    built = build.run()
    missing = {g: m for g, m in built.common_missing.items() if m}
    if missing:
        for g, words in missing.items():
            print(f"{g}: common words not accepted: {', '.join(words)}", file=sys.stderr)
        return 1
    if args.check:
        differ = build.check(built)
        for path in differ:
            print(f"differs: {path.relative_to(config.ROOT)}", file=sys.stderr)
        print("the same bytes" if not differ else f"{len(differ)} files differ")
        return 1 if differ else 0
    build.write(built)
    print(f"wrote {len(built.files)} files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
