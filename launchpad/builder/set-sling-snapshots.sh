#!/bin/bash
#
# EXPERIMENTAL script to remove versions from the
# provisioning model lines of Sling artifacts.
#
# Meant to create an alternate version of the Sling
# launchpad using all the latest snapshots, for 
# integration testing in parallel with a stable
# versions that uses releases.
#
# This script modifies provisioning model files under
# src/main/provisioning, the changes are NOT
# meant to be committed, just meant to build and
# deploy an "all snapshots" launchpad jar meant to
# be used by the launchpad/testing module.
#
# To verify the results use
# mvn dependency:resolve | grep org.apache.sling
#
# For some reason currently two modules are not resolved
# to snapshots although their versions are correctly removed
# in the provisioning model:
#
# mvn dependency:resolve | grep org.apache.sling | grep -v SNAPSHOT
# [INFO] --- maven-dependency-plugin:2.10:resolve (default-cli) @ org.apache.sling.launchpad ---
# [INFO]    org.apache.sling:org.apache.sling.resourceresolver:jar:1.4.18:provided
# [INFO]    org.apache.sling:org.apache.sling.i18n:jar:2.5.4:provided
#

function removeSlingVersions() {
	sed 's/\(org.apache.sling\)\/\(org.apache.sling.*\)\/.*/org.apache.sling\/\2/g' < $1
}

SRC="src/main/provisioning/*.txt"
echo "Removing Sling versions from provisioning model files under $SRC..."
for i in $SRC
do
	TMP=/tmp/$ME_$$
	removeSlingVersions $i > $TMP
	mv $TMP $i
done