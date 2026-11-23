# Changelog

All notable changes to Loom are documented here.

## [1.1] – 2026-11-23

A full bug and performance pass, new daily-use features, and safer backups. Updating from 1.0 keeps your data.

### Added
- **Reminders** – water, supplements, skincare (AM/PM), task deadlines, fasting finished and an evening recap, each with its own time and switch; notification actions (+250 ml, Mark taken, Done).
- **Backups** – ZIP export including images; weekly automatic backups to a chosen folder (latest 5 kept). Older `.json` backups still import.
- **Trends & Streaks** – weight, sleep, calories, protein and water over 7/30/90 days; workout, water, supplement and skincare streaks; Calendar days shaded by activity.
- **Widgets** – Water (one-tap add) and Today (macros, fasting, next task).
- **Health Connect** – sync weight, sleep and water.
- **Food** – favourites, recent foods, copy yesterday's meals, meal templates, full-text search.
- **Workouts** – last session, personal records, pre-filled sets, progress chart.
- **Spaces** – undo/redo, duplicate block, "Linked from" backlinks, nested search, audio recording.
- **Appearance** – Material You colours, pure black (AMOLED), accent colours.
- **Calendar** – optional Jalali calendar with Persian month names; choice of first weekday.

### Fixed
- Future app updates could wipe all data (database migrations were never applied).
- A bad import file could leave the app empty; imports are now validated and all-or-nothing.
- Pages in Spaces gained a hidden nested block every time they were opened (cleaned up automatically).
- Edits in Spaces could be lost when switching quickly between pages.
- Logs went to the wrong day if the app stayed open past midnight; quick taps could overwrite each other.
- Crashes from a 0 supplement interval, a 0 water goal and uneven tables.
- Recurring tasks repeating on the same date, and date pickers choosing the previous day.
- Done tasks are hidden instead of deleted, so Calendar history keeps them.
- PIN is stored hashed with attempt lockout; private spaces' cards no longer appear on Home.
- Typed text in dialogs survives rotation.

## [1.0] – 2026-11-01

First public release.

- **Spaces** – modular pages built from 30+ block types: text & lists, tables, databases, kanban, timeline, calendar, gallery, charts, progress trackers, sketch, code, math, media, cards and nested Spaces.
- **Health** – hydration, nutrition (7,000+ food pantry), workouts, skincare and supplements.
- **Dashboard** – customizable home with daily agenda, nutrition, hydration and pinned cards.
- **Calendar** – events, tasks, checklists and a per-day health summary.
- **Diary & Notes** – tagged, searchable notes with PIN lock.
- **Privacy** – fully offline, PIN & biometric lock, JSON export / import.
