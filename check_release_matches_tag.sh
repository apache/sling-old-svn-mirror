#!/bin/bash
# check that a Sling staged release matches the corresponding svn tags
#
# usage:
#  sh check_release_matches_tag.sh 004 /tmp/sling-staging
#
# Note that differences in line endings are not ignored by default.
# doing "EXPORT DIFFOPT=-b" before calling this ignores them.
BASE=$2/$1/org/apache/sling
TAGBASE=http://svn.apache.org/repos/asf/sling/tags/

function fail() {
	echo $* >&2
	exit 1
}

function check() {
       TAG=$TAGBASE/$1
       ZIP=$PWD/$2
       WORKDIR=workdir/$1/$(date +%s)
       CUR=$PWD
       echo
       echo "Checking $ZIP against $TAG"
       mkdir -p $WORKDIR
       cd $WORKDIR > /dev/null
       unzip $ZIP > /dev/null
       ZIPDIR=$PWD/$(ls)
       svn export $TAG svnexport > /dev/null
       cd svnexport > /dev/null
       diff $DIFFOPT -r . $ZIPDIR
       cd $CUR

}

CURDIR=`pwd`
cd $BASE || fail "Cannot cd to $BASE"

find . -name *.zip | cut -c 3- | sed 's/\// /g' | while read line
do
       set $line
       TAG=${1}-${2}
       ZIP=${1}/${2}/${3}
       check $TAG $ZIP
done
openssl sha1 $(find . -name *.zip)
cd $CURDIR
