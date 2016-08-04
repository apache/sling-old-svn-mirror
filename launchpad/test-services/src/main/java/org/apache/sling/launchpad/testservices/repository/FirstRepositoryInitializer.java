/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.testservices.repository;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.api.SlingRepositoryInitializer;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SlingRepositoryInitializer that creates a node to which SecondRepositoryInitializer
 * can add a property, to verify that they are executed in order of their service ranking.
 */
@Component
@Service(SlingRepositoryInitializer.class)
@Properties({
    // Execute this before SecondRepositoryInitializer
    @Property(name=Constants.SERVICE_RANKING, intValue=100)
})
public class FirstRepositoryInitializer implements SlingRepositoryInitializer {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String SIGNAL_NODE_PATH = "/" + FirstRepositoryInitializer.class.getName();
    
    @Override
    public void processRepository(SlingRepository repo) throws Exception {
        final Session s = repo.loginAdministrative(null);
        try {
            if(s.itemExists(SIGNAL_NODE_PATH)) {
                log.warn("{} already exists, these tests expect to run on an empty repository", SIGNAL_NODE_PATH);
            } else {
                s.getRootNode().addNode(SIGNAL_NODE_PATH.substring(1));
                log.info("{} created", SIGNAL_NODE_PATH);
                s.save();
            }
        } finally {
            s.logout();
        }
    }
}
