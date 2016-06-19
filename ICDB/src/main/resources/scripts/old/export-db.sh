#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "A database name must be specified as an argument"
    exit 1
fi

export_script=./src/scripts/sql/export-db.sql
export_path=./tmp/db-files

mkdir -p ./tmp/db-files/schema
mkdir -p ./tmp/db-files/data

rm -f ./tmp/db-files/schema/*.unl
rm -f ./tmp/db-files/data/*.unl

# Call the procedure to export schema and data (.unl) files
echo "Exporting database '$1'"
mysqldump --no-data --skip-comments $1 > ${export_path}/schema/$1-schema.sql
mysql -e "USE $1; source $export_script;"
