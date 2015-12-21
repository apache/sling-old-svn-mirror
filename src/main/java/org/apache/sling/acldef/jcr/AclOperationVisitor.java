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

import javax.jcr.Session;

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

    @Override
    public void visitSetAclPrincipal(SetAclPrincipals s) {
        log.warn("TODO - set ACL for Principals");
    }

    @Override
    public void visitSetAclPaths(SetAclPaths s) {
        log.warn("TODO - set ACL for Paths");
    }
}
