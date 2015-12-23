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
package org.apache.sling.acldef.jcr;

import static org.apache.sling.acldef.parser.operations.AclLine.PROP_PATHS;
import static org.apache.sling.acldef.parser.operations.AclLine.PROP_PRINCIPALS;
import static org.apache.sling.acldef.parser.operations.AclLine.PROP_PRIVILEGES;

import java.util.List;

import javax.jcr.Session;

import org.apache.sling.acldef.parser.operations.AclLine;
import org.apache.sling.acldef.parser.operations.CreateServiceUser;
import org.apache.sling.acldef.parser.operations.DeleteServiceUser;
import org.apache.sling.acldef.parser.operations.OperationVisitor;
import org.apache.sling.acldef.parser.operations.SetAclPaths;
import org.apache.sling.acldef.parser.operations.SetAclPrincipals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Processes the Operations produced by the ACL
 *  definitions parser to create the users and
 *  set the ACLs defined by the parser input.
 */
public class AclOperationVisitor implements OperationVisitor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private Session session;
    
    /** Create a visitor using the supplied JCR Session.
     * @param s must have sufficient rights to create users
     *      and set ACLs.
     */
    public AclOperationVisitor(Session s) {
        session = s;
    }
    
    private void report(Exception e, String message) {
        throw new RuntimeException(message, e);
    }
    
    @Override
    public void visitCreateServiceUser(CreateServiceUser s) {
        final String id = s.getUsername();
        log.info("Creating service user {}", id);
        try {
            ServiceUserUtil.createServiceUser(session, id);
        } catch(Exception e) {
            report(e, "Unable to create service user [" + id + "]:" + e);
        }
    }

    @Override
    public void visitDeleteServiceUser(DeleteServiceUser s) {
        final String id = s.getUsername();
        log.info("Deleting service user {}", id);
        try {
            ServiceUserUtil.deleteServiceUser(session, id);
        } catch(Exception e) {
            report(e, "Unable to delete service user [" + id + "]:" + e);
        }
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
            AclUtil.setAcl(s, principals, paths, privileges, isAllow);
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
}