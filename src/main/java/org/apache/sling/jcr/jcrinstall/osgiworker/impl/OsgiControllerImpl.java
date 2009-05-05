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
package org.apache.sling.jcr.jcrinstall.osgiworker.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.sling.jcr.jcrinstall.osgiworker.InstallableData;
import org.apache.sling.jcr.jcrinstall.osgiworker.JcrInstallException;
import org.apache.sling.jcr.jcrinstall.osgiworker.OsgiController;
import org.apache.sling.jcr.jcrinstall.osgiworker.OsgiResourceProcessor;
import org.apache.sling.jcr.jcrinstall.osgiworker.ResourceOverrideRules;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class OsgiControllerImpl implements OsgiController, SynchronousBundleListener {

	private BundleContext bundleContext;
    private Storage storage;
    private OsgiResourceProcessorList processors;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private ResourceOverrideRules roRules;
    private final List<Callable<Object>> tasks = new LinkedList<Callable<Object>>();
    private final OsgiControllerTaskExecutor executor = new OsgiControllerTaskExecutor();

    public static final String STORAGE_FILENAME = "controller.storage";

    /** Storage key: digest of an InstallableData */
    public static final String KEY_DIGEST = "data.digest";

    /** @scr.reference */
    private ConfigurationAdmin configAdmin;

    /** @scr.reference */
    private PackageAdmin packageAdmin;

    /** @scr.reference */
    protected StartLevel startLevel;
    
    /** Default value for getLastModified() */
    public static final long LAST_MODIFIED_NOT_FOUND = -1;

    protected void activate(ComponentContext context) throws IOException {
    	bundleContext = context.getBundleContext();
        processors = new OsgiResourceProcessorList(context.getBundleContext(), packageAdmin, startLevel, configAdmin);
        storage = new Storage(context.getBundleContext().getDataFile(STORAGE_FILENAME));
    }

    protected void deactivate(ComponentContext oldContext) {
    	bundleContext = null;
        if(storage != null) {
            try {
                storage.saveToFile();
            } catch(IOException ioe) {
                log.warn("IOException in Storage.saveToFile()", ioe);
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
        	tasks.add(new OsgiControllerTask(storage, processors, roRules, uri, data, bundleContext));
		}
    }

    public void scheduleUninstall(String uri) throws IOException, JcrInstallException {
    	synchronized (tasks) {
        	tasks.add(new OsgiControllerTask(storage, processors, roRules, uri, null, bundleContext));
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
            log.info("Not activated yet, cannot executeScheduledOperations");
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
            log.debug("Executing {} queued tasks", tasks.size());
            final long start = System.currentTimeMillis();
            executor.execute(tasks);
            log.debug("Done executing queued tasks ({} msec)", System.currentTimeMillis() - start);
		}
	}

	public void setResourceOverrideRules(ResourceOverrideRules r) {
        roRules = r;
    }
}