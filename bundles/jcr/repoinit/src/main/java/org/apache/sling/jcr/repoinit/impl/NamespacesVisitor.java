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

import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;

import org.apache.sling.repoinit.parser.operations.RegisterNamespace;

/** OperationVisitor which processes only operations related to
 *  namespaces and nodetypes. Having several such specialized visitors 
 *  makes it easy to control the execution order.
 */
class NamespacesVisitor extends DoNothingVisitor {

    /** Create a visitor using the supplied JCR Session.
     * @param s must have sufficient rights to create users
     *      and set ACLs.
     */
    public NamespacesVisitor(Session s) {
        super(s);
    }
    
    @Override
    public void visitRegisterNamespace(RegisterNamespace rn) {
        try {
            final NamespaceRegistry reg = session.getWorkspace().getNamespaceRegistry();
            log.info("Registering namespace from {}", rn);
            reg.registerNamespace(rn.getPrefix(), rn.getURI());
        } catch(Exception e) {
            report(e, "Unable to register namespace from " + rn);
        }
    }
}
