#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "A database name must be specified as an argument"
    exit 1
fi

dir=./src/scripts

echo "Generating ICDB from '$1'"

# Create directories if they don't exist
# They will keep temporary files generated

make clean
make

time bash ${dir}/export-db.sh $1
time bash ${dir}/convert-db-files.sh $1
#time bash ${dir}/mysql-create.sh $1
#time bash ${dir}/mysql-load.sh $1
#time bash ${dir}/mysql-verify.sh

echo "Conversion complete."