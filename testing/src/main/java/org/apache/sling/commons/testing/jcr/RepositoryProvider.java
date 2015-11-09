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
package org.apache.sling.commons.testing.jcr;

import javax.jcr.RepositoryException;

import org.apache.sling.jcr.api.SlingRepository;

/** Provide a Repository to test classes, on-demand,
 *  with auto-cleanup at JVM shutdown.
 */
public class RepositoryProvider {
    private static RepositoryProvider INSTANCE;
    private SlingRepository repository;
    
    private static class ShutdownThread extends Thread {
        @Override
        public void run() {
            try {
                RepositoryUtil.stopRepository();
            } catch(Exception e) {
                System.out.println("Exception in ShutdownThread:" + e);
            }
        }
        
    };
    
    private RepositoryProvider() {
    }
    
    public synchronized static RepositoryProvider instance() {
        if(INSTANCE == null) {
            INSTANCE = new RepositoryProvider();
        }
        return INSTANCE;
    }
    
    /** Return a SlingRepository. First call initializes it, and a JVM
    *  shutdown hook is registered to stop it.
    **/
    public synchronized SlingRepository getRepository() throws RepositoryException {
        if(repository == null) {
            RepositoryUtil.startRepository();
            repository = RepositoryUtil.getRepository();
            Runtime.getRuntime().addShutdownHook(new ShutdownThread());
        }
        return repository;
    }
}