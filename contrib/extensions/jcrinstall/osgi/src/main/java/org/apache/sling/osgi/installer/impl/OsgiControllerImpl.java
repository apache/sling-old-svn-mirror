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

import org.apache.sling.osgi.installer.InstallableData;
import org.apache.sling.osgi.installer.JcrInstallException;
import org.apache.sling.osgi.installer.OsgiController;
import org.apache.sling.osgi.installer.OsgiControllerServices;
import org.apache.sling.osgi.installer.OsgiResourceProcessor;
import org.apache.sling.osgi.installer.ResourceOverrideRules;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/** OsgiController service
 *
 *  @scr.service
 *  @scr.component
 *      immediate="true"
 *      metatype="no"
 *  @scr.property
 *      name="service.description"
 *      value="Sling jcrinstall OsgiController Service"
 *  @scr.property
 *      name="service.vendor"
 *      value="The Apache Software Foundation"
*/
public class OsgiControllerImpl implements OsgiController, SynchronousBundleListener, OsgiControllerServices, OsgiControllerTask.Context {

	private BundleContext bundleContext;
    private Storage storage;
    private OsgiResourceProcessorList processors;
    private ResourceOverrideRules roRules;
    private final List<OsgiControllerTask> tasks = new LinkedList<OsgiControllerTask>();
    private final OsgiControllerTaskExecutor executor = new OsgiControllerTaskExecutor();

    public static final String STORAGE_FILENAME = "controller.storage";

    /** Storage key: digest of an InstallableData */
    public static final String KEY_DIGEST = "data.digest";

    /** @scr.reference */
    private PackageAdmin packageAdmin;

    /** @scr.reference */
    protected StartLevel startLevel;
    
    /** @scr.reference */
    protected LogService logService;
    
    /** Default value for getLastModified() */
    public static final long LAST_MODIFIED_NOT_FOUND = -1;

    protected void activate(ComponentContext context) throws IOException {
    	bundleContext = context.getBundleContext();
        processors = new OsgiResourceProcessorList(context.getBundleContext(), packageAdmin, startLevel, this);
        storage = new Storage(context.getBundleContext().getDataFile(STORAGE_FILENAME));
    }

    protected void deactivate(ComponentContext oldContext) {
    	if(logService != null) {
    		logService.log(LogService.LOG_WARNING, 
    				OsgiController.class.getName() 
    				+ " service deactivated - this warning can be ignored if system is shutting down");
    	}
    	
    	bundleContext = null;
        if(storage != null) {
            try {
                storage.saveToFile();
            } catch(IOException ioe) {
            	if(logService != null) {
            		logService.log(LogService.LOG_WARNING, "IOException in Storage.saveToFile()", ioe);
            	}
            }
        }
        
        if (processors != null) {
            for (OsgiResourceProcessor processor : processors) {
                processor.dispose();
            }
        }
        
        storage = null;
        processors = null;
    }
    
    public void scheduleInstallOrUpdate(String uri, InstallableData data) throws IOException, JcrInstallException {
    	synchronized (tasks) {
        	tasks.add(new OsgiResourceTask(uri, data, bundleContext));
		}
    }

    public void scheduleUninstall(String uri) throws IOException, JcrInstallException {
    	synchronized (tasks) {
        	tasks.add(new OsgiResourceTask(uri, null, bundleContext));
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

    static String getResourceLocation(String uri) {
        return "jcrinstall://" + uri;
    }

    /** Schedule our next scan sooner if anything happens to bundles */
    public void bundleChanged(BundleEvent e) {
        //loopDelay = 0;
    }

    /** {@inheritDoc} */
    public void executeScheduledOperations() throws Exception {
    	
    	// Ready to work?
        if(processors == null) {
        	if(logService != null) {
                logService.log(LogService.LOG_INFO, "Not activated yet, cannot executeScheduledOperations");
        	}
            return;
        }
        
        // Anything to do?
        if(tasks.isEmpty()) {
        	return;
        }
        
        synchronized (tasks) {
            // Add tasks for our processors to execute their own operations,
            // after our own tasks are executed
            for(OsgiResourceProcessor p : processors) {
            	tasks.add(new ResourceQueueTask(p));
            }
            
            // Now execute all our tasks in a separate thread
        	if(logService != null) {
                logService.log(LogService.LOG_DEBUG, "Executing " + tasks.size() + " queued tasks");
        	}
            final long start = System.currentTimeMillis();
            
            // execute returns the list of tasks that could not be executed but should be retried later
            // and those have been removed from the tasks list
            tasks.addAll(executor.execute(tasks, this));
            
        	if(logService != null) {
                logService.log(LogService.LOG_DEBUG, 
                		"Done executing queued tasks (" + (System.currentTimeMillis() - start) + " msec)");
        	}
		}
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

	public OsgiResourceProcessorList getProcessors() {
		return processors;
	}

	public ResourceOverrideRules getResourceOverrideRules() {
		return roRules;
	}

	public Storage getStorage() {
		return storage;
	}
}