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
package org.apache.sling.osgi.installer.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.apache.sling.osgi.installer.impl.tasks.BundleInstallTask;
import org.osgi.service.log.LogService;

/** Worker thread where all OSGi tasks are executed.
 *  Runs cycles where the list of RegisteredResources is examined,
 *  OsgiTasks are created accordingly and executed.
 *  
 *  A separate list of RegisteredResources is kept for resources
 *  that are updated or removed during a cycle, and merged with
 *  the main list at the end of the cycle.
 */
class OsgiInstallerThread extends Thread {
    
    private final OsgiInstallerContext ctx;
    private final List<RegisteredResource> newResources = new LinkedList<RegisteredResource>();
    private final List<RegisteredResource> registeredResources = new LinkedList<RegisteredResource>();
    private final TreeSet<OsgiInstallerTask> tasks = new TreeSet<OsgiInstallerTask>();
    
    OsgiInstallerThread(OsgiInstallerContext ctx) {
        setName(getClass().getSimpleName());
        this.ctx = ctx;
    }

    @Override
    public void run() {
        while(true) {
            // TODO do nothing if nothing to process!
            try {
                mergeNewResources();
                computeListOfTasks();
                executeTasks();
                Thread.sleep(250);
            } catch(Exception e) {
                if(ctx.getLogService() != null) {
                    ctx.getLogService().log(LogService.LOG_WARNING, e.toString(), e);
                }
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ignored) {
                }
            }
        }
    }
    
    private void mergeNewResources() {
        synchronized (newResources) {
            registeredResources.addAll(newResources);
            newResources.clear();
        }
    }
    
    private void computeListOfTasks() {
        for(RegisteredResource r : registeredResources) {
            if(r.getResourceType() == RegisteredResource.ResourceType.BUNDLE) {
                tasks.add(new BundleInstallTask(r));
            }
        }
    }
    
    void addTaskToCurrentCycle(OsgiInstallerTask t) {
        if(ctx.getLogService() != null) {
            ctx.getLogService().log(LogService.LOG_DEBUG, "adding task to current cycle:" + t);
        }
        synchronized (tasks) {
            tasks.add(t);
        }
    }
    
    private void executeTasks() throws Exception {
        while(!tasks.isEmpty()) {
            OsgiInstallerTask t = null;
            synchronized (tasks) {
                t = tasks.first();
            }
            t.execute(ctx);
            synchronized (tasks) {
                tasks.remove(t);
            }
        }
    }
    
    void addResource(RegisteredResource r) {
        synchronized (newResources) {
            newResources.add(r);
        }
    }
}
