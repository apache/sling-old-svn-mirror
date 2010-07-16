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
package org.apache.sling.jcr.jackrabbit.server.impl;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jackrabbit.server.TestContentLoader;

/**
 * Component which loads the Jackrabbit test content into a Sling-managed JCR
 * repository. For now, this is a direct copy of code from jackrabbit-core's
 * JackrabbitRepositoryStub class. Once that class is refactored, we can remove
 * almost all of this code.
 *
 * @scr.component metatype="no"
 * @scr.service
 *
 * @scr.property name="service.description" value="Test Content Loader"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 *
 */
public class TestContentLoaderImpl implements TestContentLoader {

    /** @scr.reference */
    private SlingRepository repository;

    private String encoding = "UTF-8";

    public void loadTestContent() throws RepositoryException, IOException {

        Session session = repository.loginAdministrative(null);
        try {
            new org.apache.jackrabbit.core.TestContentLoader().loadTestContent(session);
        } finally {
            session.logout();
        }
    }

}
