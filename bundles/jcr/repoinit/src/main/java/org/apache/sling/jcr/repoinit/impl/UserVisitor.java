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

import javax.jcr.Session;

import org.apache.sling.repoinit.parser.operations.CreateServiceUser;
import org.apache.sling.repoinit.parser.operations.CreateUser;
import org.apache.sling.repoinit.parser.operations.DeleteServiceUser;
import org.apache.sling.repoinit.parser.operations.DeleteUser;

/** OperationVisitor which processes only operations related to
 *  service users and ACLs. Having several such specialized visitors
 *  makes it easy to control the execution order.
 */
class UserVisitor extends DoNothingVisitor {

    /** Create a visitor using the supplied JCR Session.
     * @param s must have sufficient rights to create users
     *      and set ACLs.
     */
    public UserVisitor(Session s) {
        super(s);
    }

    @Override
    public void visitCreateServiceUser(CreateServiceUser s) {
        final String id = s.getUsername();
        try {
            if(!UserUtil.serviceUserExists(session, id)) {
                log.info("Creating service user {}", id);
                UserUtil.createServiceUser(session, id);
            } else {
                log.info("Service user {} already exists, no changes made", id);
            }
        } catch(Exception e) {
            report(e, "Unable to create service user [" + id + "]:" + e);
        }
    }

    @Override
    public void visitDeleteServiceUser(DeleteServiceUser s) {
        final String id = s.getUsername();
        log.info("Deleting service user {}", id);
        try {
            UserUtil.deleteServiceUser(session, id);
        } catch(Exception e) {
            report(e, "Unable to delete service user [" + id + "]:" + e);
        }
    }

    @Override
    public void visitCreateUser(CreateUser u) {
        final String id = u.getUsername();
        try {
            if(!UserUtil.serviceExists(session, id)) {
                final String pwd = u.getPassword();
                if(pwd != null) {
                    // TODO we might revise this warning once we're able
                    // to create users by providing their encoded password
                    // using u.getPasswordEncoding - for now I think only cleartext works
                    log.warn("Creating user {} with cleartext password - should NOT be used on production systems", id);
                } else {
                    log.info("Creating user {}", id);
                }
                UserUtil.createUser(session, id, pwd);
            } else {
                log.info("User {} already exists, no changes made", id);
            }
        } catch(Exception e) {
            report(e, "Unable to create user [" + id + "]:" + e);
        }
    }

    @Override
    public void visitDeleteUser(DeleteUser u) {
        final String id = u.getUsername();
        log.info("Deleting user {}", id);
        try {
            UserUtil.deleteUser(session, id);
        } catch(Exception e) {
            report(e, "Unable to delete user [" + id + "]:" + e);
        }
    }

}
