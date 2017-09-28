#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# this script is used to generate a list of Git repo candidates for migration
# from SVN to Git

if [ ! -f check_staged_release.sh ]; then
    echo "Please run this script from the root of the Sling SVN repository"
    exit 1
fi

for pom in $(find . -name pom.xml  \
    | grep -v './tooling/ide' \
    | grep -v './performance' \
    | grep -v './samples' \
    | grep -v '/target/' \
    | grep -v '/archetype-resources/' \
    | grep -v '/src/test/resources/' \
    | sort); do

    # remove reactor poms
    grep -q modules $pom
    if [ $? -eq 0 ]; then
        continue;
    fi
    
    pom=${pom%/pom.xml} # remove trailing '/pom.xml'
    pom=${pom#./} # remove leading ./

    echo $pom
done

# by exception, these will be standalone modules
echo "tooling/ide"
echo "samples"
echo "performance"
