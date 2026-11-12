"""The daily schedules, frozen and append-only (PLANNING.md §4.6, §6.5).

Per language, the list of the dailies' words in order: daily number n is the entry n (from 1).
It is drawn from the solutions of the daily's levels (Base and Intermediate) and is **append-only**:
the committed file is read first, every entry already there is kept as it is, and only new entries
are added at its end, until the schedule reaches its length. A source that drops a word cannot
change a day: the word stays where it was, and the report names it.

Each new entry is drawn among the words used least so far that are not among the last half pool's
entries, with a generator seeded by the schedule's seed and the entry's number: every word comes
once before any comes twice, the order is the seed's, and the same files give the same schedule.
"""

import datetime
import hashlib
import random

from . import config


def days_needed(epoch: datetime.date = config.PROVISIONAL_EPOCH, years: int = config.SCHEDULE_YEARS) -> int:
    end = datetime.date(epoch.year + years, epoch.month, min(epoch.day, 28))
    return (end - epoch).days + 1


def _rng(language: str, number: int) -> random.Random:
    seed = hashlib.sha256(f"{config.DAILY_SEED}/{language}/{number}".encode()).digest()
    return random.Random(int.from_bytes(seed, "big"))


def extend(language: str, existing: list[str], pool: list[str], length: int) -> list[str]:
    """The existing schedule, unchanged, with new entries after it up to `length`."""
    if not pool:
        raise ValueError(f"{language}: no word for the daily schedule")
    schedule = list(existing)
    pool = sorted(set(pool))
    uses = {w: 0 for w in pool}
    for w in schedule:
        if w in uses:
            uses[w] += 1
    recent_span = max(1, len(pool) // 2)
    while len(schedule) < length:
        recent = set(schedule[-recent_span:])
        fewest = min(uses[w] for w in pool if w not in recent)
        choices = [w for w in pool if uses[w] == fewest and w not in recent]
        word = _rng(language, len(schedule) + 1).choice(choices)
        schedule.append(word)
        uses[word] += 1
    return schedule


def check_append_only(old: list[str], new: list[str]) -> None:
    if new[: len(old)] != old:
        first = next(i for i, (a, b) in enumerate(zip(old, new)) if a != b)
        raise ValueError(f"the schedule would change daily number {first + 1}: refused")


def years_left(count: int, epoch: datetime.date, today: datetime.date) -> float:
    """How many years of dailies are left after `today`."""
    last = epoch + datetime.timedelta(days=count - 1)
    return (last - today).days / 365.25
