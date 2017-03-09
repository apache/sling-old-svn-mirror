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
        final String username = s.getUsername();
        try {
            if (!UserUtil.userExists(session, username)) {
                log.info("Creating service user {}", username);
                UserUtil.createServiceUser(session, username);
            } else if (UserUtil.isServiceUser(session, username)) {
                log.info("Service user {} already exists, no changes made.", username);
            } else {
                final String message = String.format("Existing user %s is not a service user.", username);
                throw new RuntimeException(message);
            }
        } catch(Exception e) {
            report(e, "Unable to create service user [" + username + "]:" + e);
        }
    }

    @Override
    public void visitDeleteServiceUser(DeleteServiceUser s) {
        final String username = s.getUsername();
        log.info("Deleting service user {}", username);
        try {
            UserUtil.deleteUser(session, username);
        } catch(Exception e) {
            report(e, "Unable to delete service user [" + username + "]:" + e);
        }
    }

    @Override
    public void visitCreateUser(CreateUser u) {
        final String username = u.getUsername();
        try {
            if(!UserUtil.userExists(session, username)) {
                final String pwd = u.getPassword();
                if(pwd != null) {
                    // TODO we might revise this warning once we're able
                    // to create users by providing their encoded password
                    // using u.getPasswordEncoding - for now I think only cleartext works
                    log.warn("Creating user {} with cleartext password - should NOT be used on production systems", username);
                } else {
                    log.info("Creating user {}", username);
                }
                UserUtil.createUser(session, username, pwd);
            } else {
                log.info("User {} already exists, no changes made", username);
            }
        } catch(Exception e) {
            report(e, "Unable to create user [" + username + "]:" + e);
        }
    }

    @Override
    public void visitDeleteUser(DeleteUser u) {
        final String username = u.getUsername();
        log.info("Deleting user {}", username);
        try {
            UserUtil.deleteUser(session, username);
        } catch(Exception e) {
            report(e, "Unable to delete user [" + username + "]:" + e);
        }
    }

}
