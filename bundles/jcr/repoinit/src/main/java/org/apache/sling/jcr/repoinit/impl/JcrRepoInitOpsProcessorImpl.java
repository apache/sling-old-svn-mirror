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

import java.util.List;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.apache.sling.repoinit.parser.operations.OperationVisitor;

/** Apply Operations produced by the repoinit parser to a JCR Repository */
@Component
@Service(JcrRepoInitOpsProcessor.class)
public class JcrRepoInitOpsProcessorImpl implements JcrRepoInitOpsProcessor {
    
    /** Apply the supplied operations: first the namespaces and nodetypes
     *  registrations, then the service users, paths and ACLs.
     */
    public void apply(Session session, List<Operation> ops) {
        
        final OperationVisitor [] visitors = {
                new NamespacesVisitor(session),
                new NodetypesVisitor(session),
                new ServiceAndAclVisitor(session)
        };
        
        for(OperationVisitor v : visitors) {
            for(Operation op : ops) {
                op.accept(v);
            }
        }
    }
}
