import collections
import datetime
import unittest

from pipeline import config, schedule


class ScheduleTest(unittest.TestCase):
    POOL = [f"w{i:03d}" for i in range(50)]

    def test_ten_years_of_dailies(self):
        self.assertGreaterEqual(schedule.days_needed(), 3652)

    def test_every_word_comes_once_before_any_comes_twice(self):
        s = schedule.extend("it", [], self.POOL, 120)
        self.assertEqual(len(s), 120)
        self.assertEqual(sorted(s[:50]), sorted(self.POOL))
        self.assertEqual(sorted(s[50:100]), sorted(self.POOL))
        counts = collections.Counter(s)
        self.assertLessEqual(max(counts.values()) - min(counts.values()), 1)

    def test_the_same_inputs_give_the_same_schedule(self):
        self.assertEqual(schedule.extend("es", [], self.POOL, 80), schedule.extend("es", [], self.POOL, 80))
        self.assertNotEqual(schedule.extend("es", [], self.POOL, 80), schedule.extend("it", [], self.POOL, 80))

    def test_extending_keeps_every_entry_already_there(self):
        first = schedule.extend("en", [], self.POOL, 70)
        longer = schedule.extend("en", first, self.POOL, 140)
        self.assertEqual(longer[:70], first)
        # A pool that changed later (a word gone, a word new) never moves a day already written.
        changed = self.POOL[1:] + ["new01"]
        again = schedule.extend("en", first, changed, 140)
        self.assertEqual(again[:70], first)
        self.assertIn("new01", again[70:])
        self.assertNotIn(self.POOL[0], again[70:])

    def test_a_changed_entry_is_refused(self):
        old = ["gatto", "fiume", "notte"]
        with self.assertRaises(ValueError):
            schedule.check_append_only(old, ["gatto", "fiore", "notte", "porta"])
        schedule.check_append_only(old, old + ["porta"])

    def test_the_years_left_after_a_date(self):
        epoch = datetime.date(2027, 1, 1)
        self.assertAlmostEqual(schedule.years_left(366, epoch, epoch), 1.0, places=2)
        self.assertLess(schedule.years_left(366, epoch, datetime.date(2027, 7, 1)), 0.6)

    def test_the_daily_draws_from_base_and_intermediate(self):
        self.assertEqual(config.DAILY_LEVELS, (0, 1))


if __name__ == "__main__":
    unittest.main()
