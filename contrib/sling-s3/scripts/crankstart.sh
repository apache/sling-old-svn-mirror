#!/bin/sh

MVN_OPTS=""

if [ "$M2REPO" ] ; then
  MVN_OPTS="${MVN_OPTS} -Dorg.ops4j.pax.url.mvn.localRepository=${M2REPO}"
fi

REPO="https://repository.apache.org/content/repositories/snapshots@snapshots@id=apache-snapshots"
REPO="${REPO},http://repo1.maven.org/maven2/@id=central"
MVN_OPTS="${MVN_OPTS} -Dorg.ops4j.pax.url.mvn.repositories=${REPO}"

# If CRANKSTART_ENV is set, don't start anything.
# This lets alternate start mechanisms source all settings in this file then use an alternate exec method,
# like start-stop-daemon, which doesn't work well with shell script when additional args are needed.
if [ ! "${CRANKSTART_ENV}" ] ; then
       java ${MVN_OPTS} -jar contrib/crankstart.jar "$@"
fi
