#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "A database name must be specified as an argument"
    exit 1
fi

jar=./target/ICDB-0.5.0.jar
files=./tmp/db-files/data/*

# Convert all files to ICDB
echo "Converting Database '$1'"

mkdir -p ./tmp/converted-db-files/schema
mkdir -p ./tmp/converted-db-files/data

rm -f ./tmp/db-files/schema/*.unl
rm -f ./tmp/db-files/data/*.unl

if [ -ne ${jar} ]; then
    make
fi

java -jar ${jar} convert-data -f ./tmp/db-files/data

# Move converted files to a new directory
#mv $db/*_ICDB.unl $icdb
#mv $db/*.txt $icdb
