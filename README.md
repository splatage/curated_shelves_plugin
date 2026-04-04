# Curated Shelves Plugin

A Folia-compatible Paper plugin that adds plugin-managed Library Shelves to marked chiseled bookshelves.

## Build

```bash
./gradlew build
```

## Runtime baseline

- Paper API `1.21.11-R0.1-SNAPSHOT`
- Java 21
- `folia-supported: true`

## Locked scope in this initial repo

- Ordinary chiseled bookshelves remain vanilla.
- A crafted **Librarian's Seal** marks a chiseled bookshelf as a Library Shelf.
- Admins may also mark and unmark shelves with `/library mark` and `/library unmark`.
- Marked shelves show a lectern badge and open a plugin-managed GUI.
- GUI size is configurable by `rows: 1-4`; total slots are `rows * 9`.
- Stored books are plugin-managed rather than stored in the real shelf inventory.
- Tooltips show title, author, and shelved by.
- Removal is controlled by `shelvedBy`, with optional author rules and a librarian override permission.
- Breaking or destroying a Library Shelf spills stored books.
- Claims/build plugins remain responsible for block protection.
