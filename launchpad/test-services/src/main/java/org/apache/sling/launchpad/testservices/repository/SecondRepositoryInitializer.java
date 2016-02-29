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


public class SecondRepositoryInitializer {}

/**
 * SlingRepositoryInitializer that adds a property to the node created by 
 * FirstRepositoryInitializer.
 */
/** TODO reactivate once jcr.base 2.3.2 is released

@Component
@Service(SlingRepositoryInitializer.class)
@Properties({
    // Execute this after FirstRepositoryInitializer
    @Property(name=Constants.SERVICE_RANKING, intValue=200)
})
public class SecondRepositoryInitializer implements SlingRepositoryInitializer {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String SIGNAL_PROPERTY_NAME = SecondRepositoryInitializer.class.getSimpleName();
    
    @Override
    public void processRepository(SlingRepository repo) throws Exception {
        final Session s = repo.loginAdministrative(null);
        try {
            final String path = FirstRepositoryInitializer.SIGNAL_NODE_PATH;
            if(!s.itemExists(path)) {
                log.warn("{} not found, should have been created by another initializer", path);
            } else {
                s.getNode(path).setProperty(SIGNAL_PROPERTY_NAME, System.currentTimeMillis());
                log.info("Property {} added to {}", SIGNAL_PROPERTY_NAME, path);
                s.save();
            }
        } finally {
            s.logout();
        }
    }
}
*/