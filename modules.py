#!/usr/bin/env python
# -*- coding: utf-8 -*-

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

# simple script to find projects (modules) and print their artifact ids
# could help when moving from Subversion to Git (https://issues.apache.org/jira/browse/SLING-3987)

import os
import sys
import xml.etree.ElementTree

def find_projects(root):
    artifactIds = []
    for dirpath, dirnames, filenames in os.walk(root):
        artifactId = find_project(dirpath, filenames)
        if artifactId:
            artifactIds.append(artifactId)
    artifactIds.sort()
    for artifactId in artifactIds:
       print '* {{' + artifactId + '}}'
       # wiki markup: print '* {{' + artifactId + '}}'
    print len(artifactIds), 'artifacts found'

def find_project(dirpath, filenames):
    if 'pom.xml' in filenames:
        if 'src' not in dirpath and 'target' not in dirpath: # TODO regex?
            file = dirpath + '/pom.xml'
            try:
                pom = xml.etree.ElementTree.parse(file)
                project = pom.getroot()
                artifactId = project.findall('./{http://maven.apache.org/POM/4.0.0}artifactId')[0].text
                artifactId = artifactId.strip()
                return artifactId
            except:
                print 'error parsing file:', file

def usage():
    print 'usage:', sys.argv[0], 'projects root, e.g. trunk or whiteboard'

if __name__ == '__main__':
    if len(sys.argv) == 2:
        root = sys.argv[1] # TODO: check path?
        find_projects(root)
    else:
        usage()
