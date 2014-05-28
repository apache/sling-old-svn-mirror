#!/bin/sh -e

SITE_LOCATION='public_html/sling/ide/preview'

# 1. Clean the current update site
ssh people.apache.org "rm -rf $SITE_LOCATION/*"

# 2. Upload the new update site
scp p2update/target/*.zip people.apache.org:$SITE_LOCATION

# 3. Unzip the new update site
ssh people.apache.org "cd $SITE_LOCATION && unzip -o *.zip && rm -f *.zip"
