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
package org.apache.sling.ide.impl.vlt;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.osgi.service.event.EventAdmin;

/**
 * The <tt>VltRepositoryFactory</tt> instatiantes <tt>VltRepository</tt> instances
 *
 */
public class VltRepositoryFactory implements RepositoryFactory {

    private EventAdmin eventAdmin;

    private Map<String,VltRepository> repositoryMap = new HashMap<String,VltRepository>();
    
    @Override
    public Repository getRepository(RepositoryInfo repositoryInfo) throws RepositoryException {

        final String key = getKey(repositoryInfo);
        
        synchronized(repositoryMap) {
            VltRepository repo = repositoryMap.get(key);
            if (repo!=null && !repo.isMarkedStopped()) {
                return repo;
            }
            
            repo = new VltRepository(repositoryInfo, eventAdmin);
            repo.init();
            
            repositoryMap.put(key, repo);
            return repo;
        }
    }
    
    @Override
    public void stopRepository(RepositoryInfo repositoryInfo) {
        final String key = getKey(repositoryInfo);
        synchronized(repositoryMap) {
            VltRepository r = repositoryMap.get(key);
            // marking the repository as stopped allows us to keep using it
            // (eg for node type registry lookups) although the server is stopped
            //TODO we might come up with a proper online/offline handling here
            r.markStopped();
        }
    }

    private String getKey(RepositoryInfo repositoryInfo) {
        return repositoryInfo.getUsername()+":"+repositoryInfo.getPassword()+"@"+repositoryInfo.getUrl();
    }

    protected void bindEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    protected void unbindEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }
}
