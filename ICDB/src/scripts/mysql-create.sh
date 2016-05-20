#!/bin/bash

script=scripts/create-icdb-aes.sql
DBNAME=${1^^}_ICDB
dir=AES


# Run converted database script

echo "Creating Database '$DBNAME'"
mysql -e "USE $1; source $script;"
