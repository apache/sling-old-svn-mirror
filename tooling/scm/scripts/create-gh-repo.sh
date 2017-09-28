#!/bin/sh -ex

username="$1"
access_token="$2"
repo_name="$3"

if [ $# -ne 3 ]; then
    echo "Usage: $0 github_username github_token new_repo_name"
    exit 1
fi

curl -u "${username}:${access_token}" -d "{
    \"name\": \"${repo_name}\",
    \"has_issues\": false,
    \"has_projects\": false,
    \"has_wiki\": false
}" -X POST https://api.github.com/orgs/not-sling/repos
