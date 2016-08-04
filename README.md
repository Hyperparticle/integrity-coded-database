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

Make will simply run the command `mvn package -DskipTests` to compile `icdb.jar` under the `target` folder.

## Running the ICDB tool

The arguments for interacting with the ICDB tool is as follows:

```
$ icdb [-c config-file] [command] [options]
```

`icdb` is a bash script that simply runs the compiled jar. This can be run directly:

```
$ java -jar target/icdb-capsule.jar [-c config-file] [command] [options]
```

All interactions with the tool will require a config file containing a JSON object with several parameters:

```
ip          - the target MySQL database IP address
port        - the port the database is running on
user        - database user
password    - database password (if any)
schema      - database schema to use (for conversion)
icdbSchema  - ICDB database schema name (for execution and verification)
algorithm   - the encryption algorithm to use (RSA, AES, or SHA)
granularity - use code per field or code per tuple (FIELD or TUPLE)
macKey      - 128-bit MAC key encoded as a base64 string
rsaKeyFile  - PEM file containing public and private RSA keys
```

For convenience, a config file is given at `./ICDB/config.json`, which will be loaded by default if the `-c` option is not specified.

The default config provides the following JSON object:
```
{
  "ip": "localhost",
  "port": 3306,
  "user": "root",
  "password": "",
  "schema": "employees",
  "icdbSchema": "employees_icdb",
  "algorithm": "SHA",
  "granularity": "TUPLE",
  "macKey": "qyPTqFrPGUpxcIo9sz2MdQ==",
  "rsaKeyFile": "key.pem"
}
```

### Commands available

```
1. convert-db      - Converts an existing DB to an ICDB (both schema and data)
2. convert-query   - Converts a DB query to an ICDB query
3. execute-query   - Executes an ICDB query and verifies all returned data
```

There are also additional options for each command.

### Convert DB Command

```
convert-db [--skip-duplicate] [--skip-schema] [--skip-data] [--skip-load]
```

The `convert-db` command has 4 phases, any of which can be skipped:
--skip-duplicate - If set, the duplicate DB step will be skipped
--skip-schema - If set, the schema conversion step will be skipped
--skip-data - If set, the data conversion step will be skipped
--skip-load - If set, the data load step will be skipped

Example:
```
$ cd <project-root>/ICDB
$ ICDBTool -c ./src/main/resources/config-sample.json convert-db --skip-duplicate
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
$ ICDBTool -c ./src/main/resources/config-sample.json convert-query -q "SELECT * FROM employees;" -g "FIELD"
```

### Execute Query Command

```
execute-query [-q query] [--convert]
```

The `convert-data` command takes a SQL query as input, executes, then verifies any returned data.
-q - The SQL query, passed in as a string
--convert - Convert the query to ICDB before executing, false by default

## Sources

This project uses example databases generated for MySQL use.

- Employees DB: https://github.com/datacharmer/test_db

## Troubleshooting

If you get the error:
`The server time zone value 'MDT' is unrecognized or represents more than one time zone.`
run the query in MySQL:
`SET time_zone = 'America/Denver'; SET global time_zone = 'America/Denver';`
or whatever timezone city you live nearby. This is a workaround for a VERY ANNOYING bug in MySQL.
