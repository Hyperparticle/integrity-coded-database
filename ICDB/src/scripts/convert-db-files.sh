#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "A database name must be specified as an argument"
    exit 1
fi

jar=./target/ICDB-0.5.0.jar
files=./tmp/db-files/data/*

# Convert all files to ICDB
echo "Converting Database '$1'"

make

java -jar ${jar} -t < ./tmp/db-files/data/actor.unl

# Move converted files to a new directory
#mv $db/*_ICDB.unl $icdb
#mv $db/*.txt $icdb
