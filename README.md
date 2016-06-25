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
1. convert-db      - Converts an existing DB to an ICDB (both schema and data)
2. convert-data    - (Coming Soon) Generates integrity codes for existing DB data
3. convert-query   - Converts a DB query to an ICDB query
4. execute-query   - (Coming Soon) Executes an ICDB query and verifies all returned data
```

There are also additional options for each command.

### Convert DB Command

```
convert-db [--skip-duplicate] [--skip-schema] [--skip-data]
```

The `convert-db` command has 3 phases, any of which can be skipped:
--skip-duplicate - If set, the duplicate DB step will be skipped
--skip-schema - If set, the schema conversion step will be skipped
--skip-data - If set, the data conversion step will be skipped

Example:
```
$ cd <project-root>/ICDB
$ ICDBTool -f ./src/main/resources/config-sample.json convert-db --skip-duplicate
```

### Convert Query Command

```
convert-query [-q query] [-g granularity]
```

The `convert-query` command takes the SQL query as an input and converts it to an ICDB query. The conversion requires:
-q - The SQL query, passed in as a string
-g - The granularity of the ICDB query (TUPLE or FIELD), by default TUPLE

Example:
```
$ cd <project-root>/ICDB
$ ICDBTool -f ./src/main/resources/config-sample.json convert-query -q "SELECT * FROM employees;" -g "FIELD"
```

### Execute Query Command

```
execute-query [-q query] [-g granularity] [--convert]
```

The `convert-data` command takes a SQL query as input, executes, then verifies any returned data.
-q - The SQL query, passed in as a string
-g - The granularity of the ICDB query (TUPLE or FIELD), by default TUPLE
--convert - Convert the query to ICDB before executing, false by default

## Sources

This project uses example databases generated for MySQL use.

- Employees DB: https://github.com/datacharmer/test_db
