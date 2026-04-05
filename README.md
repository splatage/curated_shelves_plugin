# Curated Shelves Plugin

Current stage: **development overlay with SQL-backed storage and admin shelf browsing**.

This repository currently contains:
- Gradle Kotlin DSL build and Gradle wrapper
- Java 21 + Paper API baseline
- `plugin.yml` and `config.yml`
- SQLite and MySQL storage backends
- configurable database table prefix
- async persistence with in-memory runtime cache
- Librarian's Seal recipe/item
- shelf marking via seal or `/library mark`
- plugin-managed Library Shelf GUI for reading, deposit, and removal
- admin curated-shelf browser GUI
- `shelvedBy` custody with config-controlled author behavior
- stable `authorUuid` capture when the author identity can be resolved safely at deposit time
- front-face lectern badge display
- destruction spill handling
- Folia-oriented scheduler facade
- startup cleanup for orphan books and duplicate persisted shelves at the same location
- runtime `bookId` index rather than whole-library scans for action lookups

Database documentation:
- see `docs/DATABASE.md`

Known verification gaps:
- this repo has not been fully runtime-validated on a live Paper/Folia server from within this environment
- Gradle compile/test execution could not be run here because the environment cannot reach `services.gradle.org`
- automated coverage is currently limited and does **not** cover the highest-risk runtime shelf lifecycle paths end to end
