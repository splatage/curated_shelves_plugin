# Curated Shelves Plugin

Current stage: **Slice 1 complete — repo/bootstrap only**.

This repository currently contains only the initial bootstrap pass for the locked scope:
- Gradle Kotlin DSL build
- Gradle wrapper
- Java 21 + Paper API baseline
- `plugin.yml` and `config.yml`
- single implementation tree under `com.splatage.curatedshelves`
- Folia-safe scheduler facade skeleton
- plugin main class and config loader

What is intentionally **not implemented yet**:
- SQLite persistence
- Librarian's Seal recipe/item
- shelf marking and block PDC
- lectern badge
- GUI
- deposit/read/remove rules
- destruction spill handling

Remaining scope after this pass:
- Slice 2: persistence and domain storage model
- Slice 3: marking and Librarian's Seal
- Slice 4: badge system
- Slice 5: GUI and interaction
- Slice 6: destruction spill
- Slice 7: audit and cleanup
