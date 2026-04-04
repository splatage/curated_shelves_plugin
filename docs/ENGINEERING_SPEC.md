# Curated Shelves Plugin — Engineering Spec

## 1. Product definition

This plugin adds a **plugin-managed library system** to Minecraft.

A **Library Shelf** is a **chiseled bookshelf** that has been explicitly marked by the plugin. Ordinary chiseled bookshelves remain vanilla.

A marked Library Shelf:
- shows a **lectern badge**
- opens a **plugin-managed GUI**
- stores books in plugin data, not in the vanilla shelf slots
- lets anyone **read**
- restricts **removal** by custody rules
- **spills stored books** if the shelf is destroyed

Block protection is **out of scope** for this plugin. Claims/build plugins remain the source of truth for who may break blocks.

## 2. Locked scope

### In scope
- Marking a chiseled bookshelf as a Library Shelf via:
  - crafted **Librarian’s Seal**
  - admin commands
- Plugin-managed storage per shelf
- GUI rows configurable from **1 to 4**, total slots = `rows * 9`
- Tooltip display of:
  - title
  - author
  - shelved by
- Config options:
  - `author-must-deposit`
  - `author-can-remove-own-book`
- Librarian override permission to remove any book
- Lectern badge visual marker
- Spill books on destruction
- Folia-compatible implementation
- Async where advantageous

### Out of scope
- Universal behavior on all shelves
- Vanilla shelf slot extension
- Comparator/redstone behavior
- Piston relocation support
- Search/sort/filter UX
- MySQL/networked backends
- Resource-pack-based custom models
- Hidden fallback systems

## 3. Runtime and build baseline

Use a standard **Gradle Kotlin DSL** Paper plugin project with `src/main/java` and `src/main/resources`.

Use a normal `plugin.yml`, with:
- `api-version: '1.21.11'`
- `folia-supported: true`

Stay on the public Paper API.

## 4. Core design model

### Shelf identity
A Library Shelf is a `CHISELED_BOOKSHELF` block with plugin metadata attached to the block entity.

Use **Persistent Data Container** on the block’s `TileState` to store:
- `libraryShelf = true`
- `shelfId`
- optionally a version marker for future migrations

### Storage model
Books are **not** stored in the real shelf inventory.

Instead, each marked shelf has plugin-managed entries:
- `bookId`
- `shelfId`
- `slotIndex`
- serialized book item
- title snapshot
- author snapshot
- `shelvedByUuid`
- `shelvedByName`
- timestamps

This keeps the shelf as the in-world anchor and the plugin as the storage authority.

## 5. User-facing behavior

### Marking
A player crafts a **Librarian’s Seal** and uses it on a chiseled bookshelf.

That bookshelf becomes a Library Shelf:
- shelf marker written to block PDC
- lectern badge spawned
- shelf enters plugin control

Admins may also:
- `/library mark`
- `/library unmark`

### Opening
Right-clicking a Library Shelf opens the custom GUI.

### Deposit
Players deposit supported books into empty library slots.

Config-controlled rule:
- `author-must-deposit: true`  
  only the book author may deposit
- `author-must-deposit: false`  
  anyone may deposit

When deposited, custody is recorded as:
- `shelvedByUuid`
- `shelvedByName`

### Read
Anyone may click a stored book to open the normal reading view.

### Remove
A book may be removed by:
- the `shelvedBy` player
- optionally the author, if `author-can-remove-own-book: true`
- any player with librarian override permission

### Destruction
If the Library Shelf is destroyed:
- all stored books are dropped into the world
- shelf storage is cleared
- badge is removed

Claims/build plugins remain responsible for preventing unauthorized breaking.

## 6. Supported item rules

### Supported for storage
Initial scope should support:
- `WRITTEN_BOOK`

That is the cleanest first boundary.

### Not stored in v1
- normal books
- enchanted books
- writable books
- non-book items

Those can be rejected cleanly with user feedback.

## 7. GUI specification

Config:
- `rows: 1..4`
- slot count = `rows * 9`

Rules:
- every GUI slot is a real library slot
- empty slots accept supported deposits
- occupied slots show tooltip:
  - `Title: <title>`
  - `Author: <author>`
  - `Shelved by: <name>`

Interaction:
- click occupied slot:
  - if removable: open **Read / Remove**
  - otherwise: open **Read**
- click empty slot while holding supported book:
  - attempt deposit

The GUI is the **only** authoritative interaction surface for library content.

## 8. Recipe specification

Use a shaped recipe for **Librarian’s Seal**.

Recipe theme, as already locked:
- chiseled bookshelf / shelf
- lectern
- barrel
- candle
- honeycomb / beeswax

The exact recipe pattern should be treated as data in config or as a single canonical in-code definition, but not duplicated in multiple places.

## 9. Visual marker

Use an **ItemDisplay** as the lectern badge.

Why:
- no normal item-frame interaction
- cleaner positioning
- visual-only role

The badge is **derived state**, not authoritative state:
- if missing, it may be recreated
- if the shelf is removed, it is deleted
- the badge never defines whether a shelf is a library; block PDC does

## 10. Persistence design

Use **SQLite** in v1.

Reason:
- local plugin data
- straightforward schema
- no external service requirement

### Initial schema

