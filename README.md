# Integrity Coded Database (ICDB)

## Initial Setup

At this time, this project only supports Linux. 

To be able to build and run against a database, the following must be installed:

1. Maven
2. MySQL

Then run the following commands to build the project:
```
$ git clone https://github.com/Hyperparticle/IntegrityCodedDatabase.git
$ cd IntegrityCodedDatabase/ICDB
$ make
```

## Running the ICDB tool

The arguments for interacting with the ICDB tool is as follows:

```
$ ICDBTool [-f <config-file>] [command] [options]
```

All interactions with the tool will require a config file containing a JSON object with several parameters:

```
ip          - the target MySQL database IP address
port        - the port the database is running on
user        - database user
password    - database password (if any)
schema      - database schema (for conversion and query execution)
key         - 128-bit key encoded as a hexadecimal string
algorithm   - the encryption algorithm to use (RSA, AES, or SHA)
granularity - use code per field or code per tuple (FIELD or TUPLE)
```

An example config file is located under `src/main/resources/config-sample.json`.

There are a few commands available:

```
convert-db      - Converts an existing DB to an ICDB (both schema and data)
convert-data    - (Coming Soon) Generates integrity codes for existing DB data
convert-query   - (Coming Soon) Converts a DB query to an ICDB query
execute-query   - (Coming Soon) Executes an ICDB query and verifies all returned data
```

### Example Commands

```
$ cd <project-root>/ICDB
$ ICDBTool -f ./src/main/resources/config-sample.json convert-db
```

## Sources

This project uses example databases generated for MySQL use.

- Employees DB: https://github.com/datacharmer/test_db