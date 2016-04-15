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
package org.apache.sling.jcr.api;

import aQute.bnd.annotation.ConsumerType;

/**
 * All active <code>SlingRepositoryInitializer</code> services are called before 
 * making the <code>SlingRepository</code> service available, and can perform
 * initializations on it, like creating service users, setting up initial access
 * control, migrating content in upgrades, etc.
 * 
 * The <code>SlingRepositoryInitializer</code> services need to be aware of any
 * repository clustering scenarios as well as multiple Sling instances accessing
 * the same repository. They might need to implement locking to avoid conflicts. 
 */
@ConsumerType
public interface SlingRepositoryInitializer {
    /** Process the content repository before it is 
     *  registered as an OSGi service.
     *  @param repo the repository to process
     *  @throws Exception If anything happens that should prevent
     *      the SlingRepository service from being registered.
     */
    public void processRepository(SlingRepository repo) throws Exception;
}
