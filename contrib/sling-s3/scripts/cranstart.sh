#!/bin/sh

REPO="https://repository.apache.org/content/repositories/snapshots@snapshots@id=apache-snapshots"
REPO="${REPO},http://repo1.maven.org/maven2/@id=central"

java -Dorg.ops4j.pax.url.mvn.repositories=$REPO \
     -jar contrib/crankstart.jar "$@"
