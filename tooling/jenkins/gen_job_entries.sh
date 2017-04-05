#!/bin/sh

# Usage: tooling/jenkins/gen_job_entries.sh DIRECTORY

dir=$1

# generate the groovy snippet for the create_jobs script
for pom in $(find $dir -name pom.xml); do
    if [ $pom = './pom.xml' ] ; then
        continue
    fi
    # remove leading './'
    pom=${pom#./}
    pom=${pom%/pom.xml}
    cat <<EOM
    [
        location: '$pom'
    ],
EOM
done

# generate the negated module list for the sling-trunk job(s)
echo
for pom in $(find $dir -name pom.xml); do
    if [ $pom = './pom.xml' ] ; then
        continue
    fi
    # remove leading './'
    pom=${pom#./}
    pom=${pom%/pom.xml}

    echo -n ",!$pom"
done
echo
