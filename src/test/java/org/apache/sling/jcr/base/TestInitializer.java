/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.base;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.api.SlingRepositoryInitializer;

/** SlingRepositoryInitializer used to test that all initializers are 
 *  called in the right order.
 */
class TestInitializer implements SlingRepositoryInitializer {

    private final String id;
    private static final String NODE_NAME = TestInitializer.class.getName();
    private static final String NODE_PATH = "/" + NODE_NAME;
    private static final String PROP = "value";
    
    TestInitializer(String id) {
        this.id = id;
    }
    
    @Override
    public void processRepository(SlingRepository repo) throws Exception {
        if(id.equals("EXCEPTION")) {
            throw new Exception("Failing due to id=" + id);
        }
        if(id.equals("ERROR")) {
            throw new Error("Erroring due to id=" + id);
        }
        
        final Session s = repo.loginAdministrative(null);
        try {
            Node n = null;
            if(s.nodeExists(NODE_PATH)) {
                n = s.getNode(NODE_PATH);
            } else {
                n = s.getRootNode().addNode(NODE_NAME);
            }
            final String value = n.hasProperty(PROP) ? n.getProperty(PROP).getString() : "";
            n.setProperty(PROP, value + id + ",");
            n.getSession().save();
        } finally {
            s.logout();
        }
    }
    
    static String getPropertyPath() {
        return NODE_PATH + "/" + PROP;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + id;
    }
    
    public String getId() {
        return id;
    }
}
