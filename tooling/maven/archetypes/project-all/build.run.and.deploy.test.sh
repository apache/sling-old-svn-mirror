#!/usr/bin/env bash

dir=`pwd`
echo "Build the Project-All Archetype in folder: $dir"
mvn clean install

testFolder=$dir/target/test-classes/projects/basic/project/sample-test-all

echo
echo
echo "------------------------------------------"
echo "         Build and Deploy the Test Project"
echo "------------------------------------------"
echo
echo
cd $testFolder
mvn clean install -P autoInstallAll
echo
echo
echo "------------------------------------------"
echo "         Done"
echo "------------------------------------------"
echo
echo

cd $dir
echo "Current Folder `pwd`"
