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
package org.apache.sling.jcr.jackrabbit.server.impl;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.RepositoryManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractSlingRepository2;
import org.apache.sling.jcr.base.AbstractSlingRepositoryManager;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AdministrativeCredentials;
import org.osgi.framework.Bundle;

public class SlingServerRepository extends AbstractSlingRepository2 implements Repository, SlingRepository,
        RepositoryManager {

    final String adminUserName;

    protected SlingServerRepository(final AbstractSlingRepositoryManager manager, final Bundle usingBundle,
            final String adminUserName) {
        super(manager, usingBundle);
        this.adminUserName = adminUserName;
    }

    public void stop() {
        throw new UnsupportedOperationException("RepositoryManager.stop()");
    }

    public DataStoreGarbageCollector createDataStoreGarbageCollector() throws RepositoryException {
        final Repository base = this.getRepository();
        if (base instanceof RepositoryManager) {
            return ((RepositoryManager) base).createDataStoreGarbageCollector();
        }

        return null;
    }

    @Override
    protected Session createAdministrativeSession(String workspace) throws RepositoryException {
        final Credentials adminCredentials = new AdministrativeCredentials(this.adminUserName);
        return this.getRepository().login(adminCredentials, workspace);
    }
}
