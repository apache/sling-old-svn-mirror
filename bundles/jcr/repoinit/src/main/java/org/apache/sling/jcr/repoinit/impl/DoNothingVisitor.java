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

import org.apache.sling.repoinit.parser.operations.CreatePath;
import org.apache.sling.repoinit.parser.operations.CreateServiceUser;
import org.apache.sling.repoinit.parser.operations.DeleteServiceUser;
import org.apache.sling.repoinit.parser.operations.OperationVisitor;
import org.apache.sling.repoinit.parser.operations.RegisterNamespace;
import org.apache.sling.repoinit.parser.operations.RegisterNodetypes;
import org.apache.sling.repoinit.parser.operations.SetAclPaths;
import org.apache.sling.repoinit.parser.operations.SetAclPrincipals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for specialized OperationVisitors.
 */
class DoNothingVisitor implements OperationVisitor {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final Session session;
    
    /** Create a visitor using the supplied JCR Session.
     * @param s must have sufficient rights to create users
     *      and set ACLs.
     */
    protected DoNothingVisitor(Session s) {
        session = s;
    }
    
    protected void report(Exception e, String message) {
        throw new RuntimeException(message, e);
    }
    
    protected static String excerpt(String s, int maxLength) {
        if(s.length() < maxLength) {
            return s;
        } else {
            return s.substring(0, maxLength -1) + "...";
        }
    }
    
    @Override
    public void visitCreateServiceUser(CreateServiceUser s) {
    }

    @Override
    public void visitDeleteServiceUser(DeleteServiceUser s) {
    }

    @Override
    public void visitSetAclPrincipal(SetAclPrincipals s) {
     }

    @Override
    public void visitSetAclPaths(SetAclPaths s) {
    }

    @Override
    public void visitCreatePath(CreatePath cp) {
    }

    @Override
    public void visitRegisterNamespace(RegisterNamespace rn) {
    }

    @Override
    public void visitRegisterNodetypes(RegisterNodetypes rn) {
    }
}
