# Database configuration

Curated Shelves uses an **in-memory runtime cache** for shelf and book state, with an **SQL database as the persistent backing store**.

Runtime behavior:
- shelf and book reads are served from memory after plugin initialization
- create/update/delete persistence is performed asynchronously through the existing scheduler/service flow
- the database is the durable store across restarts

## Supported backends

### SQLite
SQLite is the default backend.

```yaml
storage:
  type: sqlite
  table-prefix: ''
  sqlite:
    file: library.db
```

Notes:
- `storage.sqlite.file` is resolved inside the plugin data folder
- it must be a simple file name, not a path
- `table-prefix` defaults to empty to preserve the previous table names unless you explicitly configure a prefix

### MySQL
MySQL is supported through HikariCP and MySQL Connector/J.

```yaml
storage:
  type: mysql
  table-prefix: wild_
  mysql:
    host: localhost
    port: 3306
    database: curated_shelves
    username: root
    password: change-me
    maximum-pool-size: 10
    connection-timeout-millis: 10000
```

Notes:
- `table-prefix` is applied to all Curated Shelves tables and indexes
- the MySQL backend uses a pooled `HikariDataSource`
- the plugin keeps the same in-memory cache behavior regardless of backend

## Table prefix

`storage.table-prefix` may contain only:
- letters
- numbers
- underscores

Examples:
- `''`
- `wild_`
- `network1_`

The prefix is applied to:
- `shelves`
- `books`
- supporting indexes / constraints where applicable

## Reload behavior

`/library reload` reloads configuration values, but **storage backend and table-prefix changes require a full server restart** to take effect.

## Runtime model

Curated Shelves remains:
- **async** for persistence operations
- **memory-cached** for normal gameplay reads
- **DB-backed** for restart durability

This means the backend choice changes persistent storage and connection behavior, but does not replace the existing runtime cache architecture.
