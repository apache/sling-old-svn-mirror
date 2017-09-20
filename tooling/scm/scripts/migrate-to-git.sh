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

# This script is used to migrate modules from SVN to Git
#!/bin/sh -e

# prefixes to strip from module paths. trailing slash is mandatory
prefixes='bundles/extensions/ bundles/ contrib/bundles contrib/extensions/ contrib/ karaf/ tooling/maven/'
git_repo_location='../sling-modules'
git_src_location='../sling-modules-src'

if [ ! -f check_staged_release.sh ]; then
    echo "Please run this script from the root of the Sling SVN repository"
    exit 1
fi

if [ ! -d ${git_src_location} ]; then
    # generate a git-svn checkout
    echo "Creating source git-svn checkout ..."
    git clone https://github.com/apache/sling.git ${git_src_location}
    #  ensure we don't accidentally overwrite the source repository
    chmod ugo-w ${git_src_location}
    echo "Done!"
fi

for module in $(./tooling/scm/scripts/gen-repo-candidates.sh); do

    module_orig=$module

    for prefix in $prefixes; do
        module=${module#${prefix}}
    done

    artifactId=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='artifactId']/text()" ${module_orig}/pom.xml)

    repo_name="sling-${artifactId}" # add TLP prefix
    
    echo "---- Preparing to migrate $module_orig to $repo_name ---"

    status=$(curl -s -o /dev/null -I  -w "%{http_code}" https://git-wip-us.apache.org/repos/asf?p=${repo_name})

    if [ $status = "404" ]; then
        echo "Repository not found, will create";
    elif [ $status = "200" ] ;then
        echo "Repository exists, skipping";
    else
        echo "Unhandled HTTP status code ${status}, aborting"
        exit 1
    fi
    
    if [ ! -d ${git_repo_location}/${repo_name}/.git ]; then
        echo "Converting from SVN to Git..."

        # create the initial repo
        git clone --no-hardlinks ${git_src_location} ${git_repo_location}/${repo_name}
        pushd ${git_repo_location}/${repo_name}

        # make sure we don't push to the incorrect repo and also remove make sure
        # we don't keep references to the remote repo
        git remote rm origin

        # rename trunk to master
        git branch -m trunk master

        # Remove everything except the path belonging to the module
        git filter-branch --subdirectory-filter ${module_orig}

        # remove unrelated tags
        for tag in $(git tag); do
            if [[ $tag != ${artifactId}* ]]; then
                git tag -d ${tag}
            fi
        done

        # cleanup and compaction
        git for-each-ref --format="%(refname)" refs/original/ | xargs -n1 git update-ref -d
        git reflog expire --expire=now --all
        git repack -Ad
        git gc --aggressive --prune=now
        popd
        echo "Complete!"
    else
        echo "Already converted"
    fi

    
    # TODO - create the repository using the ASF self-service tool
    echo "Creating GIT repository ..."
    exit 254 # unimplemented

    cd ${git_repo_location}/${repo_name}
    git remote add origin https://git-wip-us.apache.org/repos/asf/${repo_name}.git
    git push -u origin master
done
