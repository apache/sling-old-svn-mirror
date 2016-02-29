#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.

# sed (BSD only) commands to change versions
# TODO make GNU compatible

# Oak
OAK_VERSION_CURRENT=1.3.15
OAK_VERSION_NEW=1.3.16

sed -i '' "s/<oak.version>$OAK_VERSION_CURRENT<\/oak.version>/<oak.version>$OAK_VERSION_NEW<\/oak.version>/1" "bundles/jcr/oak-server/pom.xml"
sed -i '' "s/<oak.version>$OAK_VERSION_CURRENT<\/oak.version>/<oak.version>$OAK_VERSION_NEW<\/oak.version>/1" "bundles/jcr/it-jackrabbit-oak/pom.xml"

sed -i '' "s/oak.version=$OAK_VERSION_CURRENT/oak.version=$OAK_VERSION_NEW/1" "./launchpad/builder/src/main/provisioning/oak.txt"
sed -i '' "s/<org.apache.jackrabbit.oak.version>$OAK_VERSION_CURRENT<\/org.apache.jackrabbit.oak.version>/<org.apache.jackrabbit.oak.version>$OAK_VERSION_NEW<\/org.apache.jackrabbit.oak.version>/1" "contrib/launchpad/karaf/org.apache.sling.launchpad.karaf-features/pom.xml"


# Oak Server
OAK_SERVER_VERSION_CURRENT=1.0.0
OAK_SERVER_VERSION_NEW=1.0.1-SNAPSHOT

sed -i '' "s/<sling.oak.server.version>$OAK_SERVER_VERSION_CURRENT<\/sling.oak.server.version>/<sling.oak.server.version>$OAK_SERVER_VERSION_NEW<\/sling.oak.server.version>/1" bundles/jcr/it-jackrabbit-oak/pom.xml

sed -i '' "s/org.apache.sling\/org.apache.sling.jcr.oak.server\/$OAK_SERVER_VERSION_CURRENT/org.apache.sling\/org.apache.sling.jcr.oak.server\/$OAK_SERVER_VERSION_NEW/1" "launchpad/builder/src/main/provisioning/oak.txt"
sed -i '' "s/<bundle>mvn:org.apache.sling\/org.apache.sling.jcr.oak.server\/$OAK_SERVER_VERSION_CURRENT<\/bundle>/<bundle>mvn:org.apache.sling\/org.apache.sling.jcr.oak.server\/$OAK_SERVER_VERSION_NEW<\/bundle>/1" "contrib/launchpad/karaf/org.apache.sling.launchpad.karaf-features/src/main/feature/feature.xml"


# Jackrabbit
JACKRABBIT_VERSION_CURRENT=2.12.0
JACKRABBIT_VERSION_NEW=2.12.1

sed -i '' "s/<jackrabbit.version>$JACKRABBIT_VERSION_CURRENT<\/jackrabbit.version>/<jackrabbit.version>$JACKRABBIT_VERSION_NEW<\/jackrabbit.version>/1" "bundles/jcr/it-jackrabbit-oak/pom.xml"
sed -i '' "s/<jackrabbit.version>$JACKRABBIT_VERSION_CURRENT<\/jackrabbit.version>/<jackrabbit.version>$JACKRABBIT_VERSION_NEW<\/jackrabbit.version>/1" "bundles/jcr/webdav/pom.xml"

sed -i '' "s/jackrabbit.version=$JACKRABBIT_VERSION_CURRENT/jackrabbit.version=$JACKRABBIT_VERSION_NEW/1" "launchpad/builder/src/main/provisioning/sling.txt"
sed -i '' "s/<org.apache.jackrabbit.version>$JACKRABBIT_VERSION_CURRENT<\/org.apache.jackrabbit.version>/<org.apache.jackrabbit.version>$JACKRABBIT_VERSION_NEW<\/org.apache.jackrabbit.version>/1" "contrib/launchpad/karaf/org.apache.sling.launchpad.karaf-features/pom.xml"

sed -i '' "s/<org.apache.jackrabbit.version>$JACKRABBIT_VERSION_CURRENT<\/org.apache.jackrabbit.version>/<org.apache.jackrabbit.version>$JACKRABBIT_VERSION_NEW<\/org.apache.jackrabbit.version>/1" "contrib/launchpad/karaf/org.apache.sling.launchpad.karaf-integration-tests/pom.xml"

sed -i '' "s/<jackrabbit.version>$JACKRABBIT_VERSION_CURRENT<\/jackrabbit.version>/<jackrabbit.version>$JACKRABBIT_VERSION_NEW<\/jackrabbit.version>/1" "bundles/commons/testing/pom.xml"

# Tika
TIKA_VERSION_CURRENT=1.10
TIKA_VERSION_NEW=1.11

sed -i '' "s/<tika.version>$TIKA_VERSION_CURRENT<\/tika.version>/<tika.version>$TIKA_VERSION_NEW<\/tika.version>/1" "bundles/jcr/it-jackrabbit-oak/pom.xml"

sed -i '' "s/org.apache.tika\/tika-core\/$TIKA_VERSION_CURRENT/org.apache.tika\/tika-core\/$TIKA_VERSION_NEW/1" "launchpad/builder/src/main/provisioning/sling.txt"
sed -i '' "s/org.apache.tika\/tika-bundle\/$TIKA_VERSION_CURRENT/org.apache.tika\/tika-bundle\/$TIKA_VERSION_NEW/1" "launchpad/builder/src/main/provisioning/sling.txt"
sed -i '' "s/<org.apache.tika.version>$TIKA_VERSION_CURRENT<\/org.apache.tika.version>/<org.apache.tika.version>$TIKA_VERSION_NEW<\/org.apache.tika.version>/1" "contrib/launchpad/karaf/org.apache.sling.launchpad.karaf-features/pom.xml"
