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
convert-data  [-i <input-directory>] [-k <keyfile-path>] [-o <output-directory>] 
[-c <cipher-type>] [-g <granularity>] [-d <delimiter>]
```

### Example Commands

```
convert-data -i ./tmp/db-files/data -k ./src/main/resources/keyfile-sample -o ./tmp/converted-db-files/data
```

## TODO
1. Dump existing DB Schema to a SQL file using mysqldump
2. Duplicate the DB with the SQL file, replacing an existing instance if it exists
3. Alter the duplicate DB to conform to ICDB Schema standards (Extra columns in each table)
4. 
