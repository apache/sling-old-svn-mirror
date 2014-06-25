#!/bin/sh -e

#    Licensed to the Apache Software Foundation (ASF) under one or
#    more contributor license agreements. See the NOTICE file
#    distributed with this work for additional information regarding
#    copyright ownership. The ASF licenses this file to you under the
#    Apache License, Version 2.0 (the "License"); you may not use
#    this file except in compliance with the License. You may obtain
#    a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0 Unless required by
#    applicable law or agreed to in writing, software distributed
#    under the License is distributed on an "AS IS" BASIS, WITHOUT
#    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions
#    and limitations under the License.

SITE_LOCATION='public_html/sling/ide/preview'

# 1. Clean the current update site
ssh people.apache.org "rm -rf $SITE_LOCATION/*"

# 2. Upload the new update site
scp p2update/target/*.zip people.apache.org:$SITE_LOCATION

# 3. Unzip the new update site
ssh people.apache.org "cd $SITE_LOCATION && unzip -o *.zip && rm -f *.zip"
