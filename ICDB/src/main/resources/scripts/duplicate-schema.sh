#!/usr/bin/env bash

# This script will export a DB schema and generate a duplicate DB with the specified name

if [ "$#" -ne 2 ]; then
    echo "A db name must be specified as an argument"
    echo "An icdb target name must be specified as an argument"
    exit 1
fi

export_path=./tmp
schema_file=${export_path}/db-files/schema/$1-schema.sql
icdb_name=$2

# Create folders if it they do not exist
mkdir -p ${export_path}/db-files/schema
mkdir -p ${export_path}/db-files/data
mkdir -p ${export_path}/icdb-files/schema
mkdir -p ${export_path}/icdb-files/data




# Use mysqldump to export the DB schema
echo "Dumping database schema '$1'."
mysqldump -u root --no-data $1 > ${schema_file}

# Create a new database with the same schema
echo "Creating Database '${icdb_name}'"
mysql -e "DROP DATABASE IF EXISTS ${icdb_name}" -u "root"
mysql -e "CREATE DATABASE ${icdb_name}" -u "root"
mysql -e "USE ${icdb_name}; SOURCE ${schema_file};" -u "root"
echo "Done."
