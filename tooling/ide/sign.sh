#!/bin/bash

DIR=$1

if [ $# -ne 1 ]; then
    echo "Usage: $0 directory";
    exit 1;
fi

if [ ! -d $DIR ] ; then
    echo "$DIR is not a directory";
    exit 2;
fi

pushd $DIR

for jarfile in `find .  -name \*.jar -o -name \*.zip -exec basename {} \;` ; do
    if [ ! -f $jarfile.md5 ] ; then
        md5sum $jarfile | awk '{print $1}' > $jarfile.md5
    fi
    if [ ! -f $jarfile.sha1 ] ; then
        sha1sum $jarfile | awk '{print $1}' > $jarfile.sha1
    fi
    if [ ! -f $jarfile.asc ] ; then
        gpg --detach-sign -a $jarfile 
    fi
done

popd
