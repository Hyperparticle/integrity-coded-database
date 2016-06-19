#!/bin/bash

dir=~/aes_icdb
icdb=ICDB
icdb_ex=_ICDB

echo "Loading files into '$1'"

for f in $icdb/*.unl
do
    fname=${f%_ICDB.*}
    tablename=${fname#*/}
    
    echo "Loading $tablename"

    mysql -e "USE ${1^^}$icdb_ex;
    SET FOREIGN_KEY_CHECKS = 0;

    LOAD DATA INFILE '$dir/$f'
    REPLACE INTO TABLE $tablename
    FIELDS TERMINATED BY '|'
    LINES TERMINATED BY '\n';

    SET FOREIGN_KEY_CHECKS = 1;"
done
