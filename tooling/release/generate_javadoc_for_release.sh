#!/bin/bash -e

VERSION=8
WORKDIR=out
ALLOW_SNAPSHOT=1

# create work directory
if [ ! -d $WORKDIR ] ; then
    mkdir -p $WORKDIR
fi

# get bundle list
if [ -f $WORKDIR/slingfeature.txt ] ; then
    echo "slingfeature.txt already present, not downloading";
else
    echo "Downloading bundle list for Sling $VERSION"
    wget https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.launchpad/$VERSION/org.apache.sling.launchpad-$VERSION-slingfeature.txt -O $WORKDIR/slingfeature.txt
fi

# checkout tags
artifacts=$(awk -F '/' '/org.apache.sling\// { print $2"-"$3 }' < $WORKDIR/slingfeature.txt)

for artifact in $artifacts; do
    if [ -d $WORKDIR/$artifact ] ; then
        echo "Not checking out $artifact, already present";
    else
        if [[ "$artifact" == *-SNAPSHOT ]]; then
            if [ $ALLOW_SNAPSHOT == 0 ] ; then
                echo "Failing build due to SNAPSHOT artifact $artifact";
                exit 1;
            else
                continue
            fi
        fi
        echo "Exporting $artifact from source control"
        svn export https://svn.apache.org/repos/asf/sling/tags/$artifact $WORKDIR/$artifact
        if [ -f patches/$artifact ]; then
            echo "Applying patch"
            pushd $WORKDIR/$artifact
            patch -p0 < ../../patches/$artifact
            popd
        fi
    fi
done

# generate dummy pom.xml

echo "Generating pom.xml"

POM=$WORKDIR/pom.xml
echo "<project>" > $POM
echo "  <modelVersion>4.0.0</modelVersion>" >> $POM
echo "  <groupId>org.apache.sling</groupId>" >> $POM
echo "  <artifactId>org.apache.sling.javadoc-builder</artifactId>" >> $POM
echo "  <packaging>pom</packaging>" >> $POM
echo "  <version>$VERSION</version>" >> $POM
echo >> $POM
echo "  <parent>" >> $POM
echo "    <groupId>org.apache</groupId>" >> $POM
echo "    <artifactId>apache</artifactId>" >> $POM
echo "    <version>8</version>" >> $POM
echo "  </parent>" >> $POM
echo >> $POM
echo "  <name>Apache Sling</name>" >> $POM
echo >> $POM
echo "  <properties>" >> $POM
echo "    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>" >> $POM
echo "  </properties>" >> $POM
echo >> $POM
echo " <modules> " >> $POM

for artifact in $artifacts; do
    if [[ "$artifact" == *-SNAPSHOT ]]; then
        continue
    fi
    echo "    <module>$artifact</module>" >> $POM
done

echo "  </modules>" >> $POM
echo "</project>" >> $POM

if [ ! -f $WORKDIR/src/main/javadoc/overview.html ] ; then
    echo "Downloading javadoc overview file"
    mkdir -p $WORKDIR/src/main/javadoc
    wget https://svn.apache.org/repos/asf/sling/trunk/src/main/javadoc/overview.html -O $WORKDIR/src/main/javadoc/overview.html
fi

# generate javadoc

echo "Starting javadoc generation"

pushd $WORKDIR
mvn -DexcludePackageNames="*.impl:*.internal:*.jsp:sun.misc:*.juli:*.testservices:*.integrationtest:*.maven:javax.*:org.osgi.*" \
         org.apache.maven.plugins:maven-javadoc-plugin:2.10.3:aggregate
popd

echo "Generated Javadocs can be found in $WORKDIR/target/site/apidocs/"
