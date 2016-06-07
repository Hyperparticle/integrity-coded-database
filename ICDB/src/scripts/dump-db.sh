#!/usr/bin/env bash

if [ "$#" -ne 1 ]; then
    echo "A database name must be specified as an argument"
    exit 1
fi

export_path=./tmp/db-files

mkdir -p ${export_path}/schema

rm -f ${export_path}/schema/*.sql

# Use mysqldump to export the DB schema
echo "Dumping database schema '$1'."
mysqldump --no-data --skip-comments $1 \
    |  \
    > ${export_path}/schema/$1-schema.sql
echo "Done."