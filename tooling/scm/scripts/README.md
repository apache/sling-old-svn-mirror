SCM Tooling
====

Git migration helpers
---

The workflow for migrating from Subversion to Git is the following:

1. Generate the list of repository candidates

    $ ./tooling/scm/scripts/gen-repo-candidates.sh  > repo-candidates.txt

2. Create the remote repositories using the ASF self-service git tool

    $ ./tooling/scm/scripts/migrate-to-git.sh -r < repo-candidates.txt

Creating a repository can take up to one hour, so do this well in advance

3. Extract the modules in individual repositories

    $ ./tooling/scm/scripts/migrate-to-git.sh -r < repo-candidates.txt

Also validate that the repositories created using step 2 are now live.

4. Push the local changes to the remote repositories

    $ ./tooling/scm/scripts/migrate-to-git.sh -p < repo-candidates.txt
