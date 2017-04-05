#!/bin/bash

if [ $# -ne 0 ]; then
    echo "Usage: DIST_CHECKOUT=... SITE_CHECKOUT=... $0";
    echo ""
    echo "The DIST_CHECKOUT and SITE_CHECKOUT can also pe placed ~/.config/sling/checkouts.conf"
    exit -1;
fi

if [ -f ~/.config/sling/checkouts.conf ]; then
    . ~/.config/sling/checkouts.conf
fi

if [ -z $DIST_CHECKOUT ]; then
    echo "DIST_CHECKOUT not set";
    exit 1;
fi

if [ ! -d $DIST_CHECKOUT ]; then
    echo "DIST_CHECKOUT ( $DIST_CHECKOUT ) is not a directory";
    exit 1;
fi

if [ -z $SITE_CHECKOUT ]; then
    echo "SITE_CHECKOUT not set";
    exit 1;
fi

if [ ! -d $SITE_CHECKOUT ]; then
    echo "SITE_CHECKOUT ( $SITE_CHECKOUT ) is not a directory";
    exit 1;
fi

OBR_FILE=$SITE_CHECKOUT/trunk/content/obr/sling.xml

if [ ! -f $OBR_FILE ]; then
    echo "Did not find OBR repository file at ${OBR_FILE}";
    exit 1;
fi

echo "Looking for bundles in $DIST_CHECKOUT";

for POM_FILE in $(ls -1 $DIST_CHECKOUT/*.pom); do
    # extract PACKAGING from pom ; not perfect
    POM_FILE=$(basename $POM_FILE)
    PACKAGING=$(cat $DIST_CHECKOUT/$POM_FILE | sed -rn 's/<(\/)?packaging>//pg' | tr -d ' \n');
    # default to jar if not present
    if [ "x$PACKAGING" = "x" ] ; then
        PACKAGING=jar
    fi

    # OBR only makes sense for bundles
    if [ $PACKAGING != "bundle" ]; then
        continue;
    fi

    # get artifact base name, without extension
    BASENAME=${POM_FILE%.pom}
    JAR_FILE="${BASENAME}.jar"

    # find dash which precedes the version
    #VERSION_IDX=$(expr index $BASENAME '-[123456789]')
    # extract version as 'the first dash followed by a digit'
    VERSION=$(echo $BASENAME | grep -Eo "\-[0-9].*")
    # clean version to remove the dash
    VERSION=${VERSION:1}
    
    VERSION_IDX=$(expr ${#BASENAME} - ${#VERSION})
    ARTIFACT_ID="${BASENAME:0:($VERSION_IDX - 1)}"

    # check for artifact presence in sling.xml
    OBR_KEY="$ARTIFACT_ID/$VERSION"
    ALREADY_EXISTS=$(grep -c $OBR_KEY $OBR_FILE)
    if [ $ALREADY_EXISTS != 0 ]; then
        # no need to process files already included
        continue
    fi

    echo "Adding $OBR_KEY to $OBR_FILE"
    mvn -q org.apache.felix:maven-bundle-plugin:deploy-file -Dfile=$DIST_CHECKOUT/$JAR_FILE -DpomFile=$DIST_CHECKOUT/$POM_FILE -DbundleUrl=http://repo1.maven.org/maven2/org/apache/sling/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION.jar -Durl=file://$SITE_CHECKOUT/trunk/content/obr -DprefixUrl=http://repo1.maven.org/maven2 -DremoteOBR=sling.xml
done
