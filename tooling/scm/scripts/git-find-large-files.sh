#!/bin/sh

# script which finds large files in the git repository
# this lists all objects in the repo history, which means
# we can use it to detect when files that don't belong
# to the repo where incorrectly kept after extracting
# a module

git rev-list --objects --all | grep "$(git verify-pack -v .git/objects/pack/*.idx | sort -k 3 -n | tail -10 | awk '{print$1}')"
