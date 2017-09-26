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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.osgi.framework.Bundle;

/** Minimal AbstractSlingRepositoryManager used for testing */ 
class MockSlingRepository2 extends AbstractSlingRepository2 {

    MockSlingRepository2(MockSlingRepositoryManager manager, Bundle usingBundle, Session session) {
        super(manager, usingBundle);
    }
    
    @Override
    protected Session createAdministrativeSession(String workspace) throws RepositoryException {
        // Assuming we run on a test repo with no access control
        return getRepository().login();
    }

    @Override
    protected Session createServiceSession(Iterable<String> principalNames, String workspace) throws RepositoryException {
        return getRepository().login();
    }
}