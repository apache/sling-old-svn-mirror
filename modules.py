#!/usr/bin/env python
# -*- coding: utf-8 -*-

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
       print artifactId
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
