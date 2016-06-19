# Integrity Coded Database (ICDB)

## Initial Setup

At this time, this project only supports Linux. To be able to build and run against a database, the following must be installed:

1. Maven
2. MySQL

Then run the following commands to build the project:
```
$ git clone https://github.com/Hyperparticle/IntegrityCodedDatabase.git
$ cd IntegrityCodedDatabase/ICDB
$ make
```

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

## Sources

- Employees DB: https://github.com/datacharmer/test_db