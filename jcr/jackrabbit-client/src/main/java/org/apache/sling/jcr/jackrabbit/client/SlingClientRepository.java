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
package org.apache.sling.jcr.jackrabbit.client;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.sling.jcr.api.AbstractSlingRepository;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.RepositoryAccessor;
import org.osgi.service.log.LogService;

/**
 * The <code>RepositorySPIImpl</code> TODO
 *
 * @scr.component label="%repository.name" description="%repository.description"
 *          factory="org.apache.sling.jcr.client.SlingClientRepositoryFactory"
 *
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description"
 *      value="Factory for non-embedded JCR Repository Instances"
 *
 * @scr.service
 *
 * @scr.property value="default" name="defaultWorkspace"
 * @scr.property value="anonymous" name="anonymous.name"
 * @scr.property value="anonymous" name="anonymous.password"
 * @scr.property value="admin" name="admin.name"
 * @scr.property value="admin" name="admin.password"
 * @scr.property value="-1" type="Integer" name="pool.maxActive"
 * @scr.property value="10" type="Integer" name="pool.maxIdle"
 * @scr.property value="1" type="Integer" name="pool.maxActiveWait"
 *
 * @scr.property name="java.naming.factory.initial"
 *               value="org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory"
 * @scr.property name="java.naming.provider.url" value="http://incubator.apache.org/sling"
 */
public class SlingClientRepository extends AbstractSlingRepository
        implements Repository, SlingRepository {

    /**
     * @scr.property value="jackrabbit"
     */
    public static final String REPOSITORY_NAME = "name";

    /**
     * @scr.reference
     */
    private LogService log;

    private Repository delegatee;

    //---------- AbstractSlingRepository methods ------------------------------

    protected Repository getDelegatee() throws RepositoryException {
        if (this.delegatee == null) {
            this.delegatee = this.getRepository();
        }

        return this.delegatee;
    }

    protected LogService getLog() {
        return this.log;
    }

    //---------- SCR integration ----------------------------------------------

    // set logger
    protected void bindLogService(LogService log) {
        this.log = log;
    }

    // remove logger
    protected void unbindLogService(LogService log) {
        this.log = null;
    }

    //---------- Repository Publication ---------------------------------------

    private Repository getRepository() throws RepositoryException {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> environment = this.getComponentContext().getProperties();

        String repoName = (String) environment.get(REPOSITORY_NAME);
        if (repoName == null) {
            throw new RepositoryException("Missing property 'name'");
        }

        final Hashtable<String, Object> jndiContext = this.fromDictionary(environment);
        final Repository repo = new RepositoryAccessor().getRepository(repoName, jndiContext);
        if(repo == null) {
            throw new RepositoryException("Cannot acquire repository '" + repoName + "'");
        }
        return repo;
    }


    //---------- internal -----------------------------------------------------

    private Hashtable<String, Object> fromDictionary(Dictionary<String, Object> source) {
        Hashtable<String, Object> table = new Hashtable<String, Object>();
        for (Enumeration<String> ke=source.keys(); ke.hasMoreElements(); ) {
            String key = ke.nextElement();
            table.put(key, source.get(key));
        }
        return table;
    }
}
