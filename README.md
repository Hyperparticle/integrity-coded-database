# Integrity Coded Database (ICDB)

## Prerequisites

To be able to run all functionality in this repo, the following must be installed:


## Available Commands

### Exporting an existing database

To export an existing database:
```
$ cd <project-root>/ICDB
$ bash ./src/scripts/export-db.sh <db-name>
```
where `<db-name>` is the name of the database you wish to export.

### Running the ICDB tool

There are several options available:

```
convert-data  [-f <path-to-file>] [-q <arg-list>] [--verify]
convert-query [-f <path-to-file>] [-q <arg-list>]
execute-query [-f <path-to-file>] [-q <arg-list>] [--convert] [--skip-verify]
```

