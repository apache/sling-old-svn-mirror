#!/bin/bash

TMP=`mktemp -d -t crank.XXX`

cp crank.d/* $TMP

for var in "$@"
do
  cp crank-$var.d/* $TMP
done

cat $TMP/*
rm -rf $TMP
