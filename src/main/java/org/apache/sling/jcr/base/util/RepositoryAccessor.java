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
package org.apache.sling.jcr.base.util;

import java.util.Hashtable;

import javax.jcr.Repository;
import javax.naming.InitialContext;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.sling.api.SlingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Access a Repository via JNDI or RMI. */
 public class RepositoryAccessor {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /** Prefix for RMI Repository URLs */
    public static final String RMI_PREFIX = "rmi://";
    
    /** Prefix for JNDI Repository URLs */
    public static final String JNDI_PREFIX = "jndi://";
 
    public static class RepositoryUrlException extends SlingException {
        RepositoryUrlException(String reason) {
            super(reason);
        }
    }
    
    /** First try to access the Repository via JNDI (unless jndiContext is null), and if
     *  not successful try RMI.
     *  
     * @param repositoryName JNDI name or RMI URL (must start with "rmi://") of the Repository
     * @param jndiContext if null, JNDI is not tried
     * @return a Repository, or null if not found
     */
    public Repository getRepository(String repositoryName, Hashtable<String, Object> jndiContext) {
        
        Repository result = null;
        
        if(jndiContext == null || jndiContext.size() == 0) {
            log.info("jndiContext is null or empty, not trying JNDI");
        } else {
            log.debug("Trying to acquire Repository '" + repositoryName + "' via JNDI, context=" + jndiContext);
            final ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                InitialContext initialContext = new InitialContext(jndiContext);
                Object repoObject = initialContext.lookup(repositoryName);
                if (repoObject instanceof Repository) {
                    result = (Repository) repoObject;
                    log.info("Acquired Repository '" + repositoryName + "' via JNDI");

                } else if (repoObject instanceof RemoteRepository) {
                    RemoteRepository remoteRepo = (RemoteRepository) repoObject;
                    LocalAdapterFactory laf = new ClientAdapterFactory();
                    result = laf.getRepository(remoteRepo);
                    log.info("Acquired RemoteRepository '" + repositoryName + "' via JNDI");
                }
                
            } catch (Throwable t) {
                log.debug("Unable to acquire Repository '" + repositoryName + "' via JNDI, context=" + jndiContext, t);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }

        if(result == null) {
            if(repositoryName==null || !repositoryName.startsWith(RMI_PREFIX)) {
                log.info("Repository name does not start with '" + RMI_PREFIX + "', not trying RMI");
            } else {
                try {
                    log.debug("Trying to acquire Repository '" + repositoryName + "' via RMI");
                    ClientRepositoryFactory crf = new ClientRepositoryFactory();
                    result = crf.getRepository(repositoryName);
                    log.info("Acquired Repository '" + repositoryName + "' via RMI");
                } catch (Throwable t) {
                    log.debug("Unable to acquire Repository '" + repositoryName + "' via RMI", t);
                }
            }
        }
        
        if(result == null) {
            log.info("Unable to acquire Repository '" + repositoryName + "'");
        }
        
        return result;
    }
    
    /** Acquire a Repository from the given URL
     *  @param url for RMI, an RMI URL. For JNDI, "jndi://", followed by the JNDI repository name,
     *  followed by a colon and a comma-separated list of JNDI context values, for example:
     *  <pre>
     *      jndi://jackrabbit:java.naming.factory.initial=org.SomeClass,java.naming.provider.url=http://foo.com
     *  </pre>  
     */
    public Repository getRepositoryFromURL(String url) throws RepositoryUrlException {
        if(url == null) {
            throw new RepositoryUrlException("null URL");
        }
        
        if(url.startsWith(JNDI_PREFIX)) {
            // Parse JNDI URL to extract repository name and context
            String name = null;
            final Hashtable<String, Object> jndiContext = new Hashtable <String, Object> ();
            final String urlNoPrefix = url.substring(JNDI_PREFIX.length()); 
            final int colonPos = urlNoPrefix.indexOf(':');
            if(colonPos < 0) {
                name = urlNoPrefix;
            } else {
                name = urlNoPrefix.substring(0, colonPos);
                for(String entryStr : urlNoPrefix.substring(colonPos + 1).split(",")) {
                    final String [] entry = entryStr.split("=");
                    if(entry.length == 2) {
                        jndiContext.put(entry[0], entry[1]);
                    }
                }
            }
            
            return getRepository(name, jndiContext);
            
        } else {
            
            // Use URL as is
            return getRepository(url, null);
        }
    }
}
