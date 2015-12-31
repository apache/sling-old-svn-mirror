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
package org.apache.sling.acldef.jcr.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.acldef.jcr.AclOperationVisitor;
import org.apache.sling.acldef.parser.AclDefinitionsParser;
import org.apache.sling.acldef.parser.operations.Operation;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Deactivate;

/** OSGi component that sets up service users and ACLS
 *  based on configurations created by the acldef provisioning
 *  model processor.
 *  
 *  As Oak requires a path to exist before setting an ACL on it,
 *  this component needs to retry setting ACLs when that fails
 *  due to a non-existing path.
 */
@Component(
        configurationFactory=true,
        metatype=false,
        configurationPid=AclSetup.CONFIG_PID,
        policy=ConfigurationPolicy.REQUIRE)
public class AclSetup implements Runnable {
    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String CONFIG_PID = "org.apache.sling.acldef.jcr.AclSetup";
    public static final String ACLDEF_PROP_PREFIX = "acldef.text.";
    public static final String THREAD_POOL_NAME = "ACL Definitions";
    
    private List<String> todo; 
    private ThreadPool threadPool;
    private boolean running;
    
    @Reference
    AclDefinitionsParser parser;
    
    @Reference
    ThreadPoolManager threadPoolManager;
    
    @Reference
    SlingRepository repository;
    
    @Activate
    public void activate(Map<String, Object> config) {
        todo  = new ArrayList<String>();
        threadPool = threadPoolManager.get(THREAD_POOL_NAME);
        
        for(String key : config.keySet()) {
            if(key.startsWith(ACLDEF_PROP_PREFIX)) {
                final String value = (String)config.get(key);
                todo.add(value);
            }
        }
        if(todo.isEmpty()) {
            log.error("No {} properties in configuration {}, nothing to do", ACLDEF_PROP_PREFIX, config);
        } else {
            log.info("Got {} ACL definitions to execute asynchronously", todo.size());
            running = true;
            threadPool.execute(this);
        }
    }
    
    @Deactivate
    public void deactivate(Map<String, Object> config) {
        synchronized (this) {
            running = false;
            threadPoolManager.release(threadPool);
        }
    }

    private void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch(InterruptedException ignore) {
        }
    }
    
    @Override
    public void run() {
        log.info("Applying {} ACL definition snippets", todo.size());

        List<String> newTodo = new ArrayList<String>();
        Session s = null;
        try {
            s = repository.loginAdministrative(null);
            final AclOperationVisitor visitor = new AclOperationVisitor(s);
            for(String acldef : todo) {
                try {
                    for(Operation op : parser.parse(new StringReader(acldef))) {
                        op.accept(visitor);
                        s.save();
                    }
                } catch(Exception e) {
                    log.warn("Exception while executing an ACL definition:" + e.toString(), e);
                    newTodo.add(acldef);
                }
            }
        } catch(Exception e) {
            log.warn("Exception while executing ACL definitions, will retry everything:" + e.toString(), e);
            newTodo = todo;
        } finally {
            if(s != null) {
                s.logout();
            }
        }
        
        // TODO schedule with exponential backoff?
        if(!newTodo.isEmpty() && running) {
            sleep(1000);
        }
        
        synchronized (this) {
            todo = newTodo;
            if(todo.isEmpty()) {
                log.info("All ACL definitions executed");
            } else if(running) {
                log.info("{} ACL definitions left to execute, will retry", todo.size());
                threadPool.execute(this);
            } else {
                log.info("Some operations failed but not running anymore");
            }
        }
    }
}