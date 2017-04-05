#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

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

for jarfile in `find .  -name \*.jar -o -name \*.zip` ; do
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
