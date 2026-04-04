# Library Plugin

A Paper/Folia-compatible Minecraft plugin that adds plugin-managed library shelves.

## Current scope

- Ordinary chiseled bookshelves remain vanilla.
- A crafted **Librarian's Seal** marks a chiseled bookshelf as a **Library Shelf**.
- Marked shelves show a lectern badge and open a plugin-managed GUI.
- GUI size is configured by `rows: 1-4` in `config.yml`.
- Stored books are plugin-managed written books.
- Tooltips show title, author, and shelved by.
- `author-must-deposit` and `author-can-remove-own-book` are configurable.
- Players with `library.librarian.remove.any` may remove any stored book.
- Breaking or destroying a library shelf spills stored books.
- Block protection remains the responsibility of claims/build plugins.

## Commands

- `/library mark`
- `/library unmark`

## Permissions

- `library.admin.mark`
- `library.admin.unmark`
- `library.librarian.remove.any`

## Build

Use Gradle in a Java 21 environment:

```bash
gradle build
```

This repo includes a `build.gradle.kts` for Paper API `1.21.11-R0.1-SNAPSHOT`.
