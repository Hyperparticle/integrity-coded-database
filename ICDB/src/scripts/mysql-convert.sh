#!/bin/bash

dir=~/aes_icdb
db=database
prog=AES
icdb=ICDB

# Convert all files to ICDB
echo "Converting Database '$1'"

cd $prog
javac DataConversion.java
java DataConversion $dir/$db $1
cd ..

# Move converted files to a new directory
mv $db/*_ICDB.unl $icdb
mv $db/*.txt $icdb
