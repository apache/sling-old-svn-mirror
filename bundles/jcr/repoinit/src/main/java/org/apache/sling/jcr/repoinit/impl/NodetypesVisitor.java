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

import java.io.StringReader;

import javax.jcr.Session;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.repoinit.parser.operations.RegisterNodetypes;

/** OperationVisitor which processes only operations related to
 *  namespaces and nodetypes. Having several such specialized visitors 
 *  makes it easy to control the execution order.
 */
class NodetypesVisitor extends DoNothingVisitor {

    /** Create a visitor using the supplied JCR Session.
     * @param s must have sufficient rights to create users
     *      and set ACLs.
     */
    public NodetypesVisitor(Session s) {
        super(s);
    }
    
    @Override
    public void visitRegisterNodetypes(RegisterNodetypes rn) {
        try {
            log.info("Registering nodetypes from {}", excerpt(rn.getCndStatements(), 100));
            CndImporter.registerNodeTypes(new StringReader(rn.getCndStatements()), session);
        } catch(Exception e) {
            report(e, "Unable to register nodetypes from " + rn);
        }
    }
}
