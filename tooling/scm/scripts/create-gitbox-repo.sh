#!/bin/sh -e

# validation
if [ $# -ne 2 ]; then
    echo "Usage: $0 repo-name repo-description"
    exit 1
fi

repo_name=$1
repo_desc=$2

# will fail if wrong credentials are passed
status=$(curl --netrc -s -o /dev/null -I -w "%{http_code}" https://gitbox.apache.org/setup/newrepo.cgi?action=pmcs)
if [ $status != "200" ]; then
    echo "Got status ${status} for validation curl call, aborting."
    echo "Please check ~/.netrc for a login entry for gitbox.apache.org"
    exit 2
fi

echo "curl --netrc --data=\"action=create&pmc=sling&name=${repo_name}&description=${repo_desc}&notify=commits@sling.apache.org&ghnotify=dev@sling.apache.org&ispodling=false\" https://gitbox.apache.org/setup/newrepo.cgi"

