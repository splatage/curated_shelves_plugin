# Curated Shelves Plugin

Current stage: **overlay with MySQL-capable storage backends**.

This repository currently contains:
- Gradle Kotlin DSL build and Gradle wrapper
- Java 21 + Paper API baseline
- `plugin.yml` and `config.yml`
- SQLite and MySQL storage backends
- configurable database table prefix
- async service writes with in-memory runtime cache backed by persistent SQL storage
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
- targeted tests exist for config and the highest-risk service-layer integrity paths, but they are not exhaustive end-to-end Bukkit integration tests
