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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.osgi.installer.InstallableData;
import org.apache.sling.osgi.installer.JcrInstallException;
import org.apache.sling.osgi.installer.OsgiController;
import org.apache.sling.osgi.installer.OsgiControllerServices;
import org.apache.sling.osgi.installer.ResourceOverrideRules;
import org.apache.sling.osgi.installer.impl.tasks.BundleInstallRemoveTask;
import org.apache.sling.osgi.installer.impl.tasks.BundleStartTask;
import org.apache.sling.osgi.installer.impl.tasks.ConfigInstallRemoveTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * OsgiController service
 *
 */
public class OsgiControllerImpl
    implements OsgiController,
               OsgiControllerServices,
               OsgiControllerTaskContext {

	private final BundleContext bundleContext;
    private final Storage storage;
    private ResourceOverrideRules roRules;
    private final Set<OsgiControllerTask> tasks = new TreeSet<OsgiControllerTask>();
    private final Set<OsgiControllerTask> tasksForNextCycle = new TreeSet<OsgiControllerTask>();
    private final Set<OsgiControllerTask> tasksForThisCycle = new TreeSet<OsgiControllerTask>();
    private final ServiceTracker logServiceTracker;
    private int threadCounter;

    public static final String BUNDLE_EXTENSION = ".jar";
    public static final String STORAGE_FILENAME = "controller.storage";

    /** Storage key: digest of an InstallableData */
    public static final String KEY_DIGEST = "data.digest";

    // TODO unused?
    private final PackageAdmin packageAdmin;

    /** Default value for getLastModified() */
    public static final long LAST_MODIFIED_NOT_FOUND = -1;

    public OsgiControllerImpl(final BundleContext bc,
                              final PackageAdmin pa,
                              final ServiceTracker logServiceTracker)
    throws IOException {
        this.bundleContext = bc;
        this.packageAdmin = pa;
        this.logServiceTracker = logServiceTracker;
        storage = new Storage(bc.getDataFile(STORAGE_FILENAME));
    }

    public void deactivate() {
        try {
            storage.saveToFile();
        } catch(IOException ioe) {
        	if (getLogService() != null) {
        		getLogService().log(LogService.LOG_WARNING, "IOException in Storage.saveToFile()", ioe);
        	}
        }

        if(getLogService() != null) {
            getLogService().log(LogService.LOG_WARNING,
                    OsgiController.class.getName()
                    + " service deactivated - this warning can be ignored if system is shutting down");
        }

    }

    public void scheduleInstallOrUpdate(String uri, InstallableData data) throws IOException, JcrInstallException {
    	synchronized (tasks) {
        	tasks.add(getTaskForUri(uri, data));
		}
    }

    public void scheduleUninstall(String uri) throws IOException, JcrInstallException {
    	synchronized (tasks) {
        	tasks.add(getTaskForUri(uri, null));
    	}
    }
    
    protected OsgiControllerTask getTaskForUri(String uri, InstallableData data) {
        if(uri.endsWith(BUNDLE_EXTENSION)) {
        	return new BundleInstallRemoveTask(uri, data, bundleContext, this);
        } else {
        	return new ConfigInstallRemoveTask(uri, data, this);
        }
    }

    public Set<String> getInstalledUris() {
        return storage.getKeys();
    }

    /** {@inheritDoc}
     *  @return null if uri not found
     */
    public String getDigest(String uri) {
        String result = null;

        if(storage.contains(uri)) {
            final Map<String, Object> uriData = storage.getMap(uri);
            result = (String)uriData.get(KEY_DIGEST);
        }
        return result;
    }

    public static String getResourceLocation(String uri) {
        return "jcrinstall://" + uri;
    }

    /** {@inheritDoc} */
    public void executeScheduledOperations() throws Exception {

        // Anything to do?
        if(tasks.isEmpty()) {
        	return;
        }

    	if(getLogService() != null) {
    		getLogService().log(LogService.LOG_INFO, "executeScheduledOperations() starts");
    	}
    	
        synchronized (tasks) {
        	// Add start bundle tasks for all active bundles
        	// so that they are restarted once we're done
        	for(Bundle b : bundleContext.getBundles()) {
        		if(b.getState() == Bundle.ACTIVE) {
        			final OsgiControllerTask t = new BundleStartTask(b.getBundleId()); 
        			tasks.add(t);
                	if(getLogService() != null) {
                        getLogService().log(LogService.LOG_DEBUG, "Added " + t); 
                	}
        		}
        	}

        	if(getLogService() != null) {
                getLogService().log(LogService.LOG_INFO, "Executing " + tasks.size() + " queued tasks");
        	}
            final long start = System.currentTimeMillis();
            
            // Now execute all our tasks in a separate thread
        	// and re-add tasks scheduled for next cycle
        	executeTasksInSeparateThread();
        	tasks.clear();
        	tasks.addAll(tasksForNextCycle);
        	tasksForNextCycle.clear();

        	if(getLogService() != null) {
                getLogService().log(LogService.LOG_INFO,
                		"executeScheduledOperations() ends (" + (System.currentTimeMillis() - start) + " msec)");
        	}
		}
	}
    
    /** Execute a single task - meant to be called from worker thread */
    private void executeTask(OsgiControllerTask t) {
    	final long start = System.currentTimeMillis();
    	final LogService log = getLogService();
    	if(log != null) {
    		log.log(LogService.LOG_DEBUG, "Executing task " + t);
    	}
    	
    	try {
    		t.execute(this);
    		if(log != null) {
    			final long delta = System.currentTimeMillis() - start;
    			log.log(LogService.LOG_INFO, "Successfully executed " + t + " in " + delta + " msec");
    		}
    	} catch(Throwable th) {
    		if(log != null) {
    			log.log(LogService.LOG_INFO, "Task execution failed (" + th + ") will retry " + t);
    		}
    		tasksForNextCycle.add(t);
    	}
    }
    
    void executeTasksInSeparateThread() throws InterruptedException {
		final Runnable r = new Runnable() {
			public void run() {
				while(!tasks.isEmpty()) {
					final List<OsgiControllerTask> toRemove = new LinkedList<OsgiControllerTask>();
					for(OsgiControllerTask t : tasks) {
						toRemove.add(t);
						executeTask(t);
						if(!tasksForThisCycle.isEmpty()) {
							break;
						}
					}
					tasks.removeAll(toRemove);
					tasks.addAll(tasksForThisCycle);
					tasksForThisCycle.clear();
				}
			}
		};
		
		final String threadName = getClass().getSimpleName() + " #" + (++threadCounter);
		final Thread t = new Thread(r, threadName);
		t.setDaemon(true);
		t.start();
		t.join();
    }

	public void setResourceOverrideRules(ResourceOverrideRules r) {
        roRules = r;
    }

	public ConfigurationAdmin getConfigurationAdmin() {
		// TODO ConfigurationAdmin should be bound/unbound rather than
		// looking it up every time, but that caused problems in the it/OsgiControllerTest
		if(bundleContext != null) {
		   	final ServiceReference ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
		    if(ref != null) {
		    	return (ConfigurationAdmin)bundleContext.getService(ref);
		    }
		}
		return null;
	}

	public ResourceOverrideRules getResourceOverrideRules() {
		return roRules;
	}

	public Storage getStorage() {
		return storage;
	}
	
	public LogService getLogService() {
		return (LogService)logServiceTracker.getService();
	}

	public void addTaskToCurrentCycle(OsgiControllerTask t) {
		if(getLogService() != null) {
			getLogService().log(LogService.LOG_DEBUG, "adding task to current cycle:" + t);
		}
		tasksForThisCycle.add(t);
	}

	public void addTaskToNextCycle(OsgiControllerTask t) {
		if(getLogService() != null) {
			getLogService().log(LogService.LOG_DEBUG, "adding task to next cycle:" + t);
		}
		tasksForNextCycle.add(t);
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}
	
	public PackageAdmin getPackageAdmin() {
		return packageAdmin;
	}
	
	public OsgiControllerServices getOsgiControllerServices() {
		return this;
	}

}