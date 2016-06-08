#!/usr/bin/env bash

# This script will export a DB schema and generate a duplicate DB with -icdb appended to it

if [ "$#" -ne 1 ]; then
    echo "A database name must be specified as an argument"
    exit 1
fi

export_path=./tmp/db-files
schema_file=${export_path}/schema/$1-schema.sql
icdb_name=$1_icdb

mkdir -p ${export_path}/schema

# Use mysqldump to export the DB schema
echo "Dumping database schema '$1'."
mysqldump --no-data --skip-comments $1 > ${schema_file}

# Create a new database with the same schema
echo "Creating Database '${icdb_name}'"
mysql -e "DROP DATABASE IF EXISTS ${icdb_name}"
mysql -e "CREATE DATABASE ${icdb_name}"
mysql -e "USE ${icdb_name}; SOURCE ${schema_file};"
echo "Done."
