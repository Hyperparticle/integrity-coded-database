#!/bin/bash

dir=~/aes_icdb
prog=AES
icdb=ICDB

# Convert all files to ICDB
echo "Verifying Database"

cd $prog
javac DataFileVerification.java
java DataFileVerification $dir/$icdb
cd ..
