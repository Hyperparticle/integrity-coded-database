#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "A database name must be specified as an argument"
    exit 1
fi

echo "Generating ICDB from '$1'"

# Create directories (to keep the files) if they don't exist
mkdir -p ./tmp/database-files/schema
mkdir -p ./tmp/database-files/data
#mkdir -p ICDB

make clean
time bash ./src/main/scripts/export-db.sh $1
#time bash $dir/mysql-convert.sh $1
#time bash $dir/mysql-create.sh $1
#time bash $dir/mysql-load.sh $1
#time bash $dir/mysql-verify.sh

echo "Conversion complete."