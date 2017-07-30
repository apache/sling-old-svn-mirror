/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.repoinit.impl;

import static org.apache.sling.repoinit.parser.operations.AclLine.PROP_PATHS;
import static org.apache.sling.repoinit.parser.operations.AclLine.PROP_PRINCIPALS;
import static org.apache.sling.repoinit.parser.operations.AclLine.PROP_PRIVILEGES;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.repoinit.parser.operations.AclLine;
import org.apache.sling.repoinit.parser.operations.CreatePath;
import org.apache.sling.repoinit.parser.operations.PathSegmentDefinition;
import org.apache.sling.repoinit.parser.operations.RestrictionClause;
import org.apache.sling.repoinit.parser.operations.SetAclPaths;
import org.apache.sling.repoinit.parser.operations.SetAclPrincipals;

/** OperationVisitor which processes only operations related to ACLs.
 * Having several such specialized visitors
 * makes it easy to control the execution order.
 */
class AclVisitor extends DoNothingVisitor {

    /** Create a visitor using the supplied JCR Session.
     * @param s must have sufficient rights to create users
     *      and set ACLs.
     */
    public AclVisitor(Session s) {
        super(s);
    }

    private List<String> require(AclLine line, String propertyName) {
        final List<String> result = line.getProperty(propertyName);
        if(result == null) {
            throw new IllegalStateException("Missing property " + propertyName + " on " + line);
        }
        return result;
    }

    private void setAcl(AclLine line, Session s, List<String> principals, List<String> paths, List<String> privileges, boolean isAllow) {
        try {
            log.info("Adding ACL '{}' entry '{}' for {} on {}", isAllow ? "allow" : "deny", privileges, principals, paths);
            List<RestrictionClause> restrictions = line.getRestrictions();
            AclUtil.setAcl(s, principals, paths, privileges, isAllow, restrictions);
        } catch(Exception e) {
            throw new RuntimeException("Failed to set ACL (" + e.toString() + ") " + line, e);
        }
    }

    @Override
    public void visitSetAclPrincipal(SetAclPrincipals s) {
        final List<String> principals = s.getPrincipals();
        for(AclLine line : s.getLines()) {
            final boolean isAllow = line.getAction().equals(AclLine.Action.ALLOW);
            setAcl(line, session, principals, require(line, PROP_PATHS), require(line, PROP_PRIVILEGES), isAllow);
        }
     }

    @Override
    public void visitSetAclPaths(SetAclPaths s) {
        final List<String> paths = s.getPaths();
        for(AclLine line : s.getLines()) {
            final boolean isAllow = line.getAction().equals(AclLine.Action.ALLOW);
            setAcl(line, session, require(line, PROP_PRINCIPALS), paths, require(line, PROP_PRIVILEGES), isAllow);
        }
    }

    @Override
    public void visitCreatePath(CreatePath cp) {
        String parentPath = "";
            for(PathSegmentDefinition psd : cp.getDefinitions()) {
                final String fullPath = parentPath + "/" + psd.getSegment();
                try {
                    if(session.itemExists(fullPath)) {
                        log.info("Path already exists, nothing to do (and not checking its primary type for now): {}", fullPath);
                    } else {
                        final Node n = parentPath.equals("") ? session.getRootNode() : session.getNode(parentPath);
                        log.info("Creating node {} with primary type {}", fullPath, psd.getPrimaryType());
                        n.addNode(psd.getSegment(), psd.getPrimaryType());
                    }
                } catch(Exception e) {
                    throw new RuntimeException("CreatePath execution failed at " + psd + ": " + e, e);
                }
                parentPath += "/" + psd.getSegment();
            }
        try {
            session.save();
        } catch(Exception e) {
            throw new RuntimeException("Session.save failed: "+ e, e);
        }
    }
}