`shelves`
- `shelf_id` TEXT PRIMARY KEY
- `world_uuid` TEXT NOT NULL
- `x` INTEGER NOT NULL
- `y` INTEGER NOT NULL
- `z` INTEGER NOT NULL
- `rows` INTEGER NOT NULL
- `created_at` INTEGER NOT NULL
- `updated_at` INTEGER NOT NULL

`books`
- `book_id` TEXT PRIMARY KEY
- `shelf_id` TEXT NOT NULL
- `slot_index` INTEGER NOT NULL
- `item_data` BLOB or TEXT NOT NULL
- `title` TEXT
- `author` TEXT
- `shelved_by_uuid` TEXT NOT NULL
- `shelved_by_name` TEXT NOT NULL
- `created_at` INTEGER NOT NULL
- `updated_at` INTEGER NOT NULL

Constraint:
- unique `(shelf_id, slot_index)`

## 11. Folia-safe execution model

This is a hard requirement.

### Region-thread work
Use region scheduling for world/location-owned work:
- mark shelf
- unmark shelf
- inspect block state
- create/remove badge
- spill books
- handle destruction consequences

### Entity scheduling
Use entity scheduling for player-bound work:
- open inventory GUI
- open book UI
- send player-facing follow-up after async completion

### Async work
Use async where advantageous:
- SQLite reads/writes
- serialization/deserialization
- startup data loading
- cleanup/reconciliation planning
- config parsing/reload validation

### Rule
Never perform live world or entity mutations from async code.

## 12. Package and module layout

Use a small, explicit structure.

`com.splatage.curatedshelves`
- `CuratedShelvesPlugin`
- `config/`
  - `PluginConfig`
  - `ConfigLoader`
- `domain/`
  - `LibraryShelf`
  - `LibraryBook`
- `data/`
  - `ShelfRepository`
  - `SQLiteShelfRepository`
  - `SchemaManager`
- `service/`
  - `LibraryService`
  - `ShelfMarkerService`
  - `BadgeService`
  - `RecipeService`
  - `SpillService`
- `gui/`
  - `LibraryMenu`
  - `BookActionMenu`
- `listener/`
  - `ShelfInteractListener`
  - `ShelfDestroyListener`
  - `InventoryListener`
- `command/`
  - `LibraryCommand`
- `platform/`
  - `SchedulerFacade`
  - `FoliaSchedulerFacade`
- `util/`
  - `PdcKeys`
  - `BookCodec`
  - `Locations`
  - `Items`

No duplicate service layer. No alternate architecture tree.

## 13. Config specification

Initial `config.yml`:

```yaml
rows: 1
author-must-deposit: false
author-can-remove-own-book: false
```

That is enough for v1.

Defaults should favor custody via `shelvedBy`.

## 14. Command and permission specification

### Commands
- `/library mark`
- `/library unmark`
- `/library reload`

### Permissions
- `curatedshelves.use`
- `curatedshelves.librarian.remove.any`
- `curatedshelves.admin.mark`
- `curatedshelves.admin.unmark`
- `curatedshelves.admin.reload`

Keep the command surface small.

## 15. Non-functional requirements

### Drift safety
- one Gradle build system only
- one implementation tree only
- one persistence path only
- one canonical source of truth for shelf state

### Performance
- no polling-heavy badge scans as the primary model
- no lore-based hidden metadata
- no reflective NBT internals
- no synchronous DB work in hot interaction paths

### Failure behavior
- if DB write fails after a UI action, fail visibly and do not silently invent fallback storage
- if badge creation fails, shelf still works; badge can be reconciled later
- if a shelf record is missing but block is marked, treat it as repairable inconsistency, not as permission to invent parallel state

## 16. Acceptance criteria

The initial implementation is complete when all of these are true:

1. A player can craft a Librarian’s Seal and use it on a chiseled bookshelf.
2. The shelf becomes marked and shows a lectern badge.
3. Right-click opens the library GUI.
4. GUI capacity matches configured rows × 9.
5. A written book can be deposited into an empty slot.
6. Tooltip shows title, author, and shelved by.
7. A non-removing player can read but not remove.
8. `shelvedBy` can remove.
9. Librarian override can remove any.
10. Author restrictions honor both config flags.
11. Destroying the shelf spills stored books.
12. Normal chiseled bookshelves remain vanilla.
13. No alternate parallel codepath exists for the same behavior.
14. World mutations are not performed from async threads.

## 17. Engineering slices

### Slice 1
Repo scaffold:
- Gradle Kotlin DSL
- wrapper
- `plugin.yml`
- config
- plugin main
- scheduler façade

### Slice 2
Domain and persistence:
- SQLite schema
- shelf/book repository
- codec for stored books

### Slice 3
Shelf marking:
- Librarian’s Seal item
- recipe registration
- mark/unmark logic
- block PDC marker

### Slice 4
Badge system:
- ItemDisplay lectern badge
- create/remove/reconcile hooks

### Slice 5
GUI and interaction:
- open menu
- deposit/read/remove
- tooltip rendering
- permission checks

### Slice 6
Destruction:
- break/explosion/burn handling
- spill stored books
- cleanup record and badge

### Slice 7
Audit:
- Folia-safe scheduler review
- duplicate logic review
- dead code review
- config/permission/command contract review

## 18. Blunt recommendation

The cleanest first version is smaller than the full design space explored:
- written books only
- explicit library shelves only
- plugin-managed storage only
- SQLite only
- no piston/redstone cleverness
- no parallel architecture
- no helpful hidden fallback paths

That version is disciplined enough to build cleanly.
