"""That's The Word's words, made from free sources (PLANNING.md §6).

The package is the pipeline behind `tools/words/build.py`: it fetches the pinned sources into a
cache outside the repository, reduces the large ones to what the game reads, and writes every
file the app ships, the report and the samples a person reads. It uses Python's standard library
alone, so it runs wherever Python 3.11 runs, with nothing to install.
"""
