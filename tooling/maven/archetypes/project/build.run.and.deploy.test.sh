#!/usr/bin/env bash

dir=`pwd`
echo "Build the Project (UI) Archetype in folder: $dir"
mvn clean install
#echo "Run Integration Test"
#mvn integration-test

testFolder=$dir/target/test-classes/projects/basic/project/sample-test-ui

echo
echo
echo "------------------------------------------"
echo "         Build and Deploy the Test Project"
echo "------------------------------------------"
echo
echo
cd $testFolder
mvn clean install -P autoInstallPackage
echo
echo
echo "------------------------------------------"
echo "         Done"
echo "------------------------------------------"
echo
echo

cd $dir
echo "Current Folder `pwd`"
