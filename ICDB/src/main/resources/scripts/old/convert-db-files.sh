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

rm -f ./tmp/converted-db-files/schema/*.unl
rm -f ./tmp/converted-db-files/data/*.unl

if [ ! -f ${jar} ]; then
    make
fi

java -jar ${jar} convert-data -k ./src/main/resources/keyfile-sample
