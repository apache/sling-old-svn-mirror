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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.IGNORED;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.jcr.jcrinstall.osgi.InstallableData;
import org.apache.sling.jcr.jcrinstall.osgi.JcrInstallException;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiController;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor;
import org.apache.sling.jcr.jcrinstall.osgi.ResourceOverrideRules;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.packageadmin.PackageAdmin;
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

    private Storage storage;
    private List<OsgiResourceProcessor> processors;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private ResourceOverrideRules roRules;

    public static final String STORAGE_FILENAME = "controller.storage";

    /** @scr.reference */
    private ConfigurationAdmin configAdmin;

    /** @scr.reference */
    private PackageAdmin packageAdmin;

    /** Storage key: digest of an InstallableData */
    public static final String KEY_DIGEST = "data.digest";

    /** Default value for getLastModified() */
    public static final long LAST_MODIFIED_NOT_FOUND = -1;

    protected void activate(ComponentContext context) throws IOException {
    	
    	// Note that, in executeScheduledOperations(),
    	// processors are called in the order of this list
        processors = new LinkedList<OsgiResourceProcessor>();
        processors.add(new BundleResourceProcessor(context.getBundleContext(), packageAdmin));
        processors.add(new ConfigResourceProcessor(configAdmin));

        storage = new Storage(context.getBundleContext().getDataFile(STORAGE_FILENAME));
    }

    protected void deactivate(ComponentContext oldContext) {
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
    
    public int scheduleInstallOrUpdate(String uri, InstallableData data) throws IOException, JcrInstallException {
        int result = IGNORED;
        
        // If a corresponding higher priority resource is already installed, ignore this one
        if(roRules != null) {
            for(String r : roRules.getHigherPriorityResources(uri)) {
                if(storage.contains(r)) {
                    log.info("Resource {} ignored, overridden by {} which has higher priority",
                            uri, r);
                    return IGNORED;
                }
            }
        }
        
        // If a corresponding lower priority resource is installed, uninstall it first
        if(roRules != null) {
            for(String r : roRules.getLowerPriorityResources(uri)) {
                if(storage.contains(r)) {
                    log.info("Resource {} overrides {}, uninstalling the latter",
                            uri, r);
                    scheduleUninstall(uri);
                }
            }
        }
        
        // let suitable OsgiResourceProcessor process install
        final OsgiResourceProcessor p = getProcessor(uri, data);
        if (p != null) {
            try {
                final Map<String, Object> map = storage.getMap(uri);
                result = p.installOrUpdate(uri, map, data);
                if (result != IGNORED) {
                    map.put(KEY_DIGEST, data.getDigest());
                }
                storage.saveToFile();
            } catch(IOException ioe) {
                throw ioe;
            } catch(Exception e) {
                throw new JcrInstallException("Exception in installOrUpdate (" + uri + ")", e);
            }
        }
        return result;
    }

    public void scheduleUninstall(String uri) throws JcrInstallException {
        // If a corresponding higher priority resource is installed, ignore this request
        if(roRules != null) {
            for(String r : roRules.getHigherPriorityResources(uri)) {
                if(storage.contains(r)) {
                    log.info("Resource {} won't be uninstalled, overridden by {} which has higher priority",
                            uri, r);
                    return;
                }
            }
        }
        
        try {
	        // let each processor try to uninstall, one of them
        	// should know how that handle uri
	    	for(OsgiResourceProcessor p : this.processors) {
	                p.uninstall(uri, storage.getMap(uri));
	    	}
	    	
	        storage.remove(uri);
	        storage.saveToFile();
	        
        } catch(Exception e) {
            throw new JcrInstallException("Exception in uninstall (" + uri + ")", e);
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

    /** Return the first processor that accepts given uri, null if not found */
    OsgiResourceProcessor getProcessor(String uri, InstallableData data) {
        OsgiResourceProcessor result = null;

        if(processors == null) {
            throw new IllegalStateException("Processors are not set");
        }

        for(OsgiResourceProcessor p : processors) {
            if(p.canProcess(uri, data)) {
                result = p;
                break;
            }
        }

        if(result == null) {
            log.debug("No processor found for resource {}", uri);
        }

        return result;
    }

    /** Schedule our next scan sooner if anything happens to bundles */
    public void bundleChanged(BundleEvent e) {
        //loopDelay = 0;
    }

    /** {@inheritDoc} */
    public void executeScheduledOperations() throws Exception {
        for(OsgiResourceProcessor p : processors) {
            p.processResourceQueue();
        }
	}

	public void setResourceOverrideRules(ResourceOverrideRules r) {
        roRules = r;
    }
}